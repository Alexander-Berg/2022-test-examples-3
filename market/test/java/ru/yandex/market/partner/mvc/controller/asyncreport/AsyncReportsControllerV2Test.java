package ru.yandex.market.partner.mvc.controller.asyncreport;

import java.time.OffsetDateTime;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.mbi.asyncreport.FilterReportRequestDTO;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.web.paging.PageTokenHelper;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static ru.yandex.market.core.asyncreport.model.ReportsType.FULFILLMENT_ORDERS;

/**
 * Тесты для {@link AsyncReportsControllerV2}.
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "AsyncReportsControllerTestV2.common.before.csv")
class AsyncReportsControllerV2Test extends FunctionalTest {

    @Autowired
    private EnvironmentService environmentService;

    private static final PageTokenHelper pageTokenHelper = new PageTokenHelper(OBJECT_MAPPER);
    private static final String offset = OffsetDateTime.now().getOffset().toString();

    @Test
    @DisplayName("Проверяет, что получим все отчёты поставщика нескольких определённых типов V2")
    @DbUnitDataSet(
            before = "AsyncReportsControllerTestV2.reports.before.csv"
    )
    void getReportInfosV2_multi_paging_1() {
        var token_page_2 = pageTokenHelper.createNextToken(
                OffsetDateTime.parse("2019-08-13T00:00:00" + offset));
        var token_page_3 = pageTokenHelper.createNextToken(
                OffsetDateTime.parse("2019-07-13T00:00:00" + offset));

        String url_page_1 = getUrlGetReport(21, "FULFILLMENT_ORDERS,DSBS_ORDERS", 3, null);
        String url_page_2 = getUrlGetReport(21, "FULFILLMENT_ORDERS,DSBS_ORDERS", 3, token_page_2);
        String url_page_3 = getUrlGetReport(21, "FULFILLMENT_ORDERS,DSBS_ORDERS", 3, token_page_3);

        checkReportInfos(url_page_1, "get-report-infos-multi.page-1.expected.json", token_page_2);
        checkReportInfos(url_page_2, "get-report-infos-multi.page-2.expected.json", token_page_3);
        checkReportInfos(url_page_3, "get-report-infos-multi.page-3.expected.json", null);
    }

    @Test
    @DisplayName("Проверяет, что получим все отчёты партнеров")
    @DbUnitDataSet(
            before = "AsyncReportsControllerTestV2.reports.before.csv"
    )
    void getReportInfosV2_post() {
        var token_page_2 = pageTokenHelper.createNextToken(
                OffsetDateTime.parse("2019-08-13T00:00:00" + offset));

        String url_page_1 = postUrlGetReport(null);

        checkReportInfosPost(url_page_1, 5, List.of(1L, 3L), List.of(FULFILLMENT_ORDERS), false,
                "get-report-infos-post.default.expected.json", token_page_2);
    }

    @Test
    @DisplayName("Проверяет, что получим все отчёты партнеров и бизнеса")
    @DbUnitDataSet(
            before = "AsyncReportsControllerTestV2.reports.before.csv"
    )
    void getReportInfosV2_post_reportByBusiness() {
        var token_page_2 = pageTokenHelper.createNextToken(
                OffsetDateTime.parse("2019-08-13T00:00:00" + offset));

        String url_page_1 = postUrlGetReport(null);

        checkReportInfosPost(url_page_1, 5, List.of(1L, 3L), List.of(FULFILLMENT_ORDERS), true,
                "get-report-infos-post.business-report.expected.json", token_page_2);
    }

    void checkReportInfos(String url, String jsonExcepted, @Nullable String token) {
        environmentService.setValue("AsyncReportsController.get-reports.countLastDays", "99999");
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), jsonExcepted)
                        .withVariable("zoneOffset", offset)
                        .withVariable("next_token", token)
                        .toString()
        );
    }

    void checkReportInfosPost(String url, long businessId, List<Long> partnerIds, List<ReportsType> reportsTypes,
                              boolean businessReport, String jsonExcepted, @Nullable String token) {
        environmentService.setValue("AsyncReportsController.get-reports.countLastDays", "99999");

        var filter = new FilterReportRequestDTO(businessId, partnerIds, reportsTypes, businessReport);
        ResponseEntity<String> response = FunctionalTestHelper.post(url, filter);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), jsonExcepted)
                        .withVariable("zoneOffset", offset)
                        .withVariable("next_token", token)
                        .toString()
        );
    }

    private String getUrlGetReport(int campaign_id, String reportType, int limit, @Nullable String token) {
        return getUrl(2, "/get-reports?campaign_id=" + campaign_id +
                "&reportType=" + reportType +
                "&limit=" + limit +
                (token != null ? "&page_token=" + token : "")
        );
    }

    private String postUrlGetReport(@Nullable String token) {
        return getUrl(2, "/get-reports" +
                (token != null ? "?page_token=" + token : "")
        );
    }

    private String getUrl(int v, String relatedPath) {
        return baseUrl + "/v" + v + "/async-reports" + relatedPath;
    }
}
