package ru.yandex.market.grade.statica.web;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.grade.statica.PersStaticWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.grade.statica.mock.SaasRequestMatchers.withNoCache;
import static ru.yandex.market.grade.statica.mock.SaasRequestMatchers.withQueryParam;
import static ru.yandex.market.grade.statica.mock.SaasRequestMatchers.withQueryRegexp;
import static ru.yandex.market.grade.statica.mock.SaasRequestMatchers.withSearchAttribute;


/**
 * @author a-danilov
 * 27.02.17
 */
public class OpinionControllerTest extends PersStaticWebTest {

    private static final String WITH_PHOTO = "with-photo";
    private static final String PAGE_NUM = "page_num";
    private static final String PAGE_SIZE = "page_size";
    private static final String WITH_CLONES = "with_clones";
    private static final String CPA = "with-cpa";
    private static final String REGION_ID = "region_id";

    @Test
    public void modelOpinionNoUsageTime() throws Exception {
        invokeAndCheckResponse(
            "/data/model_opinion_no_usage_time.json",
            "/data/saasresponse/model_opinion_no_usage_time.json",
            "/api/opinion/model/950065");
    }

    @Test
    public void modelOpinionNoPhoto() throws Exception {
        invokeAndCheckResponse(
                "/data/model_opinion_no_photo.json",
                "/data/saasresponse/model_opinion_no_photo.json",
                "/api/opinion/model/950065");
    }

    @Test
    public void modelOpinionLessThanZeroPage4XX() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/api/opinion/model/950065")
            .param(PAGE_NUM, "-1")
            .param(PAGE_SIZE, "7");

        mockMvc.perform(requestBuilder
            .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void modelOpinionsWithPhotos() throws Exception {
        invokeAndCheckResponse(
                "/data/model_opinions_with_photos.json",
                "/data/saasresponse/model_opinions_with_photos.json",
                "/api/opinion/model/950548");
    }

    @Test
    public void modelOpinionsWithMixedPhotos() throws Exception {
        invokeAndCheckResponse(
                "/data/model_opinions_with_photos_mixed_order.json",
                "/data/saasresponse/model_opinions_with_photos_mixed_order.json",
                "/api/opinion/model/950548");
    }

    @Test
    public void modelOpinionsWithLongResourceId() throws Exception {
        invokeAndCheckResponse(
            "/data/model_opinion_with_long_resource_id.json",
            "/data/saasresponse/model_opinion_with_long_resource_id.json",
            "/api/opinion/model/4294967293");
    }

    @Test
    public void modelOpinionsWithSource() throws Exception {
        invokeAndCheckResponse(
            "/data/model_opinion_with_source.json",
            "/data/saasresponse/model_opinion_with_source.json",
            "/api/opinion/model/950548");
    }

    @Test
    public void modelOpinionsWithPhotoOn() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(WITH_PHOTO, "1");
        invokeAndCheckResponse(
                "/data/model_opinions_with_photo_on.json",
                "/data/saasresponse/model_opinions_with_photo_on.json",
                "/api/opinion/model/950548", params);
    }

