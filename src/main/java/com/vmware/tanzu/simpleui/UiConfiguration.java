package com.vmware.tanzu.simpleui;

import io.pivotal.cfenv.boot.genai.GenaiLocator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UiConfiguration {

  @Bean
  public ChatModel chatModel(GenaiLocator locator) {
    return locator.getFirstAvailableChatModel();
  }

  //    @Bean
  //    public OpenAiApi openAiApi(OpenAiConnectionProperties commonProperties, OpenAiChatProperties
  // chatProperties,
  //                               ObjectProvider<RestClient.Builder> restClientBuilderProvider,
  //                               ObjectProvider<WebClient.Builder> webClientBuilderProvider,
  // ResponseErrorHandler responseErrorHandler) {
  //
  //        return OpenAiApi.builder()
  //                .baseUrl(resolved.baseUrl())
  //                .apiKey(new SimpleApiKey(resolved.apiKey()))
  //                .completionsPath(chatProperties.getCompletionsPath())
  //                .embeddingsPath(OpenAiEmbeddingProperties.DEFAULT_EMBEDDINGS_PATH)
  //
  // .restClientBuilder(restClientBuilderProvider.getIfAvailable(RestClient::builder))
  //                .webClientBuilder(webClientBuilderProvider.getIfAvailable(WebClient::builder))
  //                .responseErrorHandler(responseErrorHandler)
  //                .build();
  //    }

  //    @Bean
  //    public EmbeddingModel embeddingModel(GenaiLocator locator) {
  //        return locator.getFirstAvailableEmbeddingModel();
  //    }
}
