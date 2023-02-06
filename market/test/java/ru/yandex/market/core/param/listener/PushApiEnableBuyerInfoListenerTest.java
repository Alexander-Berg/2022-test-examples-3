package ru.yandex.market.core.param.listener;

import java.util.Map;
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
import ru.yandex.market.checkout.checkouter.shop.OrderVisibility;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ParamValue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(before = "PushApiEnableBuyerInfoListenerTest.before.csv")
public class PushApiEnableBuyerInfoListenerTest extends FunctionalTest {
    private static final long ACTION_ID = 100500L;

    private static final long SHOP_ID_VALUE_TRUE = 1L;
    private static final long SHOP_ID_VALUE_FALSE = 2L;
    private static final long SHOP_ID_NEW_PARAM = 3L;

    @Autowired
    private ParamService paramService;

    @Autowired
    private CheckouterAPI checkouterAPI;

    private CheckouterShopApi checkouterShopApi;

    @BeforeEach
    void setUp() {
        checkouterShopApi = mock(CheckouterShopApi.class);
        doReturn(checkouterShopApi).when(checkouterAPI).shops();
    }

    @Test
    public void testOnChangeFromFalseToTrue() {
        paramService.setParam(getParam(SHOP_ID_VALUE_FALSE, true), ACTION_ID);

        ArgumentCaptor<ShopMetaData> metaDataCaptor = ArgumentCaptor.forClass(ShopMetaData.class);
        verify(checkouterShopApi).updateShopData(
                eq(SHOP_ID_VALUE_FALSE),
                metaDataCaptor.capture()
        );

        Map<OrderVisibility, Boolean> orderVisibilityMap = metaDataCaptor.getValue().getOrderVisibilityMap();

        Assertions.assertTrue(orderVisibilityMap.get(OrderVisibility.BUYER));
        Assertions.assertTrue(orderVisibilityMap.get(OrderVisibility.BUYER_NAME));
        Assertions.assertTrue(orderVisibilityMap.get(OrderVisibility.BUYER_PHONE));
        Assertions.assertTrue(orderVisibilityMap.get(OrderVisibility.BUYER_FOR_EARLY_STATUSES));
    }

    @Test
    public void testOnChangeFromTrueToFalse() {
        paramService.setParam(getParam(SHOP_ID_VALUE_TRUE, false), ACTION_ID);

        ArgumentCaptor<ShopMetaData> metaDataCaptor = ArgumentCaptor.forClass(ShopMetaData.class);
        verify(checkouterShopApi).updateShopData(
                eq(SHOP_ID_VALUE_TRUE),
                metaDataCaptor.capture()
        );

        Map<OrderVisibility, Boolean> orderVisibilityMap = metaDataCaptor.getValue().getOrderVisibilityMap();

        Assertions.assertNull(orderVisibilityMap.get(OrderVisibility.BUYER));
        Assertions.assertNull(orderVisibilityMap.get(OrderVisibility.BUYER_NAME));
        Assertions.assertNull(orderVisibilityMap.get(OrderVisibility.BUYER_PHONE));
        Assertions.assertNull(orderVisibilityMap.get(OrderVisibility.BUYER_FOR_EARLY_STATUSES));
    }

    @Test
    public void testNoChange() {
        paramService.setParam(getParam(SHOP_ID_VALUE_TRUE, true), ACTION_ID);
        paramService.setParam(getParam(SHOP_ID_VALUE_FALSE, false), ACTION_ID);

        verify(checkouterShopApi, never()).updateShopData(anyLong(), any());
    }

    @Test
    public void testOnCreateTrue() {
        paramService.setParam(getParam(SHOP_ID_NEW_PARAM, true), ACTION_ID);

        ArgumentCaptor<ShopMetaData> metaDataCaptor = ArgumentCaptor.forClass(ShopMetaData.class);
        verify(checkouterShopApi).updateShopData(
                eq(SHOP_ID_NEW_PARAM),
                metaDataCaptor.capture()
        );

        Map<OrderVisibility, Boolean> orderVisibilityMap = metaDataCaptor.getValue().getOrderVisibilityMap();

        Assertions.assertTrue(orderVisibilityMap.get(OrderVisibility.BUYER));
        Assertions.assertTrue(orderVisibilityMap.get(OrderVisibility.BUYER_NAME));
        Assertions.assertTrue(orderVisibilityMap.get(OrderVisibility.BUYER_PHONE));
        Assertions.assertTrue(orderVisibilityMap.get(OrderVisibility.BUYER_FOR_EARLY_STATUSES));
    }

    @Test
    public void testOnCreateFalse() {
        paramService.setParam(getParam(SHOP_ID_NEW_PARAM, false), ACTION_ID);

        ArgumentCaptor<ShopMetaData> metaDataCaptor = ArgumentCaptor.forClass(ShopMetaData.class);
        verify(checkouterShopApi).updateShopData(
                eq(SHOP_ID_NEW_PARAM),
                metaDataCaptor.capture()
        );

        Map<OrderVisibility, Boolean> orderVisibilityMap = metaDataCaptor.getValue().getOrderVisibilityMap();

        Assertions.assertNull(orderVisibilityMap.get(OrderVisibility.BUYER));
        Assertions.assertNull(orderVisibilityMap.get(OrderVisibility.BUYER_NAME));
        Assertions.assertNull(orderVisibilityMap.get(OrderVisibility.BUYER_PHONE));
        Assertions.assertNull(orderVisibilityMap.get(OrderVisibility.BUYER_FOR_EARLY_STATUSES));
    }

    public static Stream<Arguments> shopsWithParam() {
        return Stream.of(
                Arguments.of(SHOP_ID_VALUE_TRUE),
                Arguments.of(SHOP_ID_VALUE_FALSE)
        );
    }

    @ParameterizedTest
    @MethodSource("shopsWithParam")
    public void testOnDelete(long shopId) {
        ParamValue param = paramService.getParam(ParamType.PUSHAPI_ENABLE_BUYER_INFO, shopId);
        paramService.deleteParam(ACTION_ID, param);

        ArgumentCaptor<ShopMetaData> metaDataCaptor = ArgumentCaptor.forClass(ShopMetaData.class);
        verify(checkouterShopApi).updateShopData(
                eq(shopId),
                metaDataCaptor.capture()
        );

        Map<OrderVisibility, Boolean> orderVisibilityMap = metaDataCaptor.getValue().getOrderVisibilityMap();

        Assertions.assertNull(orderVisibilityMap.get(OrderVisibility.BUYER));
        Assertions.assertNull(orderVisibilityMap.get(OrderVisibility.BUYER_NAME));
        Assertions.assertNull(orderVisibilityMap.get(OrderVisibility.BUYER_PHONE));
        Assertions.assertNull(orderVisibilityMap.get(OrderVisibility.BUYER_FOR_EARLY_STATUSES));
    }

    private static BooleanParamValue getParam(long entityId, boolean value) {
        return new BooleanParamValue(ParamType.PUSHAPI_ENABLE_BUYER_INFO, entityId, value);
    }
}
