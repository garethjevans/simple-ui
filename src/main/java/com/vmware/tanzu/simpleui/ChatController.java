package com.vmware.tanzu.simpleui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.tanzu.simpleui.locator.ModelLocator;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

  private final ModelLocator modelLocator;
  private final ToolCallbackProvider toolCallbackProvider;
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final ObjectMapper objectMapper;

  private static final DecimalFormat df = new DecimalFormat("0.00");

  public ChatController(
      ModelLocator modelLocator,
      @Nullable SyncMcpToolCallbackProvider toolCallbackProvider,
      ObjectMapper objectMapper) {
    LOGGER.info("using ToolCallbackProvider {}", toolCallbackProvider);
    this.toolCallbackProvider = toolCallbackProvider;
    this.modelLocator = modelLocator;
    this.objectMapper = objectMapper;
  }

  @PostMapping(
      path = {"/chat/stream"},
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamMessage(@RequestBody ChatRequest request) {
    LOGGER.info("Got streaming request: {}", request);

    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    ChatModel chatModel = modelLocator.getChatModelByName(request.model());
    ChatClient client =
        ChatClient.builder(chatModel).defaultToolCallbacks(toolCallbackProvider).build();

    executor.execute(
        () -> {
          try {
            Flux<ChatClientResponse> responseStream =
                client
                    .prompt(
                        new Prompt(
                            convertMessages(request),
                            ToolCallingChatOptions.builder().model(request.model()).build()))
                    .stream()
                    .chatClientResponse();

            responseStream
                .filter(Objects::nonNull)
                .subscribe(
                    chunk -> {
                      try {
                        // Send as JSON to preserve exact content
                        Map<String, ChatClientResponse> payload = Map.of("content", chunk);
                        String jsonData = objectMapper.writeValueAsString(payload);

                        emitter.send(SseEmitter.event().data(jsonData).name("message"));
                      } catch (IOException e) {
                        emitter.completeWithError(e);
                      }
                    },
                    emitter::completeWithError,
                    () -> {
                      try {
                        emitter.send(SseEmitter.event().name("close").data(""));
                        emitter.complete();
                      } catch (IOException e) {
                        emitter.completeWithError(e);
                      }
                    });

          } catch (Exception e) {
            emitter.completeWithError(e);
          }
        });

    return emitter;
  }

  @PostMapping(path = {"/chat"})
  public ChatResponse sendMessage(@RequestBody ChatRequest request) {
    LOGGER.info("Got request: {}", request);

    if (toolCallbackProvider != null) {
      var callbacks = toolCallbackProvider.getToolCallbacks();
      for (var callback : callbacks) {
        LOGGER.info(
            "Got tool {}: {}",
            callback.getToolDefinition().name(),
            callback.getToolDefinition().description());
      }
    }

    ChatModel chatModel = modelLocator.getChatModelByName(request.model());
    ChatClient client =
        ChatClient.builder(chatModel).defaultToolCallbacks(toolCallbackProvider).build();

    try {
      long start = System.currentTimeMillis();
      var response =
          client
              .prompt(
                  new Prompt(
                      convertMessages(request),
                      ToolCallingChatOptions.builder().model(request.model()).build()))
              .call()
              .chatResponse();

      String model = response.getMetadata().getModel();
      String responseText = response.getResult().getOutput().getText();
      responseText = responseText.replace("<", "&lt;").replace(">", "&gt;");
      responseText = toHtml(responseText);
      org.springframework.ai.chat.metadata.Usage usage = response.getMetadata().getUsage();

      long end = System.currentTimeMillis();

      var cr =
          new ChatResponse(
              List.of(new Message("assistant", responseText)),
              model,
              new Usage(usage.getPromptTokens(), usage.getCompletionTokens(), end - start));
      LOGGER.info("Got chatResponse: {}", cr);
      return cr;
    } catch (NonTransientAiException exp) {
      String responseText = exp.getMessage();
      String model = "error";

      var cr = new ChatResponse(List.of(new Message("error", responseText)), model, null);
      LOGGER.info("Got failed chatResponse: {}", cr);
      return cr;
    }
  }

  @GetMapping(path = {"/models"})
  public List<String> models() {
    LOGGER.info("Listing models");
    return modelLocator.getModelNamesByCapability("CHAT");
  }

  private String toHtml(String in) {
    Parser parser = Parser.builder().build();
    Node document = parser.parse(in);
    HtmlRenderer renderer = HtmlRenderer.builder().build();
    return renderer.render(document);
  }

  private List<org.springframework.ai.chat.messages.Message> convertMessages(ChatRequest request) {
    List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
    for (Message m : request.messages()) {
      if ("user".equals(m.role)) {
        messages.add(new UserMessage(m.message));
      } else {
        messages.add(new AssistantMessage(m.message));
      }
    }
    return messages;
  }

  public record ChatRequest(List<Message> messages, String model) {}

  public record ChatResponse(List<Message> messages, String model, Usage usage) {}

  public record Message(String role, String message) {}

  public record Usage(
      Integer promptTokens,
      Integer completionTokens,
      Integer totalTokens,
      Long timeTaken,
      String tokensPerSecond) {

    public Usage(Integer promptTokens, Integer completionTokens, Long timeTaken) {
      this(
          promptTokens,
          completionTokens,
          promptTokens + completionTokens,
          timeTaken,
          df.format(((double) (promptTokens + completionTokens) * 1000) / (double) timeTaken));
    }
  }
}
