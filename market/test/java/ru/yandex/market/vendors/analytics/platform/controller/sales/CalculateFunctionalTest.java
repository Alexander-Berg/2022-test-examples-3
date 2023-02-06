package ru.yandex.market.vendors.analytics.platform.controller.sales;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.vendors.analytics.core.model.common.language.Language;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

/**
 * Общий тест для ручек расчёта данных.
 *
 * @author antipov93.
 */
public abstract class CalculateFunctionalTest extends FunctionalTest {

    private static final String CALCULATE_PATH_PREFIX = "/calculate";

    protected static final long DASHBOARD_ID = 100L;

    protected static final String BASE_REPORT_URL = "http://report.tst.vs.market.yandex.net:17051/yandsearch"
            + "?place=filter_models&numdoc=500";

    @Autowired
    protected RestTemplate reportRestTemplate;

    protected MockRestServiceServer mockRestServiceServer;

    protected String getFullWidgetUrl(String widgetUrlSuffix) {
        return getFullWidgetUrl(widgetUrlSuffix, Language.RU);
    }

    protected String getFullWidgetUrl(String widgetUrlSuffix, Language language) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path(CALCULATE_PATH_PREFIX)
                .path(widgetUrlSuffix)
                .queryParam("language", language)
                .toUriString();
    }

    protected String getFullWidgetUrl(String widgetUrlSuffix, long dashboardId) {
        return getFullWidgetUrl(widgetUrlSuffix, dashboardId, Language.RU);
    }

    protected String getFullWidgetUrl(String widgetUrlSuffix, long dashboardId, Language language) {
        return UriComponentsBuilder.fromUriString(getFullWidgetUrl(widgetUrlSuffix, language))
                .queryParam("dashboardId", dashboardId)
                .queryParam("language", language)
                .toUriString();
    }

    @BeforeEach
    void resetMocks() {
        mockRestServiceServer = MockRestServiceServer.createServer(reportRestTemplate);
    }
}
