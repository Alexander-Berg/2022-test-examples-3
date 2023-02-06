package ru.yandex.market.api.server.sec.client.strategy;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.domain.v1.ModelInfo;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.model.DefaultModelV1;
import ru.yandex.market.api.offer.Offer;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.exceptions.AccessDeniedException;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

/**
 * Created by tesseract on 11.07.17.
 */
@WithContext
public class VendorClientStrategyTest extends UnitTestBase {

    VendorClientStrategy INSTANCE = VendorClientStrategy.INSTANCE;

    @Test(expected = AccessDeniedException.class)
    public void checkOfferV1_other() {
        long vendorId = 1234567;
        long otherId = 56789;
        // настройка системы
        prepareContext(vendorId);
        // вызов системы
        Offer offer = new Offer();
        offer.setVendorId(otherId);

        INSTANCE.checkAccess(offer);
        // проверка утверждений
    }

    @Test
    public void checkOfferV1_owner() {
        long vendorId = 1234567;
        // настройка системы
        prepareContext(vendorId);
        // вызов системы
        Offer offer = new Offer();
        offer.setVendorId(vendorId);

        Offer result = INSTANCE.checkAccess(offer);
        // проверка утверждений
        Assert.assertTrue(offer == result);
    }

    @Test(expected = AccessDeniedException.class)
    public void checkOfferV2_other() {
        long vendorId = 1234567;
        long otherId = 56789;
        // настройка системы
        prepareContext(vendorId);
        // вызов системы
        OfferV2 offer = new OfferV2();
        offer.setVendorId(otherId);

        INSTANCE.checkAccess(offer);
        // проверка утверждений
    }

    @Test
    public void checkOfferV2_owner() {
        long vendorId = 1234567;
        // настройка системы
        prepareContext(vendorId);
        // вызов системы
        OfferV2 offer = new OfferV2();
        offer.setVendorId(vendorId);

        OfferV2 result = INSTANCE.checkAccess(offer);
        // проверка утверждений
        Assert.assertTrue(offer == result);
    }

    @Test(expected = AccessDeniedException.class)
    public void checkModelV1_other() {
        long vendorId = 1234567;
        long otherId = 56789;
        // настройка системы
        prepareContext(vendorId);
        // вызов системы
        DefaultModelV1 model = new DefaultModelV1();
        model.setVendorId(otherId);

        INSTANCE.checkAccess(model);
        // проверка утверждений
    }

    @Test
    public void checkModelV1_owner() {
        long vendorId = 1234567;
        // настройка системы
        prepareContext(vendorId);
        // вызов системы
        DefaultModelV1 model = new DefaultModelV1();
        model.setVendorId(vendorId);

        DefaultModelV1 result = INSTANCE.checkAccess(model);
        // проверка утверждений
        Assert.assertTrue(model == result);
    }

    @Test(expected = AccessDeniedException.class)
    public void checkModelInfo_other() {
        long vendorId = 1234567;
        long otherId = 56789;
        // настройка системы
        prepareContext(vendorId);
        // вызов системы
        ModelInfo model = new ModelInfo();
        model.setVendorId(otherId);

        INSTANCE.checkAccess(model);
        // проверка утверждений
    }

    @Test
    public void checkModelInfo_owner() {
        long vendorId = 1234567;
        // настройка системы
        prepareContext(vendorId);
        // вызов системы
        ModelInfo model = new ModelInfo();
        model.setVendorId(vendorId);

        ModelInfo result = INSTANCE.checkAccess(model);
        // проверка утверждений
        Assert.assertTrue(model == result);
    }

    @Test(expected = AccessDeniedException.class)
    public void checkModelV2_other() {
        long vendorId = 1234567;
        long otherId = 56789;
        // настройка системы
        prepareContext(vendorId);
        // вызов системы
        ModelV2 model = new ModelV2();
        model.setVendorId(otherId);

        INSTANCE.checkAccess(model);
        // проверка утверждений
    }

    @Test
    public void checkModelV2_owner() {
        long vendorId = 1234567;
        // настройка системы
        prepareContext(vendorId);
        // вызов системы
        ModelV2 model = new ModelV2();
        model.setVendorId(vendorId);

        ModelV2 result = INSTANCE.checkAccess(model);
        // проверка утверждений
        Assert.assertTrue(model == result);
    }

    private void prepareContext(long vendorId) {
        Client client = new Client();
        client.setType(Client.Type.VENDOR);
        client.setVendorId(vendorId);

        ContextHolder.update(context -> {
            context.setClient(client);
            context.setRequest(MockRequestBuilder.start().build());
        });
    }
}
