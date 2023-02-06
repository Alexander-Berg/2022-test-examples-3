package ru.yandex.market.core.param.listener;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
import ru.yandex.market.core.param.model.NumberParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ParamValue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(before = "MultiParamListenerForCheckouterAsyncTest.before.csv")
public class MultiParamListenerForCheckouterAsyncTest extends FunctionalTest {
    private static final long ACTION_ID = 100500L;

    private static final long SHOP_ID_VALUE_DEFAULT = 1L;
    private static final long SHOP_ID_NEW_PARAM = 2L;

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

    public static Stream<Arguments> shopsWithParamOnDelete() {
        return Stream.of(
                Arguments.of(new NumberParamValue(ParamType.PRESCRIPTION_MANAGEMENT_SYSTEM, SHOP_ID_VALUE_DEFAULT, 1)),
                Arguments.of(new NumberParamValue(ParamType.DELIVERY_RECEIPT_NEED_TYPE, SHOP_ID_VALUE_DEFAULT, 2)),
                Arguments.of(new BooleanParamValue(ParamType.IS_EARLY_SHOW_EMAIL_ENABLED, SHOP_ID_VALUE_DEFAULT, true))
        );
    }

    @ParameterizedTest
    @MethodSource("shopsWithParamOnDelete")
    public void testOnChange(ParamValue paramValue) {
        paramService.setParam(paramValue, ACTION_ID);

        ArgumentCaptor<ShopMetaData> metaDataCaptor = ArgumentCaptor.forClass(ShopMetaData.class);
        // Ожидаем один апдейт от одного листенера
        verify(checkouterShopApi).updateShopData(
                eq(SHOP_ID_VALUE_DEFAULT),
                metaDataCaptor.capture()
        );
    }

    @ParameterizedTest
    @MethodSource("shopsWithParamOnDelete")
    public void testOnCreate(ParamValue paramValue) {
        paramValue.setEntityId(SHOP_ID_NEW_PARAM);
        paramService.setParam(paramValue, ACTION_ID);

        ArgumentCaptor<ShopMetaData> metaDataCaptor = ArgumentCaptor.forClass(ShopMetaData.class);
        // Ожидаем один апдейт от одного листенера
        verify(checkouterShopApi).updateShopData(
                eq(SHOP_ID_NEW_PARAM),
                metaDataCaptor.capture()
        );
    }

    @ParameterizedTest
    @MethodSource("shopsWithParamOnDelete")
    @SuppressWarnings({"rawtypes","unchecked"})
    public void testNoChangeSameValue(ParamValue paramValue) {
        ParamValue oldParamValue = paramService.getParam(paramValue.getType(), SHOP_ID_VALUE_DEFAULT);
        Assertions.assertNotNull(oldParamValue);

        paramValue.setValue(oldParamValue.getValue());
        paramService.setParam(paramValue, ACTION_ID);

        verify(checkouterShopApi, never()).updateShopData(anyLong(), any());
    }
}
