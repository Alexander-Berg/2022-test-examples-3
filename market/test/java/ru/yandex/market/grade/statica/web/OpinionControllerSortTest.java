package ru.yandex.market.grade.statica.web;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import ru.yandex.market.grade.statica.PersStaticWebTest;
import ru.yandex.market.grade.statica.web.dto.RearrFlags;

import static ru.yandex.market.grade.statica.mock.SaasRequestMatchers.withQueryParam;
import static ru.yandex.market.grade.statica.mock.SaasRequestMatchers.withQueryRegexp;
import static ru.yandex.market.grade.statica.mock.SaasRequestMatchers.withSearchAttribute;

public class OpinionControllerSortTest extends PersStaticWebTest {

    @Test
    public void testShopOpinionByBusinessId() throws Exception {
        invokeAndCheckResponse(
            "/data/shop_opinion_with_factors.json",
            "/data/saasresponse/shop_opinion_with_factors.json",
            "/api/opinion/business/774?first_grade_id=123"
        );

        verifySaasRequest(withSearchAttribute("s_group_id", "774"));
        verifySaasRequest(withQueryRegexp(".*?relev=formula=sort_by_date.*"));
        verifySaasRequest(withQueryRegexp(".*?calc=grade_boost:inset\\(#grade_id,123\\).*"));
    }

    @Test
    public void modelOpinionWithFirstGradeByFixId() throws Exception {
        Map<String, String> params = new HashMap<>();
        String fixId = "594";
        params.put(ControllerConst.FIRST_GRADE_BY_FIX_ID, fixId);
        invokeAndCheckResponse(
            "/data/model_opinions_first_grade.json",
            "/data/saasresponse/model_opinions_first_grade.json",
            "/api/opinion/model/950548",
            params);
        verifySaasRequest(withQueryParam("relev=formula=sort_by_karma_model_relevance;calc=grade_boost:inset\\" +
            "(#s_fix_id," + fixId + "\\);calc=sort_hash:fnvhash_f32\\(zdocid_i64\\(\\)\\)"));
    }


    @Test
    public void modelOpinionWithFirstGradeByGradeIdAndFixId() throws Exception {
        Map<String, String> params = new HashMap<>();
        String gradeId = "53965095";
        String fixId = "594";
        params.put(ControllerConst.FIRST_GRADE_BY_FIX_ID, fixId);
        params.put(ControllerConst.FIRST_GRADE_ID, gradeId);
        invokeAndCheckResponse(
            "/data/model_opinions_first_grade.json",
            "/data/saasresponse/model_opinions_first_grade.json",
            "/api/opinion/model/950548",
            params);
        verifySaasRequest(withQueryParam("relev=formula=sort_by_karma_model_relevance;calc=grade_boost:inset\\" +
            "(#grade_id," + gradeId + "\\);calc=sort_hash:fnvhash_f32\\(zdocid_i64\\(\\)\\)"));
    }

    @Test
    public void modelOpinionWithoutFirstGrade() throws Exception {
        invokeAndCheckResponse(
            "/data/model_opinions_first_grade.json",
            "/data/saasresponse/model_opinions_first_grade.json",
            "/api/opinion/model/950548");
        verifySaasRequest(withQueryParam("relev=formula=sort_by_karma_model_relevance;" +
            "calc=grade_boost:inset\\(#grade_id,0\\);calc=sort_hash:fnvhash_f32\\(zdocid_i64\\(\\)\\)"));
    }

    @Test
    public void modelOpinionWithNewRelevanceSort() throws Exception {
        invokeAndCheckResponse(
            "/data/model_opinion_with_votes_new_relevance_sorter.json",
            "/data/saasresponse/model_opinion_with_votes.json",
            "/api/opinion/model/950065?sort_by=new_relevance");
        verifySaasRequest(withQueryParam("relev=formula=sort_by_new_model_relevance;" +
            "calc=grade_boost:inset\\(#grade_id,0\\);calc=sort_hash:fnvhash_f32\\(zdocid_i64\\(\\)\\)"));
    }

