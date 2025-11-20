package com.vmware.tanzu.simpleui;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import io.pivotal.cfenv.boot.genai.DefaultGenaiLocator;
import io.pivotal.cfenv.boot.genai.GenaiLocator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest()
class SimpleUiApplicationTests {

  @Test
  void contextLoads() {}

  @Configuration
  @Import(SimpleUiApplication.class)
  static class Config {

    @Bean
    GenaiLocator genaiLocator(RestClient.Builder builder) {

      WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());

      wireMockServer.addStubMapping(
              new StubMapping(
                      WireMock.anyRequestedFor(WireMock.anyUrl()).build(),
                      WireMock.aResponse().withBody("""
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
                              .withHeader("Content-Type", "application/json").build()
              ));

      wireMockServer.start();

      return new DefaultGenaiLocator(builder,
              wireMockServer.baseUrl(),
              "12345",
              wireMockServer.baseUrl());
    }
  }
}
