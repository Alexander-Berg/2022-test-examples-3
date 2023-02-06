package ru.yandex.market.grade.statica.web;

import org.junit.Ignore;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.grade.statica.PersStaticWebTest;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.grade.statica.mock.SaasRequestMatchers.withQueryParam;

/**
 * @author varvara
 * 19.08.2019
 */
public class TagControllerTest extends PersStaticWebTest {

    @Ignore
    @Test
    public void modelTags() throws Exception {
        long modelId = 122344;
        //given
        setSaasResponse(
            "/data/saasresponse/tags/model_count.json",
            HttpClientMockUtils.and(withQueryParam("p=0"), withQueryParam("numdoc=1"))
        );

        setSaasResponse(
            "/data/saasresponse/tags/tags_zero_page.json",
            HttpClientMockUtils.and(withQueryParam("p=0"), withQueryParam("numdoc=10"), withQueryParam("gta=s_tag"))
        );

        setSaasResponse(
            "/data/saasresponse/tags/tags_first_page.json",
            HttpClientMockUtils.and(withQueryParam("p=1"), withQueryParam("numdoc=10"), withQueryParam("gta=s_tag"))
        );

        setSaasResponse(
            "/data/saasresponse/tags/tags_second_page.json",
            HttpClientMockUtils.and(withQueryParam("p=2"), withQueryParam("numdoc=10"), withQueryParam("gta=s_tag"))
        );

        setSaasResponse(
            "/data/saasresponse/tags/tags_third_page.json",
            HttpClientMockUtils.and(withQueryParam("p=3"), withQueryParam("numdoc=10"), withQueryParam("gta=s_tag"))
        );

        //when
        MockHttpServletRequestBuilder requestBuilder = get("/api/tags/model/" + modelId);
        String response = mockMvc.perform(requestBuilder)
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andReturn().getResponse().getContentAsString();

        //then

        JSONAssert.assertEquals(
            IOUtils.readInputStream(
                getClass().getResourceAsStream("/data/model_tags.json")),
            response, JSONCompareMode.STRICT_ORDER);
    }

    @Ignore
    @Test
    public void modelTagsOnePage() throws Exception {
        long modelId = 122344;
        //given
        setSaasResponse(
            "/data/saasresponse/tags/model_count_one_page.json",
            HttpClientMockUtils.and(withQueryParam("p=0"), withQueryParam("numdoc=1"))
        );

        setSaasResponse(
            "/data/saasresponse/tags/tags_only_one_page.json",
            HttpClientMockUtils.and(withQueryParam("p=0"), withQueryParam("numdoc=10"), withQueryParam("gta=s_tag"))
        );


        //when
        MockHttpServletRequestBuilder requestBuilder = get("/api/tags/model/" + modelId);
        String response = mockMvc.perform(requestBuilder)
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andReturn().getResponse().getContentAsString();

        //then

        JSONAssert.assertEquals(
            IOUtils.readInputStream(
                getClass().getResourceAsStream("/data/model_tags_one_page.json")),
            response, JSONCompareMode.STRICT_ORDER);
    }

}
