package ru.yandex.market.grade.statica.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.verification.VerificationMode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.grade.statica.PersStaticWebTest;

import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.grade.statica.mock.SaasRequestMatchers.withQueryParam;
import static ru.yandex.market.grade.statica.mock.SaasRequestMatchers.withSearchAttribute;

/**
 * Unit-тесты на DistributionController
 * Created by stille on 19.05.17.
 */
public class DistributionControllerTest extends PersStaticWebTest {

    private static final String WITH_CLONES = "with_clones";

    @Test
    public void testDistributionModel() throws Exception {
        invokeAndCheckDistribution(
            "/data/distribution/model_simple.json",
            "/data/saasresponse/distribution/model_simple.json",
            "/api/distribution/model/119420"
        );
    }

    @Test
    public void testDistributionModelWithTag() throws Exception {
        final String tag = "тестовый тэг";
        final long modelId = 119420;
        invokeAndCheckDistribution(
            "/data/distribution/model_simple.json",
            "/data/saasresponse/distribution/model_simple.json",
            "/api/distribution/model/" + modelId + "?tag=" + tag
        );

        verifySaasRequest(withSearchAttribute("s_resource_list", String.valueOf(modelId)));
        verifySaasRequest(withSearchAttribute("s_tag", "\\(тестовый тэг\\)"));
    }

    @Test
    public void testDistributionModelFiltered() throws Exception {
        invokeAndCheckDistribution("/data/distribution/model_simple.json", "/data/saasresponse/distribution/model_simple.json", "/api/distribution/model/119420?with-cpa=1&with-photo=1");
    }

    @Test
    public void testDistributionModelsSingle() throws Exception {
        invokeAndCheckDistribution("/data/distribution/models_single.json", "/data/saasresponse/distribution/models_single.json", "/api/distribution/model?modelid=119393");
    }

    @Test
    public void testDistributionModelsMultiple() throws Exception {
        invokeAndCheckDistribution("/data/distribution/models_multiple.json",
                "/data/saasresponse/distribution/models_multiple.json",
                "/api/distribution/model?modelid=119393&modelid=119393",
                times(2));
    }

    @Test
    public void testDistributionShop() throws Exception {
        invokeAndCheckDistribution("/data/distribution/shop_simple.json", "/data/saasresponse/distribution/shop_simple.json", "/api/distribution/shop/720");
    }

    @Test
    public void testDistributionShopFiltered() throws Exception {
        invokeAndCheckDistribution("/data/distribution/shop_simple.json", "/data/saasresponse/distribution/shop_simple.json", "/api/distribution/shop/720?with-cpa=1");
    }

    @Test
    public void testDistributionShopsSingle() throws Exception {
        invokeAndCheckDistribution("/data/distribution/shops_single.json",
                "/data/saasresponse/distribution/shops_single.json",
                "/api/distribution/shop?shopid=720");
    }

    @Test
    public void testDistributionShopsMultiple() throws Exception {
        invokeAndCheckDistribution("/data/distribution/shops_multiple.json",
                "/data/saasresponse/distribution/shops_multiple.json",
                "/api/distribution/shop?shopid=720&shopid=720",
                times(2));
    }

    @Test
    public void testDistributionShopsWithClones() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(WITH_CLONES, "true");
        invokeAndCheckDistribution("/data/distribution/shops_multiple.json",
            "/data/saasresponse/distribution/shops_multiple.json",
            "/api/distribution/shop?shopid=720&shopid=720",
            times(2),
            params);

        verifySaasRequest(withSearchAttribute("s_resource_list", "720"), times(2));
    }

    @Test
    public void testDistributionShopsWithoutClones() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(WITH_CLONES, "false");
        invokeAndCheckDistribution("/data/distribution/shops_multiple.json",
            "/data/saasresponse/distribution/shops_multiple.json",
            "/api/distribution/shop?shopid=720&shopid=720",
            times(2),
            params);

        verifySaasRequest(withSearchAttribute("s_resource", "720"), times(2));
    }

    @Test
    public void testBulkDistributionShopWithTooManyIds() throws Exception {
        String path = "/api/distribution/shop?shopid=720&shopid=155&shopid=48321&shopid=774&shopid=152&shopid=90";
        MockHttpServletRequestBuilder requestBuilder = get(path);
        mockMvc.perform(requestBuilder
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void testBulkDistributionModelWithTooManyIds() throws Exception {
        String path = "/api/distribution/model?modelid=7156943&modelid=13985357&modelid=124214&modelid=314863" +
                "&modelid=134223&modelid=1432432";
        MockHttpServletRequestBuilder requestBuilder = get(path);
        mockMvc.perform(requestBuilder
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void testDistributionShopByBusinessId() throws Exception {
        invokeAndCheckDistribution("/data/distribution/shops_single.json",
            "/data/saasresponse/distribution/shops_single.json",
            "/api/distribution/business/720");

        verifySaasRequest(withSearchAttribute("s_group_id", "720"));
    }

    @Test
    public void testDistributionShopByBusinessIds() throws Exception {
        invokeAndCheckDistribution("/data/distribution/shops_multiple.json",
            "/data/saasresponse/distribution/shops_multiple.json",
            "/api/distribution/business?businessId=720&businessId=720",
            times(2));

        verifySaasRequest(withSearchAttribute("s_group_id", "720"), times(2));
    }

    private void invokeAndCheckDistribution(String expectedResponseFilename, String mockedResponseFilename,
                                            String path) throws Exception {
        invokeAndCheckDistribution(expectedResponseFilename, mockedResponseFilename, path, times(1));
    }

    private void invokeAndCheckDistribution(String expectedResponseFilename, String mockedResponseFilename,
                                            String path, VerificationMode times) throws Exception {
        invokeAndCheckDistribution(expectedResponseFilename, mockedResponseFilename, path, times, Collections.emptyMap());
    }


    private void invokeAndCheckDistribution(String expectedResponseFilename, String mockedResponseFilename,
                                            String path, VerificationMode times, Map<String, String> params) throws Exception {
        invokeAndCheckResponse(
            expectedResponseFilename,
            mockedResponseFilename,
            path,
            params);
        verifySaasRequest(withQueryParam("how=docid"), times);
        verifySaasRequest(withQueryParam("haha=da"), times);
        verifySaasRequest(withQueryParam("qi=facet_i_average_grade"), times);
        verifySaasRequest(withQueryParam("gafacets=i_average_grade"), times);
    }

}
