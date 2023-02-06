package ru.yandex.market.partner.cpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест активации push-api токена.
 * Тесты на http метод activatePushAPITokenS, {@link ActivatePushAPITokenServantlet}.
 *
 * @author stani on 05.07.18.
 */
@DbUnitDataSet(before = "ActivatePushAPITokenServantlet.before.csv")
class ActivatePushAPITokenServantletTest extends FunctionalTest {

    @Autowired
    private CheckouterAPI checkouterClient;

    @BeforeEach
    void init() {
        when(checkouterClient.shops()).thenReturn(mock(CheckouterShopApi.class));
    }

    @Test
    @DbUnitDataSet(after = "testGetShopActivatePushAPIToken.after.csv")
    void testGetShopActivatePushAPIToken() {
        FunctionalTestHelper.get(baseUrl + "/activatePushAPIToken?id={campaignId}&format=json", 101L);
    }

    @Test
    @DbUnitDataSet(after = "testGetSupplierActivatePushAPIToken.after.csv")
    void testGetSupplierActivatePushAPIToken() {
        FunctionalTestHelper.get(baseUrl + "/activatePushAPIToken?id={campaignId}&format=json", 201L);
    }
}
