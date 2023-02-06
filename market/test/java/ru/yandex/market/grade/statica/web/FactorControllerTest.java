package ru.yandex.market.grade.statica.web;

import org.junit.Test;

import ru.yandex.market.grade.statica.PersStaticWebTest;

/**
 * @author dinyat
 * 18/04/2017
 */
public class FactorControllerTest extends PersStaticWebTest {

    // for old format
    @Test
    public void getShopFactorsOld() throws Exception {
        invokeAndCheckResponse(
            "/data/factor/shop_factor_old.json",
            "/data/saasresponse/factor/shop_factor_old.json",
            "/api/summary/factor/shop/152");
    }

    @Test
    public void getShopFactors() throws Exception {
        invokeAndCheckResponse(
                "/data/factor/shop_factor.json",
                "/data/saasresponse/factor/shop_factor.json",
                "/api/summary/factor/shop/152");
    }

    @Test
    public void getShopFactorsWithoutGrades() throws Exception {
        invokeAndCheckResponse(
                "/data/factor/empty.json",
                "/data/saasresponse/factor/empty.json",
                "/api/summary/factor/shop/99999");
    }

    @Test
    public void getModelFactorsWithOrderNum() throws Exception {
        invokeAndCheckResponse(
                "/data/factor/model_factor_with_order_num.json",
                "/data/saasresponse/factor/model_factor_with_order_num.json",
                "/api/summary/factor/model/3472237");
    }

    @Test
    public void getModelFactorsWithRecommend() throws Exception {
        invokeAndCheckResponse(
                "/data/factor/model_factor_with_recommend.json",
                "/data/saasresponse/factor/model_factor_with_recommend.json",
                "/api/summary/factor/model/8444109");
    }

    @Test
    public void getModelFactorsWithoutGrades() throws Exception {
        invokeAndCheckResponse(
                "/data/factor/empty.json",
                "/data/saasresponse/factor/empty.json",
                "/api/summary/factor/model/1");
    }

    @Test
    public void getNewModelFactorsFromNewMethodWithRadio() throws Exception {
        invokeAndCheckResponse(
            "/data/factor/model_factor_v2_with_radio.json",
            "/data/saasresponse/factor/model_factor.json",
            "/api/summary/factor/v2/model/3472237");
    }
}
