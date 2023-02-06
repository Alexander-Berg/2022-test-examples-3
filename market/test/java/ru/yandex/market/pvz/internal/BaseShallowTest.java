package ru.yandex.market.pvz.internal;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.test.TestExternalConfiguration.DEFAULT_UID;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.DEFAULT_CLIENT_ID;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.DEFAULT_DATASOURCE_ID;

public abstract class BaseShallowTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    private MbiApiClient mbiApiClient;

    @BeforeEach
    void setupMocks() {
        var shopInfo = new SimpleShopRegistrationResponse();
        shopInfo.setDatasourceId(DEFAULT_DATASOURCE_ID);
        shopInfo.setCampaignId(RandomUtils.nextInt(0, Integer.MAX_VALUE));
        shopInfo.setClientId(DEFAULT_CLIENT_ID);
        shopInfo.setOwnerId(DEFAULT_UID);
        when(mbiApiClient.simpleRegisterShop(eq(DEFAULT_UID), eq(DEFAULT_UID), any())).thenReturn(shopInfo);
    }
}
