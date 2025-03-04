package com.vmware.tanzu.simpleui;

import com.vmware.tanzu.simpleui.model.ModelResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

    private final ChatClient chatClient;
    private final ModelResolver modelResolver;

    public ChatController(ChatClient.Builder chatClientBuilder, ModelResolver modelResolver) {
        this.chatClient = chatClientBuilder.build();
        this.modelResolver = modelResolver;
    }

    @PostMapping(path={"/chat"})
    public List<Message> sendMessage(@RequestBody Request request) {
        LOGGER.info("Got request: {}", request);

        String responseText = chatClient.
                prompt(new Prompt(
                        convertMessages(request),
                        ChatOptions.
                                builder().
                                model(request.model()).
                                build()
                        )
                ).
                call().content();

        Message response = new Message("system", responseText);
        LOGGER.info("Got response: {}", response);

        return List.of(response);
    }

    @GetMapping(path={"/models"})
    public List<String> models() {
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
