package ru.yandex.direct.bsexport.snapshot.holders;

import ru.yandex.direct.bsexport.snapshot.model.ExportedClient;

public class TestClientsHolder extends ClientsHolder {
    public TestClientsHolder() {
        //noinspection ConstantConditions
        super(null, null);
    }

    @Override
    protected void checkInitialized() {
    }

    public void put(ExportedClient client) {
        Long clientId = client.getId();
        put(clientId, client);
    }
}
