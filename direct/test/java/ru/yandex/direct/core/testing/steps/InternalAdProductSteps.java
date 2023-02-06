package ru.yandex.direct.core.testing.steps;

import java.util.Collections;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.thymeleaf.util.StringUtils;

import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.internalads.Constants;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsProduct;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsProductOption;
import ru.yandex.direct.core.entity.internalads.service.InternalAdsProductService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.dbutil.model.ClientId;

@ParametersAreNonnullByDefault
public class InternalAdProductSteps {
    private final ClientSteps clientSteps;
    private final ClientService clientService;
    private final UserService userService;
    private final InternalAdsProductService internalAdsProductService;

    public InternalAdProductSteps(ClientSteps clientSteps, ClientService clientService,
                                  UserService userService, InternalAdsProductService internalAdsProductService) {
        this.userService = userService;
        this.clientSteps = clientSteps;
        this.clientService = clientService;
        this.internalAdsProductService = internalAdsProductService;
    }

    public ClientInfo createDefaultInternalAdProduct() {
        return createDefaultInternalAdProductWithOptions(Collections.emptySet(), "");
    }

    public ClientInfo createMobileInternalAdProduct() {
        return createDefaultInternalAdProductWithOptions(Collections.emptySet(), Constants.APP_PRODUCT_SUFFIX);
    }

    public ClientInfo createDefaultInternalAdProductWithOptions(Set<InternalAdsProductOption> options) {
        return createDefaultInternalAdProductWithOptions(options, "");
    }

    public ClientInfo createDefaultInternalAdProductWithOptions(Set<InternalAdsProductOption> options,
                                                                String productNameSuffix) {
        ClientInfo client = clientSteps.createDefaultClient();
        createDefaultInternalAdProduct(client, options, productNameSuffix);

        return client;
    }

    public void createDefaultInternalAdProduct(ClientInfo client,
                                               Set<InternalAdsProductOption> options,
                                               String productNameSuffix) {
        ClientId clientId = client.getClientId();
        clientService.setClientInternalAdProductPerm(clientId);

        internalAdsProductService.createProduct(new InternalAdsProduct()
                .withClientId(clientId)
                .withName("product name " + StringUtils.randomAlphanumeric(20) + productNameSuffix)
                .withDescription("product description " + StringUtils.randomAlphanumeric(20))
                .withOptions(options));
    }
}
