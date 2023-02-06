package ru.yandex.market.core.param.listener;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(before = "FreeLiftingEnabledListenerTest.before.csv")
class FreeLiftingEnabledListenerTest extends FunctionalTest {
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
        paramService.setParam(getFreeLiftingParam(SHOP_ID_VALUE_FALSE, true), ACTION_ID);

        ArgumentCaptor<ShopMetaData> metaDataCaptor = ArgumentCaptor.forClass(ShopMetaData.class);
        verify(checkouterShopApi).updateShopData(
                eq(SHOP_ID_VALUE_FALSE),
                metaDataCaptor.capture()
        );

        Assertions.assertTrue(metaDataCaptor.getValue().isFreeLiftingEnabled());
    }

    @Test
    public void testOnChangeFromTrueToFalse() {
        paramService.setParam(getFreeLiftingParam(SHOP_ID_VALUE_TRUE, false), ACTION_ID);

        ArgumentCaptor<ShopMetaData> metaDataCaptor = ArgumentCaptor.forClass(ShopMetaData.class);
        verify(checkouterShopApi).updateShopData(
                eq(SHOP_ID_VALUE_TRUE),
                metaDataCaptor.capture()
        );

        Assertions.assertFalse(metaDataCaptor.getValue().isFreeLiftingEnabled());
    }

    @Test
    public void testNoChange() {
        paramService.setParam(getFreeLiftingParam(SHOP_ID_VALUE_TRUE, true), ACTION_ID);
        paramService.setParam(getFreeLiftingParam(SHOP_ID_VALUE_FALSE, false), ACTION_ID);

        verify(checkouterShopApi, never()).updateShopData(anyLong(), any());
    }

    @Test
    public void testOnCreateTrue() {
        paramService.setParam(getFreeLiftingParam(SHOP_ID_NEW_PARAM, true), ACTION_ID);

        ArgumentCaptor<ShopMetaData> metaDataCaptor = ArgumentCaptor.forClass(ShopMetaData.class);
        verify(checkouterShopApi).updateShopData(
                eq(SHOP_ID_NEW_PARAM),
                metaDataCaptor.capture()
        );

        Assertions.assertTrue(metaDataCaptor.getValue().isFreeLiftingEnabled());
    }

    private static BooleanParamValue getFreeLiftingParam(long entityId, boolean value) {
        return new BooleanParamValue(ParamType.FREE_LIFTING_ENABLED, entityId, value);
    }
}
