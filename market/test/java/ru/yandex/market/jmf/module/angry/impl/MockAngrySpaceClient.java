package ru.yandex.market.jmf.module.angry.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import ru.yandex.market.jmf.module.angry.AngrySpaceClient;
import ru.yandex.market.jmf.module.angry.controller.v1.model.AccountObject;

@Component
public class MockAngrySpaceClient {
    private final AngrySpaceClient angrySpaceClient;

    public MockAngrySpaceClient(AngrySpaceClient angrySpaceClient) {
        this.angrySpaceClient = angrySpaceClient;
    }

    public void clear() {
        Mockito.reset(angrySpaceClient);
    }

    public void setupSendItem(JsonNode returningNode) {
        Mockito.when(angrySpaceClient.sendItem(Mockito.anyString(), Mockito.any()))
                .thenAnswer(inv -> returningNode);
    }

    public void setupSendMessage(JsonNode returningNode) {
        Mockito.when(angrySpaceClient.sendChatMessage(Mockito.anyString(), Mockito.any()))
                .thenAnswer(inv -> returningNode);
    }

    public void setupGetAccount(AccountObject accountObject) {
        Mockito.when(angrySpaceClient.getAccount(Mockito.any()))
                .thenReturn(accountObject);
    }
}
