package com.vmware.tanzu.simpleui;

import com.vmware.tanzu.simpleui.locator.ModelLocator;
import com.vmware.tanzu.simpleui.locator.impl.DefaultModelLocator;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class McpConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpConfiguration.class);

    @Bean
    public ModelLocator modelLocator() {
        return new DefaultModelLocator();
    }

    @Bean
    public McpSyncClientCustomizer samplingCustomizer(ModelLocator modelLocator) {

        return (name, mcpClientSpec) -> {

            mcpClientSpec = mcpClientSpec.loggingConsumer(logingMessage -> {
                System.out.println("MCP LOGGING: [" + logingMessage.level() + "] " + logingMessage.data());
            });

            mcpClientSpec.sampling(llmRequest -> {
                var userPrompt = ((McpSchema.TextContent) llmRequest.messages().get(0).content()).text();

                Map<String, ChatClient> chatClients = modelLocator
                        .getModelNamesByCapability("TOOLS")
                        .stream().collect(Collectors.toMap(model -> model, model -> ChatClient.create(modelLocator.getChatModelByName(model))));

                ChatClient chatClient;
                if (llmRequest.modelPreferences() != null && llmRequest.modelPreferences().hints() != null) {
                    LOGGER.info("Using model preferences to find client");
                    String modelHint = llmRequest.modelPreferences().hints().get(0).name();

                    chatClient = chatClients.entrySet().stream()
                            .filter(e -> e.getKey().contains(modelHint)).findFirst()
                            .orElseThrow().getValue();
                } else {
                    String model = (String)RequestContextHolder.getRequestAttributes().getAttribute("model", RequestAttributes.SCOPE_REQUEST);
                    if (model != null) {
                        LOGGER.info("No hints, trying to get {}", model);
                        chatClient = chatClients.get(model);
                    } else {
                        LOGGER.info("No hints, returning the first");
                        chatClient = chatClients.values().stream().findFirst().orElse(null);
                    }
                }

                String response = chatClient.prompt()
                        .system(llmRequest.systemPrompt())
                        .user(userPrompt)
                        .call()
                        .content();

                return McpSchema.CreateMessageResult.builder().content(new McpSchema.TextContent(response)).build();
            });
            LOGGER.info("Customizing {}", name);
        };
    }

    @Bean
    public List<SyncMcpToolCallbackProvider> getMcpToolCallbackProviders(ModelLocator modelLocator, McpSyncClientCustomizer samplingCustomizer) {
        return modelLocator.getMcpServers()
                .stream()
                .map(m -> new SyncMcpToolCallbackProvider(
                        McpClient.sync(HttpClientSseClientTransport
                                .builder(m.url())
                                .build())
                                .build()
                ))
                .toList();
    }
}
