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
}
