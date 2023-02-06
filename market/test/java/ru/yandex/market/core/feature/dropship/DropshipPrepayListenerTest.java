package ru.yandex.market.core.feature.dropship;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.param.model.ParamCheckStatus;

import static org.mockito.Mockito.when;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(before = "DropshipPrepayListenerTest.before.csv")
class DropshipPrepayListenerTest extends FunctionalTest {

    @Autowired
    private CheckouterShopApi checkouterShopApi;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private CheckouterAPI checkouterClient;

    @BeforeEach
    void setUp() {
        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
    }

    @Test
    @DbUnitDataSet(before = "DropshipPrepayListenerTest.enable.before.csv")
    void enable() {
        featureService.changeStatus(1, ShopFeature.of(1, FeatureType.PREPAY, ParamCheckStatus.SUCCESS));
        ArgumentCaptor<ShopMetaData> capture = ArgumentCaptor.forClass(ShopMetaData.class);
        Mockito.verify(checkouterShopApi).updateShopData(Mockito.eq(1L), capture.capture());
        Assertions.assertEquals(PaymentClass.YANDEX, capture.getValue().getPaymentClass(false));
    }

    @Test
    void disable() {
        featureService.changeStatus(1, ShopFeature.of(1, FeatureType.PREPAY, ParamCheckStatus.DONT_WANT));
        ArgumentCaptor<ShopMetaData> capture = ArgumentCaptor.forClass(ShopMetaData.class);
        Mockito.verify(checkouterShopApi).updateShopData(Mockito.eq(1L), capture.capture());
        Assertions.assertEquals(PaymentClass.OFF, capture.getValue().getPaymentClass(false));
    }
}
