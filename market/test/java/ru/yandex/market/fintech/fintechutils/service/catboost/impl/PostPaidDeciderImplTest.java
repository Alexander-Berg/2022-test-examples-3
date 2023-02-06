package ru.yandex.market.fintech.fintechutils.service.catboost.impl;

import java.util.Collections;
import java.util.Random;

import ai.catboost.CatBoostModel;
import ai.catboost.CatBoostPredictions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fintech.fintechutils.AbstractFunctionalTest;
import ru.yandex.market.fintech.fintechutils.service.catboost.PostPaidDecider;
import ru.yandex.market.fintech.fintechutils.service.commonproperty.CommonPropertyCachedService;
import ru.yandex.market.fintech.fintechutils.service.commonproperty.CommonPropertyService;
import ru.yandex.market.fintech.fintechutils.utils.Experiments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PostPaidDeciderImplTest extends AbstractFunctionalTest {

    private static final String INITIAL_LABEL = "initial_label";

    private static final Random RNG = new Random();

    private static final OrderFeatures TEST_FEATURES = OrderFeatures.builder()
            .setOrderId(0L)
            .setLabel(INITIAL_LABEL)
            .setD2pd(0)
            .setGmv(0)
            .setItemCount(0)
            .setUserBadCancelGmvRatioPostpaid(0)
            .setUserBadCancelGmvRatioPrepaid(0)
            .setUserBadCancelOrdersRatioPostpaid(0)
            .setUserBadCancelOrdersRatioPrepaid(0)
            .setUserOrderCountPostpaid(0)
            .setUserOrderCountPrepaid(0)
            .setSupplierBadCancelGmvRatioPostpaid(0)
            .setSupplierBadCancelGmvRatioPrepaid(0)
            .setSupplierBadCancelOrdersRatioPostpaid(0)
            .setSupplierBadCancelOrdersRatioPrepaid(0)
            .setSupplierOrderCountPostpaid(0)
            .setSupplierOrderCountPrepaid(0)
            .setCategoryPPostpaid(0)
            .setSupplierGmvShare(0)
            .setDaysChkpt130(0)
            .setCategoryNameLevel1("s")
            .setCategoryNameLevel2("p")
            .setOrderDeliveryRegionFederalDistrictName("t")
            .setOrderDeliveryRegionProvinceName("aa")
            .setOrderYandexPlusUserFlag(true)
            .setHasPlus(true)
            .setHasPuid(true)
            .setOrderBusinessSchemeName("orderDto.orderBusinessSchemeName()")
            .setOrderDeliveryAttributeDeliveryType("orderDto.orderDeliveryAttributeDeliveryType()")
            .build();

    @Autowired
    private CommonPropertyService commonPropertyService;

    private PostPaidDecider testDecider;

    @BeforeEach
    void setUp() throws Exception {
        CatBoostModel mockModel = Mockito.mock(CatBoostModel.class);
        CatBoostPredictions predictions = new CatBoostPredictions(1, 1);
        predictions.copyObjectPredictions(0, new double[] {0.5});
        var spyPredictions = Mockito.spy(predictions);
        Mockito.doReturn(new double[] {}).when(spyPredictions).copyRowMajorPredictions();
        Mockito.doReturn(0.5).when(spyPredictions).get(Mockito.anyInt(), Mockito.anyInt());
        Mockito.doReturn(spyPredictions).when(mockModel).predict(Mockito.any(), Mockito.<String[][]>any());

        testDecider = new PostPaidDeciderImpl(mockModel, commonPropertyService);
    }

}