    /**
     * есть нужный реарр флаг, сортировка не передана -> отдаём новую сортировку
     * @throws Exception
     */
    @Test
    public void rearrFlagNewRelevanceSort() throws Exception {
        invokeAndCheckResponse(
            "/data/model_opinion_with_votes_new_relevance_sorter.json",
            "/data/saasresponse/model_opinion_with_votes.json",
            "/api/opinion/model/950065",
            null,
            Map.of(RearrFlags.HEADER_NAME, "market_blender_bundles_for_inclid=17:eats_retail_shops_incut.json;" +
                "djid_forbid_eats_morda_default_shopFeed_report=0;reviews_sort_new_relevance"));
        verifySaasRequest(withQueryParam("relev=formula=sort_by_new_model_relevance;" +
            "calc=grade_boost:inset\\(#grade_id,0\\);calc=sort_hash:fnvhash_f32\\(zdocid_i64\\(\\)\\)"));
    }

    /**
     * нет нужного реарр флага, сортировка не передана -> отдаём старую сортировку
     * @throws Exception
     */
    @Test
    public void withoutRearrFlagNewRelevanceSort() throws Exception {
        invokeAndCheckResponse(
            "/data/model_opinion_with_votes.json",
            "/data/saasresponse/model_opinion_with_votes.json",
            "/api/opinion/model/950065",
            null,
            Map.of(RearrFlags.HEADER_NAME, "market_blender_bundles_for_inclid=17:eats_retail_shops_incut.json;" +
                "djid_forbid_eats_morda_default_shopFeed_report=0"));
        verifySaasRequest(withQueryParam("relev=formula=sort_by_karma_model_relevance;" +
            "calc=grade_boost:inset\\(#grade_id,0\\);calc=sort_hash:fnvhash_f32\\(zdocid_i64\\(\\)\\)"));
    }

    /**
     * есть нужный реарр флаг, явно передана сортировка по релевантности -> отдаём новую сортировку
     * @throws Exception
     */
    @Test
    public void rearrFlagNewRelevanceSortWithExplicitRelevance() throws Exception {
        invokeAndCheckResponse(
            "/data/model_opinion_with_votes_new_relevance_sorter.json",
            "/data/saasresponse/model_opinion_with_votes.json",
            "/api/opinion/model/950065?sort_by=relevance",
            null,
            Map.of(RearrFlags.HEADER_NAME, "market_blender_bundles_for_inclid=17:eats_retail_shops_incut.json;" +
                "djid_forbid_eats_morda_default_shopFeed_report=0;reviews_sort_new_relevance"));
        verifySaasRequest(withQueryParam("relev=formula=sort_by_new_model_relevance;" +
            "calc=grade_boost:inset\\(#grade_id,0\\);calc=sort_hash:fnvhash_f32\\(zdocid_i64\\(\\)\\)"));
    }

    @Test
    public void newSortWithFirstGradeByFixId() throws Exception {
        Map<String, String> params = new HashMap<>();
        String fixId = "594";
        params.put(ControllerConst.FIRST_GRADE_BY_FIX_ID, fixId);
        invokeAndCheckResponse(
            "/data/model_opinion_with_votes_new_relevance_sorter.json",
            "/data/saasresponse/model_opinion_with_votes.json",
            "/api/opinion/model/950065",
            params,
            Map.of(RearrFlags.HEADER_NAME, "market_blender_bundles_for_inclid=17:eats_retail_shops_incut.json;" +
                "djid_forbid_eats_morda_default_shopFeed_report=0;reviews_sort_new_relevance"));
        verifySaasRequest(withQueryParam("relev=formula=sort_by_new_model_relevance;calc=grade_boost:inset\\" +
            "(#s_fix_id," + fixId + "\\);calc=sort_hash:fnvhash_f32\\(zdocid_i64\\(\\)\\)"));
    }

    @Test
    public void testDuduplicationRearr() throws Exception {
        invokeAndCheckResponse(
            "/data/model_opinion_with_votes_new_relevance_sorter.json",
            "/data/saasresponse/model_opinion_with_votes.json",
            "/api/opinion/model/950065",
            null,
            Map.of(RearrFlags.HEADER_NAME, "reviews_sort_new_relevance;market_diversity_rearrange_type=2;market_diversity_rearrange_type=2;"));
    }

}
