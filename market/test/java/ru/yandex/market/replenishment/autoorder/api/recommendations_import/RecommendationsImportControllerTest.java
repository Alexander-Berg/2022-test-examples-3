package ru.yandex.market.replenishment.autoorder.api.recommendations_import;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.dto.RecommendationsImportRequest;
import ru.yandex.market.replenishment.autoorder.dto.YtTableEventDTO;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.replenishment.autoorder.dto.RecommendationsImportEvent.INTER_WAREHOUSE;
import static ru.yandex.market.replenishment.autoorder.dto.RecommendationsImportEvent.TENDER;
import static ru.yandex.market.replenishment.autoorder.dto.RecommendationsImportEvent.TYPE_1P;
import static ru.yandex.market.replenishment.autoorder.dto.RecommendationsImportEvent.TYPE_3P;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.ImportEvents.REPLENISHMENT_LOAD;
import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.dtoToString;
@WithMockLogin
public class RecommendationsImportControllerTest extends ControllerTest {

    private static final String RECOMMENDATIONS_TABLE_PATH = "//home/market/production/replenishment/order_planning" +
            "/2020-12-19/outputs/recommendations";

    @Before
    public void mockWorkbookConfigAndTimeService() {
        setTestTime(LocalDateTime.of(2020, 12, 22, 0, 0));
    }