    @Test
    public void modelOpinionsWithPhotoOff() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(WITH_PHOTO, "0");
        invokeAndCheckResponse(
                "/data/model_opinions_with_photos.json",
                "/data/saasresponse/model_opinions_with_photos.json",
                "/api/opinion/model/950548",
                params);
    }

    @Test
    public void shopOpinionLessThanZeroPage4XX() throws Exception {
        setSaasResponse("/data/saasresponse/shop_opinions_first_page_when_group_not_null.json");
        MockHttpServletRequestBuilder requestBuilder = get("/api/opinion/shop/152")
            .param(PAGE_NUM, "-1")
            .param(PAGE_SIZE, "7");

        mockMvc.perform(requestBuilder
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shopOpinionMoreThan199Page4XX() throws Exception {
        setSaasResponse("/data/saasresponse/shop_opinions_first_page_when_group_not_null.json");
        MockHttpServletRequestBuilder requestBuilder = get("/api/opinion/shop/152")
            .param(PAGE_NUM, "200")
            .param(PAGE_SIZE, "10");

        mockMvc.perform(requestBuilder
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is4xxClientError());

        requestBuilder = get("/api/opinion/shop/152")
            .param(PAGE_NUM, "199")
            .param(PAGE_SIZE, "10");

        mockMvc.perform(requestBuilder
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void shopOpinionBigPageSize4XX() throws Exception {
        setSaasResponse("/data/saasresponse/shop_opinions_first_page_when_group_not_null.json");
        MockHttpServletRequestBuilder requestBuilder = get("/api/opinion/shop/152")
            .param(PAGE_NUM, "1")
            .param(PAGE_SIZE, "70");

        mockMvc.perform(requestBuilder
            .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void shopOpinionFirstPageTestWhenShopGroupNotNull() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(PAGE_NUM, "0");
        params.put(PAGE_SIZE, "2");
        invokeAndCheckResponse(
                "/data/shop_opinions_first_page_when_group_not_null.json",
                "/data/saasresponse/shop_opinions_first_page_when_group_not_null.json",
                "/api/opinion/shop/152",
                params);
    }

    @Test
    public void shopOpinionSecondPageTestWhenShopGroupNotNull() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(PAGE_NUM, "1");
        params.put(PAGE_SIZE, "1");
        invokeAndCheckResponse(
                "/data/shop_opinions_second_page_when_group_not_null.json",
                "/data/saasresponse/shop_opinions_second_page_when_group_not_null.json",
                "/api/opinion/shop/152",
                params);
    }

    @Test
    public void shopOpinionFirstPageTestWhenShopGroupNull() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(PAGE_NUM, "0");
        params.put(PAGE_SIZE, "2");
        invokeAndCheckResponse(
                "/data/shop_opinions_first_page_when_group_null.json",
                "/data/saasresponse/shop_opinions_first_page_when_group_null.json",
                "/api/opinion/shop/155",
                params);
    }

    @Test
    public void shopOpinionSecondPageTestWhenShopGroupNull() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(PAGE_NUM, "1");
        params.put(PAGE_SIZE, "1");
        invokeAndCheckResponse(
                "/data/shop_opinions_second_page_when_group_null.json",
                "/data/saasresponse/shop_opinions_second_page_when_group_null.json",
                "/api/opinion/shop/155",
                params);
    }

    @Test
    public void shopOpinionFirstPageTestWhenShopGroupNotNullAndSort() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(PAGE_NUM, "0");
        params.put(PAGE_SIZE, "2");
        invokeAndCheckResponse(
                "/data/shop_opinions_first_page_when_group_not_null_and_sort.json",
                "/data/saasresponse/shop_opinions_first_page_when_group_not_null_and_sort.json",
                "/api/opinion/shop/152?sort_by=date",
                params);
    }

    @Test
    public void shopOpinionSecondPageTestWhenShopGroupNotNullAndSort() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(PAGE_NUM, "1");
        params.put(PAGE_SIZE, "1");
        invokeAndCheckResponse(
                "/data/shop_opinions_second_page_when_group_not_null_and_sort.json",
                "/data/saasresponse/shop_opinions_second_page_when_group_not_null_and_sort.json",
                "/api/opinion/shop/152?sort_by=date",
                params);
    }

    @Test
    public void userModelsWithGrade() throws Exception {
        invokeAndCheckResponse(
            "/data/user_grade_models.json",
            "/data/saasresponse/user_grade_models.json",
            "/api/opinion/user/123/model/ids?count=42");
        verifySaasRequest(withQueryParam("text=\\(s_author_id:123\\) && \\(i_type:1 \\| i_type:2\\)"));
        verifySaasRequest(withQueryParam("p=0"));
        verifySaasRequest(withQueryParam("numdoc=42"));
        verifySaasRequest(withQueryParam("how=created"));
        verifySaasRequest(withQueryParam("gta=s_resource"));
    }

    @Test
    public void testWhiteOpinionsBulk() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("gradeId", "53965095");
        params.add("gradeId", "4245245");
        invokeAndCheckResponseMultiParam(
            "/data/white_opinion_bulk.json",
            "/data/saasresponse/model_opinion_ungrouped.json",
            "/api/opinion/white/grade/bulk",
            params);
        verifySaasRequest(withQueryParam("text=\\(url:53965095 \\| url:4245245\\)"));
        verifySaasRequest(withQueryParam("p=0"));
        verifySaasRequest(withQueryParam("numdoc=2"));
    }

    @Test
    public void testWhiteOpinionsBulkTooMany() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        IntStream.range(0, OpinionController.MAX_BULK_GRADE_LIST + 1)
            .forEach(id -> params.add("gradeId", "539650" + id));

        invokeAndCheckResponseMultiParam(
            "/data/saasresponse/model_opinion_ungrouped.json",
            "/api/opinion/white/grade/bulk",
            params,
            status().is4xxClientError());
    }

    @Test
    public void modelOpinionWithFirstGrade() throws Exception {
        Map<String, String> params = new HashMap<>();
        String gradeId = "53965095";
        params.put("first_grade_id", gradeId);
        invokeAndCheckResponse(
                "/data/model_opinions_first_grade.json",
                "/data/saasresponse/model_opinions_first_grade.json",
                "/api/opinion/model/950548",
                params);
        verifySaasRequest(withQueryParam("relev=formula=sort_by_karma_model_relevance;calc=grade_boost:inset\\(#grade_id," + gradeId + "\\);calc=sort_hash:fnvhash_f32\\(zdocid_i64\\(\\)\\)"));
    }

    @Test
    public void modelOpinionWithFirstGradeWithVotes() throws Exception {
        Map<String, String> params = new HashMap<>();
        String gradeId = "53965095";
        params.put("first_grade_id", gradeId);
        invokeAndCheckResponse(
            "/data/model_opinion_with_votes.json",
            "/data/saasresponse/model_opinion_with_votes.json",
            "/api/opinion/model/950065",
            params);
        verifySaasRequest(withQueryParam("relev=formula=sort_by_karma_model_relevance;calc=grade_boost:inset\\(#grade_id," + gradeId + "\\);calc=sort_hash:fnvhash_f32\\(zdocid_i64\\(\\)\\)"));
    }

    @Test
    public void modelOpinionGet() throws Exception {
        invokeAndCheckResponse(
                "/data/model_opinion_single_grade.json",
                "/data/saasresponse/model_opinion_single_grade.json",
                "/api/opinion/model/grade/54208564");
    }

    @Test
    public void modelWhiteOpinionGet() throws Exception {
        invokeAndCheckResponse(
            "/data/model_opinion_single_grade.json",
            "/data/saasresponse/model_opinion_single_grade.json",
            "/api/opinion/white/grade/54208564");
    }


    @Test
    public void modelOpinionCount() throws Exception {
        invokeAndCheckResponse(
            "/data/model_count_single_grade.json",
            "/data/saasresponse/models_resource_id_single.json",
            "/api/opinion/count/model/54208564");
    }

    @Test
    public void testUserOpinionsCount() throws Exception {
        invokeAndCheckResponse(
                "/data/model_count_single_grade.json",
                "/data/saasresponse/model_opinion_single_grade.json",
                "/api/opinion/count/user/54208564");
        verifySaasRequest(withQueryParam(
                "text=\\(s_author_id:54208564\\) && \\(i_type:0 \\| i_type:1 \\| i_type:2\\)"));
    }

    @Test
    public void modelOpinionCountBulk() throws Exception {
        //given
        setSaasResponse("/data/saasresponse/distribution/models_resource_id.json");

        //when
        MockHttpServletRequestBuilder requestBuilder = get("/api/opinion/count/model?modelId=54208564&modelId=917369712&modelId=123");
        String response = mockMvc.perform(requestBuilder
            .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andReturn().getResponse().getContentAsString();

        verifySaasRequest(withQueryParam(
            "text=\\(s_resource_list:54208564 \\| s_resource_list:917369712 \\| s_resource_list:123\\)" +
                " && \\(i_type:1 \\| i_type:2\\)"));
        verifySaasRequest(withQueryParam("qi=facet_s_resource_list"));
        verifySaasRequest(withQueryParam("facets=s_resource_list"));

        //then
        verifyBulkCountForKey(2, "54208564", response);
        verifyBulkCountForKey(3, "917369712", response);
        verifyBulkCountForKey(0, "123", response);
    }

    @Test
    public void modelOpinionCountBulkNoImplicitAcceptType() throws Exception {
        //given
        setSaasResponse("/data/saasresponse/distribution/models_resource_id.json");

        //when
        MockHttpServletRequestBuilder requestBuilder = get("/api/opinion/count/model?modelId=54208564&modelId=917369712&modelId=123");
        String response = mockMvc.perform(requestBuilder)
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andReturn().getResponse().getContentAsString();

        //then
        verifyBulkCountForKey(2, "54208564", response);
        verifyBulkCountForKey(3, "917369712", response);
        verifyBulkCountForKey(0, "123", response);
    }

    @Test
    public void modelOpinionCountBulkTooMany() throws Exception {
        //given
        setSaasResponse("/data/saasresponse/distribution/models_resource_id.json");

        String args = IntStream.range(0, 51)
            .mapToObj(x -> "modelId=111" + x)
            .collect(Collectors.joining("&"));

        //when
        MockHttpServletRequestBuilder requestBuilder = get("/api/opinion/count/model?" + args);

        String response = mockMvc.perform(requestBuilder
            .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().is4xxClientError())
            .andReturn().getResolvedException().getMessage();
        Assert.assertTrue(response.contains("Requested too many models"));
    }

    @Test
    public void modelOpinionForUser() throws Exception {
        final String testAuthorId = "302144043";
        final String testResourceId = "54208564";
        invokeAndCheckResponse(
            "/data/model_opinion_single_grade_for_user.json",
            "/data/saasresponse/model_opinion_single_grade_for_user.json",
            "/api/opinion/user/" + testAuthorId + "/model/" + testResourceId);
        verifySaasRequest(withNoCache());
        verifySaasRequest(withSearchAttribute("s_author_id", testAuthorId));
        verifySaasRequest(withSearchAttribute("s_resource_list", testResourceId));
    }

    @Test
    public void modelOpinionRecommend() throws Exception {
        invokeAndCheckResponse(
                "/data/model_opinions_recommend.json",
                "/data/saasresponse/model_opinions_recommend.json",
                "/api/opinion/model/14231783");
    }

    @Test
    public void modelOpinionGetNotExisting() throws Exception {
        setSaasResponse("/data/saasresponse/no_result.json");
        mockMvc.perform(get("/api/opinion/model/grade/9999999")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is(404));
    }

    @Test
    public void shopOpinionCpaGroup() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(CPA, "1");
        invokeAndCheckResponse(
                "/data/shop_opinions_group_cpa_filtered.json",
                "/data/saasresponse/shop_opinions_group_cpa_filtered.json",
                "/api/opinion/shop/152",
                params);
    }

    @Test
    public void shopOpinionCpa() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(CPA, "1");
        invokeAndCheckResponse(
                "/data/shop_opinions_cpa_filtered.json",
                "/data/saasresponse/shop_opinions_cpa_filtered.json",
                "/api/opinion/shop/155",
                params);
    }

    @Test
    public void shopOpinionRegion() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(REGION_ID, "213");
        invokeAndCheckResponse(
                "/data/shop_opinions_region_filtered.json",
                "/data/saasresponse/shop_opinions_region_filtered.json",
                "/api/opinion/shop/155",
                params);
    }

    @Test
    public void shopOpinionGet() throws Exception {
        invokeAndCheckResponse(
                "/data/shop_opinion_single_grade.json",
                "/data/saasresponse/shop_opinion_single_grade.json",
                "/api/opinion/shop/grade/59749791");
    }

    @Test
    public void shopWhiteOpinionGet() throws Exception {
        invokeAndCheckResponse(
            "/data/shop_opinion_single_grade.json",
            "/data/saasresponse/shop_opinion_single_grade.json",
            "/api/opinion/white/grade/59749791");
    }

    @Test
    public void shopOpinionGetWithFirstGrade() throws Exception {
        Map<String, String> params = new HashMap<>();
        String gradeId = "53965095";
        params.put("first_grade_id", gradeId);
        invokeAndCheckResponse(
            "/data/shop_opinion_with_factors.json",
            "/data/saasresponse/shop_opinion_with_factors.json",
            "/api/opinion/shop/774",
            params);
        verifySaasRequest(withQueryParam("relev=formula=sort_by_date;calc=grade_boost:inset\\(#grade_id," + gradeId + "\\);calc=sort_hash:fnvhash_f32\\(zdocid_i64\\(\\)\\)"));

    }

    @Test
    public void shopOpinionGetWithFirstGradeByFixId() throws Exception {
        Map<String, String> params = new HashMap<>();
        String fixId = "53965095";
        params.put(ControllerConst.FIRST_GRADE_BY_FIX_ID, fixId);
        invokeAndCheckResponse(
            "/data/shop_opinion_with_factors.json",
            "/data/saasresponse/shop_opinion_with_factors.json",
            "/api/opinion/shop/774",
            params);
        verifySaasRequest(withQueryParam("relev=formula=sort_by_date;calc=grade_boost:inset\\(#s_fix_id," + fixId + "\\);calc=sort_hash:fnvhash_f32\\(zdocid_i64\\(\\)\\)"));

    }

    @Test
    public void shopOpinionGetWithFirstGradeByGradeIdAndFixId() throws Exception {
        Map<String, String> params = new HashMap<>();
        String gradeId = "53965095";
        String fixId = "634534";
        params.put(ControllerConst.FIRST_GRADE_BY_FIX_ID, fixId);
        params.put(ControllerConst.FIRST_GRADE_ID, gradeId);
        invokeAndCheckResponse(
            "/data/shop_opinion_with_factors.json",
            "/data/saasresponse/shop_opinion_with_factors.json",
            "/api/opinion/shop/774",
            params);
        verifySaasRequest(withQueryParam("relev=formula=sort_by_date;calc=grade_boost:inset\\(#grade_id," + gradeId + "\\);calc=sort_hash:fnvhash_f32\\(zdocid_i64\\(\\)\\)"));

    }

    @Test
    public void shopOpinionForUser() throws Exception {
        final String testAuthorId = "292906435";
        final String testResourceId = "59749791";
        invokeAndCheckResponse(
            "/data/shop_opinion_single_grade_for_user.json",
            "/data/saasresponse/shop_opinion_single_grade_for_user.json",
            "/api/opinion/user/" + testAuthorId+ "/shop/" + testResourceId );
        verifySaasRequest(withNoCache());
        verifySaasRequest(withSearchAttribute("s_author_id", testAuthorId));
        verifySaasRequest(withSearchAttribute("s_resource_list", testResourceId));
    }

    @Test
    public void modelOpinionGetButHaveShopGrade() throws Exception {
        setSaasResponse("/data/saasresponse/shop_opinion_single_grade.json");
        mockMvc.perform(get("/api/opinion/model/grade/59749791")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shopOpinionGetButHaveModelGrade() throws Exception {
        setSaasResponse("/data/saasresponse/model_opinion_single_grade.json");
        mockMvc.perform(get("/api/opinion/shop/grade/54208564")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void modelOpinionCpa() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(CPA, "1");
        invokeAndCheckResponse(
                "/data/model_opinions_cpa_filtered.json",
                "/data/saasresponse/model_opinions_cpa_filtered.json",
                "/api/opinion/model/7156943",
                params);
    }

    @Test
    public void modelUngroupedOpinion() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(PAGE_SIZE, "2");
        invokeAndCheckResponse(
                "/data/model_opinion_ungrouped.json",
                "/data/saasresponse/model_opinion_ungrouped.json",
                "/api/opinion/model/10586210",
                params);
    }

    @Test
    public void modelOpinionWithFactors() throws Exception {
        invokeAndCheckResponse(
                "/data/model_opinion_with_factors.json",
                "/data/saasresponse/model_opinion_with_factors.json",
                "/api/opinion/model/950548");
    }

    @Test
    public void shopOpinionsWithFactors() throws Exception {
        invokeAndCheckResponse(
                "/data/shop_opinion_with_factors.json",
                "/data/saasresponse/shop_opinion_with_factors.json",
                "/api/opinion/shop/774");
    }

    @Test
    public void shopOpinionsWithoutClones() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(WITH_CLONES, "false");
        invokeAndCheckResponse(
            "/data/shop_opinion_with_factors.json",
            "/data/saasresponse/shop_opinion_with_factors.json",
            "/api/opinion/shop/774",
            params);
        verifySaasRequest(withSearchAttribute("s_resource", "774"));
    }

    @Test
    public void shopOpinionsWithClones() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(WITH_CLONES, "true");
        invokeAndCheckResponse(
            "/data/shop_opinion_with_factors.json",
            "/data/saasresponse/shop_opinion_with_factors.json",
            "/api/opinion/shop/774",
            params);
        verifySaasRequest(withSearchAttribute("s_resource_list", "774"));
    }

    @Test
    public void modelOpinionDefaultSortFormula() throws Exception {
        invokeAndCheckResponse(
            "/data/model_opinion_with_votes.json",
            "/data/saasresponse/model_opinion_with_votes.json",
            "/api/opinion/model/950065");
        verifySaasRequest(withQueryParam("relev=formula=sort_by_karma_model_relevance.*;calc=sort_hash:fnvhash_f32\\" +
            "(zdocid_i64\\(\\)\\)"));
    }

    @Test
    public void modelOpinionWithRankSort() throws Exception {
        invokeAndCheckResponse(
            "/data/model_opinion_with_votes_rank_sorter.json",
            "/data/saasresponse/model_opinion_with_votes.json",
            "/api/opinion/model/950065?sort_by=rank");
        verifySaasRequest(withQueryParam("relev=formula=sort_by_karma_rank;calc=sort_hash:fnvhash_f32\\(zdocid_i64\\" +
            "(\\)\\)"));
    }

    @Test
    public void shopOpinionsWithVotesByDefault() throws Exception {
        invokeAndCheckResponse(
            "/data/shop_opinion_with_votes.json",
            "/data/saasresponse/shop_opinion_with_votes.json",
            "/api/opinion/shop/774");
    }

    @Test
    public void usefulModelOpinionWithVotesByDefault() throws Exception {
        final String testResourceId = "54208564";
        invokeAndCheckResponse(
            "/data/model_opinion_useful_with_votes.json",
            "/data/saasresponse/model_opinion_useful_with_votes.json",
            "/api/opinion/model/" + testResourceId + "/useful");
        verifySaasRequest(withNoCache());
        verifySaasRequest(withSearchAttribute("s_resource_list", testResourceId));
        verifySaasRequest(withQueryParam("relev=formula=sort_by_karma_model_relevance.*;calc=sort_hash:fnvhash_f32\\" +
            "(zdocid_i64\\(\\)\\)"));
    }

    @Test
    public void modelGradeByTags() throws Exception {
        final String testResourceId = "54208564";
        final String tag = "тестовый тэг";
        invokeAndCheckResponse(
            "/data/model_opinion_with_tag.json",
            "/data/saasresponse/model_opinion_useful_with_votes.json",
            "/api/opinion/model/" + testResourceId + "?tag=" + tag);
        verifySaasRequest(withSearchAttribute("s_resource_list", testResourceId));
        verifySaasRequest(withSearchAttribute("s_tag", "\\(тестовый тэг\\)"));
    }

}
