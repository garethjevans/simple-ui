package com.vmware.tanzu.simpleui;

import io.pivotal.cfenv.boot.genai.GenaiLocator;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UiConfiguration {

  @Bean
  public OpenAiChatModel chatModel(GenaiLocator locator) {
    return (OpenAiChatModel) locator.getFirstAvailableChatModel();
  }

  //  @Bean
  //  public OpenAiEmbeddingModel embeddingModel(GenaiLocator locator) {
  //    return (OpenAiEmbeddingModel) locator.getFirstAvailableEmbeddingModel();
  //  }

  @Bean
  public OpenAiApi openAiApi(
      @Value("${genai.locator.api-key}") String apiKey,
      @Value("${genai.locator.api-base}") String apiBase) {
    return OpenAiApi.builder().apiKey(apiKey).baseUrl(apiBase + "/openai/").build();
  }
}
