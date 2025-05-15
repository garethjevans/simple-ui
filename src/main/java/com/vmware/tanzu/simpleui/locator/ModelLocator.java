package com.vmware.tanzu.simpleui.locator;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

public interface ModelLocator {

    List<String> getModelNames();

    List<String> getModelNamesByCapability(String capability);

    ChatModel getChatModelByName(String name);

    ChatModel getFirstAvailableChatModel();

    ChatModel getFirstAvailableToolModel();

    EmbeddingModel getEmbeddingModelByName(String name);

    EmbeddingModel getFirstAvailableEmbeddingModel();

}
