package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.pixels.ClientPixelProvider;
import ru.yandex.direct.core.entity.banner.model.pixels.CriterionType;
import ru.yandex.direct.core.entity.banner.model.pixels.PixelCampaignType;
import ru.yandex.direct.core.entity.banner.model.pixels.Provider;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestBannerPixelsRepository;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

public class ClientPixelProviderSteps {
    @Autowired
    private TestBannerPixelsRepository testBannerPixelsRepository;
    @Autowired
    private DslContextProvider dslContextProvider;

    /**
     * добавляет несколько провайдеров пикселей пачкой.
     * Нужно чтобы тесты компактнее писать
     * @param clientInfo
     */
    public void addCpmBannerPixelsPermissions(ClientInfo clientInfo) {
        addClientPixelProviderPermissionCpmBanner(clientInfo, Provider.ADRIVER);
        addClientPixelProviderPermissionCpmBanner(clientInfo, Provider.TNS);
        addClientPixelProviderPermissionCpmBanner(clientInfo, Provider.DCM);
    }

    public void addClientPixelProviderPermissionCpmBanner(ClientInfo clientInfo, Provider provider) {
        ClientPixelProvider clientPixelProvider = new ClientPixelProvider()
                .withProvider(provider)
                .withPixelCampaignType(PixelCampaignType.CPM_BANNER)
                .withCriterionType(CriterionType.NONE);
        testBannerPixelsRepository.addClientPixelProviderPermission(
                dslContextProvider.ppc(clientInfo.getShard()),
                clientInfo.getClientId().asLong(),
                clientPixelProvider);
    }
}
