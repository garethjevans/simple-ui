package com.vmware.tanzu.simpleui;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest()
class SimpleUiApplicationTests {

  static WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());

  @BeforeAll
  static void startWireMockServer() {
    wireMockServer.addStubMapping(
        new StubMapping(
            WireMock.anyRequestedFor(WireMock.anyUrl()).build(),
            WireMock.aResponse()
                .withBody(
                    """
                              {
                                "name":"test",
                                "description":"test",
                                "wireFormat":"OPENAI",
                                "advertisedModels":[
                                  {
                                    "name":"tanzu://chat",
                                    "description":"default",
                                    "capabilities":["CHAT"]
                                  }
                                ],
                                "advertisedMcpServers":[]
                              }
                              """)
                .withHeader("Content-Type", "application/json")
                .build()));

    wireMockServer.start();
  }

  @Test
  void contextLoads() {}

  @DynamicPropertySource
  static void registerGenaiLocatorProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "genai.locator.api-base",
        () -> String.format("http://localhost:%s", wireMockServer.port()));
    registry.add(
        "genai.locator.config-url",
        () -> String.format("http://localhost:%s/config/v1/endpoint", wireMockServer.port()));
    registry.add("genai.locator.api-key", () -> "12345");
  }

  @Configuration
  @Import(SimpleUiApplication.class)
  static class Config {

    @Bean
    WireMockServer wireMockServer() {
      return wireMockServer;
    }
  }
}