    @Test
    public void testConsumeEventWithEmptyName() throws Exception {
        YtTableEventDTO event = new YtTableEventDTO();
        event.setEventName("");
        event.setTablePath(RECOMMENDATIONS_TABLE_PATH);

        mockMvc.perform(post("/api/v1/events")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(event)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationsImportControllerTest.importStatus.before.csv")
    public void testRecommendationsImportStatus() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/import-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeProcessed1p").value("2020-12-20T07:44:03.413444"))
                .andExpect(jsonPath("$.timeProcessed3p").value("2020-12-20T07:36:50.99825"))
                .andExpect(jsonPath("$.timeProcessedTender").value("2020-12-20T07:28:00.475649"))
                .andExpect(jsonPath("$.date1p").value("2020-12-19"))
                .andExpect(jsonPath("$.date3p").value("2020-12-20"))
                .andExpect(jsonPath("$.dateTender").value("2020-12-20"));
    }

    @Test
    public void testConsumeEventWithEmptyTablePath() throws Exception {
        YtTableEventDTO event = new YtTableEventDTO();
        event.setEventName(REPLENISHMENT_LOAD);
        event.setTablePath("");

        mockMvc.perform(post("/api/v1/events")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(event)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationsImportControllerTest.consumeEvent.before.csv",
            after = "RecommendationsImportControllerTest.consumeEvent.after.csv")
    public void testConsumeEventOk() throws Exception {
        YtTableEventDTO event = new YtTableEventDTO();
        event.setEventName(REPLENISHMENT_LOAD);
        event.setTablePath(RECOMMENDATIONS_TABLE_PATH);

        mockMvc.perform(post("/api/v1/events")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(event)))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationsImportControllerTest.consumeEventIfImportWasToday.before.csv",
        after = "RecommendationsImportControllerTest.consumeEventIfImportWasToday.after.csv")
    public void testConsumeEventIfImportWasToday() throws Exception {
        YtTableEventDTO event = new YtTableEventDTO();
        event.setEventName(REPLENISHMENT_LOAD);
        event.setTablePath(RECOMMENDATIONS_TABLE_PATH);

        mockMvc.perform(post("/api/v1/events")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(event)))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("admin")
    @DbUnitDataSet(before = "RecommendationsImportControllerTest.reimport.before.csv",
            after = "RecommendationsImportControllerTest.reimport.after.csv")
    public void testRecommendationReimport_simpleV2() throws Exception {
        mockMvc.perform(post("/api/v2/admin/recommendations-reimport")
                .contentType(APPLICATION_JSON_UTF8)
                .content(TestUtils.dtoToString(new RecommendationsImportRequest(TYPE_1P, null))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("admin")
    @DbUnitDataSet(before = "RecommendationsImportControllerTest.reimport.before2.csv",
            after = "RecommendationsImportControllerTest.reimport3p.after.csv")
    public void testRecommendationReimport_3p_simpleV2() throws Exception {
        mockMvc.perform(post("/api/v2/admin/recommendations-reimport")
                .contentType(APPLICATION_JSON_UTF8)
                .content(TestUtils.dtoToString(new RecommendationsImportRequest(TYPE_3P, null))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("admin")
    @DbUnitDataSet(before = "RecommendationsImportControllerTest.reimport.before2.csv",
            after = "RecommendationsImportControllerTest.reimport_tender.after.csv")
    public void testRecommendationReimport_tender_simpleV2() throws Exception {
        mockMvc.perform(post("/api/v2/admin/recommendations-reimport")
                .contentType(APPLICATION_JSON_UTF8)
                .content(TestUtils.dtoToString(new RecommendationsImportRequest(TENDER, null))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("admin")
    @DbUnitDataSet(before = "RecommendationsImportControllerTest.reimport.before.csv",
            after = "RecommendationsImportControllerTest.testReimportWithPath.after.csv")
    public void testRecommendationReimport_simpleWithCustomPathV2() throws Exception {
        mockMvc.perform(post("/api/v2/admin/recommendations-reimport")
                .contentType(APPLICATION_JSON_UTF8)
                .content(TestUtils.dtoToString(new RecommendationsImportRequest(TYPE_1P,
                        RECOMMENDATIONS_TABLE_PATH + "_custom"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("admin")
    @DbUnitDataSet(before = "RecommendationsImportControllerTest.reimport_before_today.before.csv",
            after = "RecommendationsImportControllerTest.reimport_before_today.after.csv")
    public void testRecommendationReimport_BeforeTodayV2() throws Exception {
        mockMvc.perform(post("/api/v2/admin/recommendations-reimport")
                .contentType(APPLICATION_JSON_UTF8)
                .content(TestUtils.dtoToString(new RecommendationsImportRequest(TYPE_1P, null))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("admin")
    @DbUnitDataSet(before = "RecommendationsImportControllerTest.reimport_before_today_v3.before.csv",
        after = "RecommendationsImportControllerTest.reimport_before_today_v3.after.csv")
    public void testRecommendationReimport_BeforeTodayV3() throws Exception {
        mockMvc.perform(post("/api/v2/admin/recommendations-reimport")
                .contentType(APPLICATION_JSON_UTF8)
                .content(TestUtils.dtoToString(new RecommendationsImportRequest(TYPE_1P,
                    "//home/market/production/replenishment/order_planning/2020-12-21/outputs/recommendations"))))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("admin")
    @DbUnitDataSet(
        before = "RecommendationsImportControllerTest.import_interwh.before.csv",
        after = "RecommendationsImportControllerTest.import_interwh.after.csv")
    public void testRecommendationReimport_ImportInterWh() throws Exception {
        mockMvc.perform(post("/api/v2/admin/recommendations-reimport")
                .contentType(APPLICATION_JSON_UTF8)
                .content(TestUtils.dtoToString(new RecommendationsImportRequest(INTER_WAREHOUSE, null))))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("admin")
    @DbUnitDataSet(
        before = "RecommendationsImportControllerTest.import_interwh_with_already_event.before.csv",
        after = "RecommendationsImportControllerTest.import_interwh_with_already_event.after.csv")
    public void testRecommendationReimport_ImportInterWhWithAlreadyEvent() throws Exception {
        mockMvc.perform(post("/api/v2/admin/recommendations-reimport")
                .contentType(APPLICATION_JSON_UTF8)
                .content(TestUtils.dtoToString(new RecommendationsImportRequest(INTER_WAREHOUSE, null))))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("admin")
    @DbUnitDataSet(before = "RecommendationsImportControllerTest.reimport_while_importing_2.before.csv")
    public void testRecommendationReimport_ImportingNow2V2() throws Exception {
        mockMvc.perform(post("/api/v2/admin/recommendations-reimport")
                .contentType(APPLICATION_JSON_UTF8)
                .content(TestUtils.dtoToString(new RecommendationsImportRequest(TYPE_1P, null))))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Рекомендации уже импортируются")
                );
    }

    @Test
    @WithMockLogin("not-admin")
    @DbUnitDataSet(before = "RecommendationsImportControllerTest.reimport.before.csv")
    public void testRecommendationReimport403V2() throws Exception {
        mockMvc.perform(post("/api/v2/admin/recommendations-reimport")
                .contentType(APPLICATION_JSON_UTF8)
                .content(TestUtils.dtoToString(new RecommendationsImportRequest(TYPE_1P, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Access is denied"));
    }
}
