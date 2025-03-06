package com.vmware.tanzu.simpleui;

import com.vmware.tanzu.simpleui.model.ModelResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
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
    public List<Message> sendMessage(@RequestBody Request request) {
        LOGGER.info("Got request: {}", request);
        Message response = null;
        try {
            String responseText = client.
                    prompt(new Prompt(
                                    convertMessages(request),
                                    ToolCallingChatOptions.
                                            builder().
                                            model(request.model()).
                                            build()
                            )
                    ).
                    call().content();

            response = new Message("assistant", responseText);
        } catch (NonTransientAiException exp) {
            response = new Message("error", exp.getMessage());
        }

        LOGGER.info("Got response: {}", response);
        return List.of(response);
    }

    @GetMapping(path={"/models"})
    public List<String> models() {
        LOGGER.info("Listing models");
        return modelResolver.availableModels();
    }

    private List<org.springframework.ai.chat.messages.Message> convertMessages(Request request) {
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


    public record Request(List<Message> messages, String model) {}

    public record Message(String role, String message) {}
}
