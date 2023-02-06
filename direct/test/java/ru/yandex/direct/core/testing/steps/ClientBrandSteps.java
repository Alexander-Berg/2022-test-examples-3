package ru.yandex.direct.core.testing.steps;

import java.time.LocalDateTime;
import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.client.model.ClientBrand;
import ru.yandex.direct.core.entity.client.repository.ClientBrandsRepository;
import ru.yandex.direct.core.testing.info.ClientBrandInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;

@ParametersAreNonnullByDefault
public class ClientBrandSteps {
    private final ClientSteps clientSteps;
    private final ClientBrandsRepository clientBrandsRepository;

    @Autowired
    public ClientBrandSteps(ClientSteps clientSteps, ClientBrandsRepository clientBrandsRepository) {
        this.clientSteps = clientSteps;
        this.clientBrandsRepository = clientBrandsRepository;
    }

    public ClientBrandInfo createDefaultClientBrand() {
        return createClientBrand(LocalDateTime.now());
    }

    public ClientBrandInfo createClientBrand(LocalDateTime syncDateTime) {
        ClientInfo clientInfo = clientSteps.createDefaultClient();
        ClientInfo brandClientInfo = clientSteps.createDefaultClient();

        ClientBrand brand = new ClientBrand()
                .withClientId(clientInfo.getClientId().asLong())
                .withBrandClientId(brandClientInfo.getClientId().asLong())
                .withLastSync(syncDateTime);
        clientBrandsRepository.replaceClientBrands(clientInfo.getShard(), Collections.singletonList(brand));
        return new ClientBrandInfo()
                .withClientInfo(clientInfo)
                .withBrandClientInfo(brandClientInfo)
                .withSyncTime(syncDateTime);
    }
}
