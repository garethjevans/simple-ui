package com.vmware.tanzu.simpleui;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.vmware.tanzu.simpleui.locator.ModelLocator;
import com.vmware.tanzu.simpleui.locator.impl.DefaultModelLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

public class ModelLocatorTest {

    private WireMockServer wireMockServer;

    private ModelLocator modelLocator;

    @BeforeEach
    public void setup() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        modelLocator = new DefaultModelLocator("""
                {
                  "genai": [
                    {
                      "name": "genai",
                      "label": "genai",
                      "credentials": {
                        "endpoint": {
                          "api_base": "http://localhost:PORT/test",
                          "name": "test",
                          "api_key": "fake-jwt-token",
                          "config_url": "http://localhost:PORT/test/config/v1/endpoint"
                        }
                      }
                    },
                    {
                      "name": "other",
                      "label": "genai",
                      "credentials": {
                        "endpoint": {
                          "api_base": "http://localhost:PORT/other",
                          "name": "other",
                          "api_key": "other-fake-jwt-token",
                          "config_url": "http://localhost:PORT/other/config/v1/endpoint"
                        }
                      }
                    }
                  ],
                  "solace-messaging": [
                    {
                      "name": "solmessaging-shared-instance",
                      "label": "solace-messaging",
                      "plan": "VMR-shared",
                      "tags": [
                        "solace",
                        "rest",
                        "mqtt"
                      ],
                      "credentials": {
                        "clientUsername": "user1",
                        "clientPassword": "password1",
                        "jmsJndiTlsUri": "smf://host:port"
                      }
                    }
                  ]
                }
                """.replace("PORT", Integer.toString(wireMockServer.port())));

        wireMockServer.stubFor(
                get("/test/config/v1/endpoint")
                        .withHeader("Authorization", equalTo("Bearer fake-jwt-token"))
                        .willReturn(
                                aResponse()
                                        .withBody("""
                                                            {
                                                                "name": "test",
                                                                "description": "test",
                                                                "wireFormat": "OPENAI",
                                                                "advertisedModels": [
                                                                    {
                                                                        "name": "chat-2",
                                                                        "description": "",
                                                                        "capabilities": ["CHAT"]
                                                                    },
                                                                    {
                                                                        "name": "embedding-1",
                                                                        "description": "",
                                                                        "capabilities": ["EMBEDDING"]
                                                                    }
                                                                ]
                                                            }
                                                   """)
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")));

        wireMockServer.stubFor(
                get("/other/config/v1/endpoint")
                        .withHeader("Authorization", equalTo("Bearer other-fake-jwt-token"))
                        .willReturn(
                                aResponse()
                                        .withBody("""
                                                            {
                                                                "name": "other",
                                                                "description": "other",
                                                                "wireFormat": "OPENAI",
                                                                "advertisedModels": [
                                                                    {
                                                                        "name": "chat-1",
                                                                        "description": "",
                                                                        "capabilities": ["CHAT"]
                                                                    },
                                                                    {
                                                                        "name": "chat-and-tools-1",
                                                                        "description": "",
                                                                        "capabilities": ["CHAT", "TOOLS"]
                                                                    }
                                                                ]
                                                            }
                                                   """)
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")));
    }

    @Test
    public void canListModels() {
        List<String> models = modelLocator.getModelNames();
        assertThat(models).isNotNull();
        assertThat(models).hasSize(4);
    }

    @Test
    public void canListModelsByCapability() {
        List<String> chatModels = modelLocator.getModelNamesByCapability("CHAT");
        assertThat(chatModels).isNotNull();
        assertThat(chatModels).hasSize(3);
        assertThat(chatModels).contains("chat-1", "chat-2", "chat-and-tools-1");

        List<String> toolModels = modelLocator.getModelNamesByCapability("TOOLS");
        assertThat(toolModels).isNotNull();
        assertThat(toolModels).hasSize(1);
        assertThat(toolModels).contains("chat-and-tools-1");

        ChatModel chatModel = modelLocator.getChatModelByName("chat-1");
        assertThat(chatModel).isNotNull();

        ChatModel firstAvailableChatModel = modelLocator.getFirstAvailableChatModel();
        assertThat(firstAvailableChatModel).isNotNull();

        ChatModel firstAvailableToolModel = modelLocator.getFirstAvailableToolModel();
        assertThat(firstAvailableToolModel).isNotNull();

        EmbeddingModel embeddingModel = modelLocator.getEmbeddingModelByName("embedding-1");
        assertThat(embeddingModel).isNotNull();

        EmbeddingModel firstAvailableEmbeddingModel = modelLocator.getFirstAvailableEmbeddingModel();
        assertThat(firstAvailableEmbeddingModel).isNotNull();
    }

}
