package ru.yandex.market.api.cpa;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static com.google.common.primitives.Longs.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты на {@link CPADataPusher}.
 *
 * @author stani
 */
@DbUnitDataSet(before = {"DbCPAPlacementServiceDbUnitTest.before.csv", "create_shop_meta_data.before.csv"})
class CPADataPusherFunctionalTest extends FunctionalTest {

    @Autowired
    private CPADataPusher cpaDataPusher;

    @Autowired
    private CheckouterShopApi checkouterShopApi;

    @Autowired
    private CheckouterAPI checkouterClient;

    private final ArgumentCaptor<ShopMetaData> shopMetaDataArgumentCaptor = ArgumentCaptor.forClass(ShopMetaData.class);

    @BeforeEach
    void setUp() {
        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
    }

    @Test
    void shouldReturnSwitchedOffSupplier() {
        cpaDataPusher.pushShopInfoToCheckout(720L);
        verify(checkouterShopApi, times(0)).updateShopData(eq(720L), any(ShopMetaData.class));
    }

    @Test
    void shouldReturnSwitchedOnSupplier() {
        cpaDataPusher.pushShopInfoToCheckout(721L);
        verify(checkouterShopApi).updateShopData(eq(721L), shopMetaDataArgumentCaptor.capture());
        Assertions.assertEquals(suppliersWithCompletedRequest(), shopMetaDataArgumentCaptor.getValue());
    }

    @Test
    void shouldReturnSwitchedOffSupplierWithFrozenRequest() {
        cpaDataPusher.pushShopInfoToCheckout(722L);
        verify(checkouterShopApi).updateShopData(eq(722L), shopMetaDataArgumentCaptor.capture());
        // 10 - seller_client_id для поставщика 722. Должен быть использован именно seller даже в статусе FROZEN.
        Assertions.assertEquals(suppliersWithoutCompletedRequest(10722L, 10, 1111),
                shopMetaDataArgumentCaptor.getValue());
    }

    @Test
    void shouldReturnSwitchedOffShopForPrepayRequestInStatusNew() {
        cpaDataPusher.pushShopInfoToCheckout(774L);
        verify(checkouterShopApi, never()).updateShopData(eq(774L), any());
    }

    @Test
    void shouldReturnSwitchedOnShopWithYandexMarketPrepayType() {
        cpaDataPusher.pushShopInfoToCheckout(775L);
        verify(checkouterShopApi).updateShopData(eq(775L), shopMetaDataArgumentCaptor.capture());
        Assertions.assertEquals(shopWithCompletedRequest(), shopMetaDataArgumentCaptor.getValue());
    }

    @Test
    void dropshipSupplierWithoutPrepay() {
        cpaDataPusher.pushShopInfoToCheckout(723L);
        verify(checkouterShopApi).updateShopData(eq(723L), shopMetaDataArgumentCaptor.capture());
        Assertions.assertEquals(aliveSupplierWithoutPrepay(), shopMetaDataArgumentCaptor.getValue());
    }

    @Test
    void pushSchedules() {
        doReturn(mock(ShopMetaData.class))
                .when(checkouterShopApi).getShopData(eq(721L));
        doThrow(new ErrorCodeException("Error", "Error", HttpStatus.NOT_FOUND.value()))
                .when(checkouterShopApi).getShopData(eq(774L));

        cpaDataPusher.pushSchedules(asList(721L, 722L, 774L));
        verify(checkouterShopApi, times(1)).pushSchedules(any());
    }

    private ShopMetaData suppliersWithoutCompletedRequest(long campaignId, long clientId, long businessId) {
        return ShopMetaDataBuilder.of(campaignId)
                .withBusninessId(businessId)
                .withClientId(clientId)
                .withSandboxPaymentClass(PaymentClass.OFF)
                .withProdPaymentClass(PaymentClass.OFF)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("789")
                .withPhoneNumber("+7789")
                .withSupplierFastReturnEnabled(true)
                .build();
    }

    private ShopMetaData suppliersWithCompletedRequest() {
        return ShopMetaDataBuilder.of(10721L)
                .withBusninessId(1111)
                .withClientId(9L)
                .withSandboxPaymentClass(PaymentClass.YANDEX)
                .withProdPaymentClass(PaymentClass.YANDEX)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("456")
                .withPhoneNumber("+7456")
                .withSupplierFastReturnEnabled(true)
                .build();
    }

    private ShopMetaData aliveSupplierWithoutPrepay() {
        return ShopMetaDataBuilder.of(10723L)
                .withBusninessId(1111)
                .withClientId(11L)
                .withSandboxPaymentClass(PaymentClass.OFF)
                .withProdPaymentClass(PaymentClass.OFF)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("790")
                .withPhoneNumber("+7790")
                .withSupplierFastReturnEnabled(true)
                .build();
    }

    private ShopMetaData shopWithCompletedRequest() {
        return ShopMetaDataBuilder.of(10775L)
                .withBusninessId(1112)
                .withClientId(8L)
                .withSandboxPaymentClass(PaymentClass.YANDEX)
                .withProdPaymentClass(PaymentClass.YANDEX)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("123")
                .withPhoneNumber("+7123")
                .build();
    }

}
