package ru.yandex.market.core.param.listener;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Тесты для {@link SupplierFastReturnParamListener}
 */

@DbUnitDataSet(before = "SupplierFastReturnParamListenerTest.before.csv")
class SupplierFastReturnParamListenerTest extends FunctionalTest {
    private static final long SUPPLIER_NO_PARAM = 1L;
    private static final long SUPPLIER_PARAM_TRUE = 2L;
    private static final long SUPPLIER_PARAM_FALSE = 3L;
    private static final long SHOP_ID = 10L;
    private static final long NOT_EXISTING_PARTNER_ID = 10L;
    private static final long ACTION_ID = 100500L;

    @Autowired
    private ParamService paramService;

    @Autowired
    private CheckouterAPI checkouterAPI;

    CheckouterShopApi checkouterShopApi;

    @BeforeEach
    void setUp() {
        checkouterShopApi = mock(CheckouterShopApi.class);
        doReturn(checkouterShopApi).when(checkouterAPI).shops();
    }

    private static Stream<Arguments> pushedParametersProvider() {
        return Stream.of(
                Arguments.of(SUPPLIER_NO_PARAM, false),
                Arguments.of(SUPPLIER_NO_PARAM, true),
                Arguments.of(SUPPLIER_PARAM_TRUE, false),
                Arguments.of(SUPPLIER_PARAM_FALSE, true)
        );
    }

    @ParameterizedTest
    @MethodSource("pushedParametersProvider")
    void testPushedIntoCheckout(long supplierId, boolean value) {
        paramService.setParam(getFastReturnParam(supplierId, value), ACTION_ID);

        ArgumentCaptor<ShopMetaData> metaDataCaptor = ArgumentCaptor.forClass(ShopMetaData.class);
        verify(checkouterShopApi).updateShopData(
                eq(supplierId),
                metaDataCaptor.capture()
        );
        Assertions.assertEquals(value, metaDataCaptor.getValue().isSupplierFastReturnEnabled());
    }

    @Test
    void testNotPushedIntoCheckout() {
        paramService.setParam(getFastReturnParam(SUPPLIER_PARAM_FALSE, false), ACTION_ID);
        paramService.setParam(getFastReturnParam(SUPPLIER_PARAM_TRUE, true), ACTION_ID);

        verify(checkouterShopApi, never()).updateShopData(anyLong(), any());
    }

    @Test
    void testNotSupplierThrows() {
        assertThrows(IllegalStateException.class,
                () -> paramService.setParam(getFastReturnParam(SHOP_ID, true), ACTION_ID));

        verify(checkouterShopApi, never()).updateShopData(anyLong(), any());
    }

    @Test
    void testNoPartnerThrows() {
        assertThrows(IllegalStateException.class,
                () -> paramService.setParam(getFastReturnParam(NOT_EXISTING_PARTNER_ID, true), ACTION_ID));

        verify(checkouterShopApi, never()).updateShopData(anyLong(), any());
    }

    private static BooleanParamValue getFastReturnParam(long entityId, boolean value) {
        return new BooleanParamValue(ParamType.SUPPLIER_FAST_RETURN_ENABLED, entityId, value);
    }

}
