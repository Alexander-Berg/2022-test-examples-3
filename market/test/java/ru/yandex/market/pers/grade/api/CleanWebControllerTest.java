package ru.yandex.market.pers.grade.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.cleanweb.dto.CleanWebVerdictDto;
import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.model.core.ModReason;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.moderation.FilterType;
import ru.yandex.market.pers.grade.mock.CleanWebMvcMocks;
import ru.yandex.market.pers.grade.web.grade.GradeCreationHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CleanWebControllerTest extends MockedPersGradeTest {

    @Autowired
    private CleanWebMvcMocks cleanWebMvcMocks;
    @Autowired
    protected GradeCreationHelper gradeCreationHelper;

    @Test
    public void testNegativeVerdictsSaved() throws Exception {
        long gradeId = createShopGrade(ModState.READY);

        String body = createCleanWebCallback(gradeId, true, true, true);
        cleanWebMvcMocks.successfulCallback(body);

        check(gradeId, ModState.AUTOMATICALLY_REJECTED);
        checkAutoModResult(gradeId);
        assertVerdictResult(gradeId, true, true, true);
        checkModReasonOfflineCleanWeb(gradeId, ModReason.BY_FILTER_SILENT.forShop());
    }

    @Test
    public void testDifferentVerdictsSaved() throws Exception {
        long gradeId = createShopGrade(ModState.READY);

        String body = createCleanWebCallback(gradeId, false, true, false);
        cleanWebMvcMocks.successfulCallback(body);

        check(gradeId, ModState.AUTOMATICALLY_REJECTED);
        checkAutoModResult(gradeId);
        assertVerdictResult(gradeId, false, true, false);
        checkModReasonOfflineCleanWeb(gradeId, ModReason.RUDE.forShop());
    }

    @Test
    public void testShopFromReadyToAutoRejectByToxicAndObscene() throws Exception {
        long gradeId = createShopGrade(ModState.READY);

        String body = createCleanWebCallback(gradeId, true, true, false);
        cleanWebMvcMocks.successfulCallback(body);

        check(gradeId, ModState.AUTOMATICALLY_REJECTED);
        checkAutoModResult(gradeId);
        assertVerdictResult(gradeId, true, true, false);
        checkModReasonOfflineCleanWeb(gradeId, ModReason.BY_FILTER_SILENT.forShop());
    }

    @Test
    public void testShopFromReadyToAutoRejectByToxic() throws Exception {
        long gradeId = createShopGrade(ModState.APPROVED);

        String body = createCleanWebCallback(gradeId, true, false, false);
        cleanWebMvcMocks.successfulCallback(body);

        check(gradeId, ModState.AUTOMATICALLY_REJECTED);
        checkAutoModResult(gradeId);
        assertVerdictResult(gradeId, true, false, false);
        checkModReasonOfflineCleanWeb(gradeId, ModReason.BY_FILTER_SILENT.forShop());
    }

    @Test
    public void testShopFromApprovedToAutoRejectByToxic() throws Exception {
        long gradeId = createShopGrade(ModState.APPROVED);

        String body = createCleanWebCallback(gradeId, true, false, false);
        cleanWebMvcMocks.successfulCallback(body);

        check(gradeId, ModState.AUTOMATICALLY_REJECTED);
        checkAutoModResult(gradeId);
        assertVerdictResult(gradeId, true, false, false);
        checkModReasonOfflineCleanWeb(gradeId, ModReason.BY_FILTER_SILENT.forShop());
    }

    @Test
    public void testModelFromReadyToAutoRejectByObscene() throws Exception {
        long gradeId = createModelGrade(ModState.READY);

        String body = createCleanWebCallback(gradeId, false, true, false);
        cleanWebMvcMocks.successfulCallback(body);

        check(gradeId, ModState.AUTOMATICALLY_REJECTED);
        checkAutoModResult(gradeId);
        assertVerdictResult(gradeId, false, true, false);
        checkModReasonOfflineCleanWeb(gradeId, ModReason.RUDE.forModel());
    }


    @Test
    public void testModelFromReadyToAutoRejectByNoSense() throws Exception {
        long gradeId = createModelGrade(ModState.READY);

        String body = createCleanWebCallback(gradeId, false, false, true);
        cleanWebMvcMocks.successfulCallback(body);

        check(gradeId, ModState.AUTOMATICALLY_REJECTED);
        checkAutoModResult(gradeId);
        assertVerdictResult(gradeId, false, false, true);
        checkModReasonOfflineCleanWeb(gradeId, ModReason.UNINFORMATIVE.forModel());
    }

    @Test
    public void testShopNoFilter() throws Exception {
        long gradeId = createShopGrade(ModState.READY);

        String body = createCleanWebCallback(gradeId, false, false, false);
        cleanWebMvcMocks.successfulCallback(body);

        check(gradeId, ModState.READY);
        Long count = pgJdbcTemplate.queryForObject(
            "select count(*) from auto_mod_result where grade_id = ? and filter_type = ?",
            Long.class, gradeId, FilterType.NEGATIVE.getValue());
        assertEquals(0, count.longValue());
        assertVerdictResult(gradeId, false, false, false);
    }

    @Test
    public void testModelNoFilter() throws Exception {
        long gradeId = createModelGrade(ModState.READY);

        String body = createCleanWebCallback(gradeId, false, false, false);
        cleanWebMvcMocks.successfulCallback(body);

        check(gradeId, ModState.READY);
        Long count = pgJdbcTemplate.queryForObject(
            "select count(*) from auto_mod_result where grade_id = ? and filter_type = ?",
            Long.class, gradeId, FilterType.NEGATIVE.getValue());
        assertEquals(0, count.longValue());
        assertVerdictResult(gradeId, false, false, false);
    }

    @Test
    public void testDifferentKeysRequest() throws Exception {
        ObjectMapper om = new ObjectMapper();
        long gradeId = createModelGrade(ModState.READY);

        String body = createCleanWebCallback(gradeId, false, false, false);
        String secondBody = createCleanWebCallback(gradeId + 1, false, false, false);

        CleanWebVerdictDto cleanWebVerdictDto = om.readValue(body, CleanWebVerdictDto.class);
        CleanWebVerdictDto secondCleanWebVerdictDto = om.readValue(secondBody, CleanWebVerdictDto.class);
        cleanWebVerdictDto.getVerdicts().addAll(secondCleanWebVerdictDto.getVerdicts());

        cleanWebMvcMocks.callback(om.writeValueAsString(cleanWebVerdictDto), status().is4xxClientError());
    }

    @Test
    public void testServerErrorOnNotExistedGrade() {
        long gradeId = 12345L;
        String body = createCleanWebCallback(gradeId, true, true, true);
        cleanWebMvcMocks.successfulCallback(body);

        //check no record in table clean_web_verdict
        Long cleanWebVerdictCount = pgJdbcTemplate.queryForObject(
            "select count(*) from clean_web_verdict where grade_id = ?",
            Long.class, gradeId);
        assertEquals(Long.valueOf(0L), cleanWebVerdictCount);
    }

    private void check(long gradeId, ModState modState) {
        Integer value = pgJdbcTemplate.queryForObject("select mod_state from grade where id = ?", Integer.class,
            gradeId);
        assertEquals(modState, ModState.byValue(value));
    }

    private void checkAutoModResult(long gradeId) {
        String filterDescriprion = pgJdbcTemplate.queryForObject(
            "select FILTER_DESCRIPTION from auto_mod_result where grade_id = ? and filter_type = ?",
            String.class, gradeId, FilterType.NEGATIVE.getValue());
        assertTrue(filterDescriprion.contains("Clean web"));
    }

    private long createShopGrade(ModState modState) {
        ShopGrade shopGrade = gradeCreationHelper.constructShopGrade(123, 444);
        long gradeId = gradeCreationHelper.createApprovedGrade(shopGrade);
        gradeCreationHelper.updateModState(gradeId, modState);
        return gradeId;
    }

    private long createModelGrade(ModState modState) {
        ModelGrade modelGrade = gradeCreationHelper.constructModelGrade(123, 444L, null);
        long gradeId = gradeCreationHelper.createApprovedGrade(modelGrade);
        gradeCreationHelper.updateModState(gradeId, modState);
        return gradeId;
    }

    private void assertVerdictResult(long gradeId, boolean toxic, boolean obscene, boolean noSense) {
        String verdict = pgJdbcTemplate.queryForObject(
            "select verdict from clean_web_verdict where grade_id = ?",
            String.class, gradeId);

        JSONAssert.assertEquals("{\"text_auto_toxic\":\"" + toxic + "\"," +
                "\"text_auto_obscene\":\"" + obscene + "\"," +
                "\"text_auto_common_toloka_no_sense\":\"" + noSense + "\"}",
            verdict, false);

    }

    private void checkModReasonOfflineCleanWeb(long gradeId, Long expectedModReason) {
        Long modReason = pgJdbcTemplate.queryForObject(
            "select mod_reason from mod_grade where grade_id = ? order by id desc limit 1",
            Long.class, gradeId);
        assertEquals(expectedModReason, modReason);
    }

    private String createCleanWebCallback(long key, boolean toxic, boolean obscene, boolean noSense) {
        return
            "{\n" +
                "  \"verdicts\": [\n" +
                "    {\n" +
                "      \"entity\": \"review\",\n" +
                "      \"key\": \"" + key + "\",\n" +
                "      \"name\": \"text_auto_toxic\",\n" +
                "      \"source\": \"clean-web\",\n" +
                "      \"subsource\": \"custom\",\n" +
                "      \"value\": " + toxic + "\n" +
                "    },\n" +
                "    {\n" +
                "      \"entity\": \"review\",\n" +
                "      \"key\": \"" + key + "\",\n" +
                "      \"name\": \"text_auto_obscene\",\n" +
                "      \"source\": \"clean-web\",\n" +
                "      \"subsource\": \"custom\",\n" +
                "      \"value\": " + obscene + "\n" +
                "    },\n" +
                "    {\n" +
                "      \"entity\": \"review\",\n" +
                "      \"key\": \"" + key + "\",\n" +
                "      \"name\": \"text_auto_common_toloka_no_sense\",\n" +
                "      \"source\": \"clean-web\",\n" +
                "      \"subsource\": \"custom\",\n" +
                "      \"value\": " + noSense + "\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

}
