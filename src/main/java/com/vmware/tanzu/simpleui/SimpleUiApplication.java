package com.vmware.tanzu.simpleui;

import org.springframework.ai.model.openai.autoconfigure.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
		OpenAiAudioTranscriptionAutoConfiguration.class,
		OpenAiModerationAutoConfiguration.class,
		OpenAiImageAutoConfiguration.class,
		OpenAiChatAutoConfiguration.class,
		OpenAiEmbeddingAutoConfiguration.class,
		OpenAiAudioSpeechAutoConfiguration.class
})
public class SimpleUiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimpleUiApplication.class, args);
	}

}
