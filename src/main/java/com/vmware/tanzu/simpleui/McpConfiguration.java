package com.vmware.tanzu.simpleui;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.pivotal.cfenv.boot.genai.GenaiLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.client.common.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(McpConfiguration.class);

  @Bean
  public McpSyncClientCustomizer samplingCustomizer(GenaiLocator locator) {

    return (name, mcpClientSpec) -> {
      mcpClientSpec =
          mcpClientSpec.loggingConsumer(
              logingMessage -> {
                System.out.println(
                    "MCP LOGGING: [" + logingMessage.level() + "] " + logingMessage.data());
              });

      mcpClientSpec.sampling(
          llmRequest -> {
            var userPrompt =
                ((McpSchema.TextContent) llmRequest.messages().get(0).content()).text();

            Map<String, ChatClient> chatClients =
                    locator.getModelNamesByCapability("TOOLS").stream()
                    .collect(
                        Collectors.toMap(
                            model -> model,
                            model -> ChatClient.create(locator.getChatModelByName(model))));

            ChatClient chatClient;
            if (llmRequest.modelPreferences() != null
                && llmRequest.modelPreferences().hints() != null) {
              LOGGER.info("Using model preferences to find client");
              String modelHint = llmRequest.modelPreferences().hints().get(0).name();

              chatClient =
                  chatClients.entrySet().stream()
                      .filter(e -> e.getKey().contains(modelHint))
                      .findFirst()
                      .orElseThrow()
                      .getValue();

            } else {
              LOGGER.info("No hints, returning the first");
              chatClient = chatClients.values().stream().findFirst().orElse(null);
            }

            String response =
                chatClient
                    .prompt()
                    .system(llmRequest.systemPrompt())
                    .user(userPrompt)
                    .call()
                    .content();

            return McpSchema.CreateMessageResult.builder()
                .content(new McpSchema.TextContent(response))
                .build();
          });
      LOGGER.info("Customizing {}", name);
    };
  }

  @Bean
  public SyncMcpToolCallbackProvider getMcpSyncClients(
      GenaiLocator locator,
      ObjectProvider<List<McpSyncClient>> syncMcpClients,
      McpSyncClientCustomizer samplingCustomizer) {
      LOGGER.info("Creating MCP Sync Clients");
    List<McpSyncClient> springConfigured = syncMcpClients.stream().flatMap(List::stream).toList();

    LOGGER.info("Configuring {} servers from ai-server", locator.getMcpServers().size());
    List<McpSyncClient> aiServerConfigured =
            locator.getMcpServers().stream()
            .map(
                m -> {
                  LOGGER.info("Connecting to mcp server at url {}", m.url());
                  var transport = HttpClientSseClientTransport.builder(m.url()).build();

                  NamedClientMcpTransport namedTransport =
                      new NamedClientMcpTransport(m.url(), transport);

                  McpClient.SyncSpec spec = McpClient.sync(namedTransport.transport());

                  samplingCustomizer.customize(namedTransport.name(), spec);

                  var client = spec.build();

                  client.initialize();

                  return client;
                })
            .toList();

    LOGGER.info("Combining lists");
    List<McpSyncClient> all = new ArrayList<>();
    all.addAll(springConfigured);
    all.addAll(aiServerConfigured);

    return new SyncMcpToolCallbackProvider(all);
  }
}
