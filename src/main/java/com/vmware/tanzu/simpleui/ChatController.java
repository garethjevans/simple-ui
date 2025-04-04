package com.vmware.tanzu.simpleui;

import com.vmware.tanzu.simpleui.model.ModelResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

    private final ModelResolver modelResolver;
    private final ChatClient client;

    public ChatController(ChatClient.Builder chatClientBuilder, ModelResolver modelResolver, SyncMcpToolCallbackProvider toolCallbackProvider) {
        LOGGER.info("using ToolCallbackProvider {}", toolCallbackProvider);

        this.modelResolver = modelResolver;

        var callbacks = toolCallbackProvider.getToolCallbacks();
        for (var callback : callbacks) {
            LOGGER.info("Got tool {}: {}", callback.getToolDefinition().name(), callback.getToolDefinition().description());
        }

        client = chatClientBuilder
                .defaultTools(toolCallbackProvider)
                .build();
    }

    @PostMapping(path={"/chat"})
    public ChatResponse sendMessage(@RequestBody ChatRequest request) {
        LOGGER.info("Got request: {}", request);
        try {
            var response = client.
                    prompt(new Prompt(
                                    convertMessages(request),
                                    ToolCallingChatOptions.
                                            builder().
                                            model(request.model()).
                                            build()
                            )
                    ).
                    call().chatResponse();

            String model = response.getMetadata().getModel();
            String responseText = response.getResult().getOutput().getText();
            Usage usage = response.getMetadata().getUsage();

            var cr = new ChatResponse(List.of(new Message("assistant", responseText)), model, usage);
            LOGGER.info("Got chatResponse: {}", cr);
            return cr;
        } catch (NonTransientAiException exp) {
            String responseText = exp.getMessage();
            String model = "error";
            Usage usage = new EmptyUsage();

            var cr = new ChatResponse(List.of(new Message("error", responseText)), model, usage);
            LOGGER.info("Got failed chatResponse: {}", cr);
            return cr;
        }
    }

    @GetMapping(path={"/models"})
    public List<String> models() {
        LOGGER.info("Listing models");
        return modelResolver.availableModels();
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
}
