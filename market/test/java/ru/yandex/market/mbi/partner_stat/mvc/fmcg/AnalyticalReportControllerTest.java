package ru.yandex.market.mbi.partner_stat.mvc.fmcg;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.mbi.partner_stat.FunctionalTest;
import ru.yandex.market.mbi.partner_stat.util.ReportTestUtil;

/**
 * Тесты для {@link AnalyticalReportController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class AnalyticalReportControllerTest extends FunctionalTest {

    private static final String SHOWS_REPORT_URL = "/fmcg/{campaignId}/report/shows";
    private static final String FUNNEL_REPORT_URL = "/fmcg/{campaignId}/report/funnel";
    private static final String CART_REPORT_URL = "/fmcg/{campaignId}/report/cart";
    private static final String SHOP_POPULARITY_REPORT_URL = "/fmcg/{campaignId}/report/shop/popularity";
    private static final String PROMO_POPULARITY_REPORT_URL = "/fmcg/{campaignId}/report/promo/popularity";

    @Test
    @DisplayName("Получение отчета по показам")
    void testShowsReport() throws IOException {
        final String url = getUrl(SHOWS_REPORT_URL, 123);
        final ResponseEntity<byte[]> result = send(url, "json/showsReport.request.json");
    }

    @Test
    @DisplayName("Получение отчета Воронка")
    void testFunnelReport() throws IOException {
        final String url = getUrl(FUNNEL_REPORT_URL, 123);
        final ResponseEntity<byte[]> result = send(url, "json/funnelReport.request.json");
    }

    @Test
    @DisplayName("Получение отчета по корзинам")
    void testCartReport() throws IOException {
        final String url = getUrl(CART_REPORT_URL, 123);
        final ResponseEntity<byte[]> result = send(url, "json/baseReport.request.json");
    }

    @Test
    @DisplayName("Получение отчета по популярности магазина")
    void testShopPopularityReport() throws IOException {
        final String url = getUrl(SHOP_POPULARITY_REPORT_URL, 123);
        final ResponseEntity<byte[]> result = send(url, "json/baseReport.request.json");
    }

    @Test
    @DisplayName("Получение отчета по поплуярности промо-акций")
    void testPromoPopularityReport() throws IOException {
        final String url = getUrl(PROMO_POPULARITY_REPORT_URL, 123);
        final ResponseEntity<byte[]> result = send(url, "json/baseReport.request.json");
    }

    private String getUrl(final String path, final long campaignId) {
        return baseUrl() + StringUtils.replace(path, "{campaignId}", String.valueOf(campaignId));
    }

    private static ResponseEntity<byte[]> send(final String url, final String requestFilePath) throws IOException {
        return ReportTestUtil.post(url, AnalyticalReportControllerTest.class, requestFilePath, byte[].class);
    }
}
