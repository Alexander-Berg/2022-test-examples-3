package ru.yandex.market.cocon.mvc.cabinet;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.Header;
import org.mockserver.model.Headers;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.mockserver.model.KeyMatchStyle;
import org.mockserver.model.MediaType;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.market.cocon.FunctionalTest;
import ru.yandex.market.cocon.model.CabinetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Functional tests for {@link CabinetController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "analyticsPages.csv")
class AnalyticsCabinetControllerTest extends FunctionalTest {

    private static final long UID = 1000L;
    private static final String PAGE = "market-analytics-platform:page:layout";
    private static final String ALLOWED_FEATURE = "analytics:dashboards:read";
    private static final String PROHIBITED_FEATURE = "analytics:userManagement:write";

    @Autowired
    private ClientAndServer mockServer;

    @BeforeEach
    void initMockServer() {
        String analyticsRequestBody = "" +
                "{\"uid\": \"1000\"}";
        String analyticsResponseBody = "" +
                "{" +
                "  \"hasAccess\": \"true\"" +
                "}";
        mockServer.when(
                HttpRequest.request()
                .withMethod("POST")
                .withPath("/check")
                .withQueryStringParameters(
                        Parameter.param("checker", "analyticsUserChecker"),
                        Parameter.param("params", "")
                )
                .withBody(JsonBody.json(analyticsRequestBody, MatchType.ONLY_MATCHING_FIELDS))
                .withHeaders(new Headers(
                        Header.header("Content-Type", "application/json;\\s*charset=UTF-8"),
                        Header.header("X-Market-Req-ID", ".*")//,
//                        Header.header("X-Ya-Service-Ticket", ".*")
                ).withKeyMatchStyle(KeyMatchStyle.MATCHING_KEY)
                )
        ).respond(
                HttpResponse.response()
                .withStatusCode(HttpStatus.OK.value())
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(JsonBody.json(analyticsResponseBody))
        );
    }

    @Test
    @DisplayName("Получение конфига для страниц аналитической платформы")
    void analyticsPages() {
        String response = getPages(CabinetType.ANALYTICS_PLATFORM, UID);

        assertThat(response,
                hasJsonPath(
                        "$.result" +
                                ".pages[?(@.name=='" + PAGE + "')]" +
                                ".roles.result",
                        is(List.of(true))
                ));

    }

    @Test
    @DisplayName("Получение разрешённой фичи для страницы аналитической платформы")
    void analyticsFeatureAllowed() {
        String response = getPage(CabinetType.ANALYTICS_PLATFORM, PAGE, UID);
        assertThat(response,
                hasJsonPath(
                        "$.result" +
                                ".pages[?(@.name=='" + PAGE + "')]" +
                                ".features[?(@.name=='" + ALLOWED_FEATURE + "')]" +
                                ".roles.result",
                        is(List.of(true))
                ));
    }

    @Test
    @DisplayName("Получение запрещённой фичи для страницы аналитической платформы")
    void analyticsFeatureProhibited() {
        String response = getPage(CabinetType.ANALYTICS_PLATFORM, PAGE, UID);
        assertThat(response,
                hasJsonPath(
                        "$.result" +
                                ".pages[?(@.name=='" + PAGE + "')]" +
                                ".features[?(@.name=='" + PROHIBITED_FEATURE + "')]" +
                                ".roles.result",
                        is(List.of(false))
                ));
    }
}
