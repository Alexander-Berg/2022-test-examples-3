package ru.yandex.direct.core.testing.steps;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.client.model.ClientsOptions;
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestClientOptionsRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

public class ClientOptionsSteps {

    private final TestClientOptionsRepository testClientOptionsRepository;
    private final ClientOptionsRepository clientOptionsRepository;

    @Autowired
    public ClientOptionsSteps(TestClientOptionsRepository testClientOptionsRepository,
                              ClientOptionsRepository clientOptionsRepository) {
        this.testClientOptionsRepository = testClientOptionsRepository;
        this.clientOptionsRepository = clientOptionsRepository;
    }

    public void addEmptyClientOptions(ClientInfo clientInfo) {
        addEmptyClientOptions(clientInfo.getShard(), clientInfo.getClientId());
    }

    public void addEmptyClientOptions(int shard, ClientId clientId) {
        testClientOptionsRepository.addEmptyClientsOptions(shard, clientId);
    }

    public void setClientFlags(ClientInfo clientInfo, String clientFlags) {
        setClientFlags(clientInfo.getShard(), clientInfo.getClientId(), clientFlags);
    }

    public void setClientFlags(int shard, ClientId clientId, String clientFlags) {
        testClientOptionsRepository.setClientFlags(shard, clientId, clientFlags);
    }

    public <V> void setClientOptionsProperty(ClientInfo clientInfo, ModelProperty<? super ClientsOptions, V> property,
                                             V value) {
        if (!ClientsOptions.allModelProperties().contains(property)) {
            throw new IllegalArgumentException(
                    "Model " + ClientsOptions.class.getName() + " doesn't contain property " + property.name());
        }
        int shard = clientInfo.getShard();
        ClientId clientId = clientInfo.getClientId();
        List<ClientsOptions> optionsList = clientOptionsRepository.getClientsOptions(shard, singleton(clientId));
        if (optionsList.isEmpty()) {
            addEmptyClientOptions(shard, clientId);
            optionsList = clientOptionsRepository.getClientsOptions(shard, singleton(clientId));
        }
        ClientsOptions clientsOptions = optionsList.get(0);
        AppliedChanges<ClientsOptions> appliedChanges = new ModelChanges<>(clientsOptions.getId(), ClientsOptions.class)
                .process(value, property)
                .applyTo(clientsOptions);
        clientOptionsRepository.update(shard, singletonList(appliedChanges));
    }

}
