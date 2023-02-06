package ru.yandex.market.cocon.mvc.cabinet;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.cocon.CabinetService;
import ru.yandex.market.cocon.FunctionalTest;
import ru.yandex.market.cocon.model.CabinetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.StringTestUtil;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@DbUnitDataSet(before = "mbiPages.csv")
class SlimPagesControllerTest extends FunctionalTest {
    private static final long UID = 10000L;
    private static final String PAGE = "market-partner:html:delivery-orders-create:get";

    @Autowired
    private ClientAndServer mockServer;

    @Autowired
    private CabinetService cabinetService;

    @BeforeEach
    void setUp() {
        mockServer.when(
                HttpRequest.request()
                        .withMethod("POST")
                        .withPath("/checker/result")
                        .withQueryStringParameters(
                                Parameter.param("uid", "10000")
                        ),
                Times.once()
        ).respond(
                HttpResponse.response()
                .withStatusCode(HttpStatus.OK.value())
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(JsonBody.json(
                        StringTestUtil.getString(AnalyticsCabinetControllerTest.class, "mbiResponseBody.json")
                ))
        );
    }

    @Test
    @DisplayName("Получение облегчённого конфига для страниц доставки, где есть фичи по умолчанию")
    void slimPages() {
        String response = getSlimPages(CabinetType.DELIVERY, UID);
        String features = "$.result.pages[?(@.name=='" + PAGE + "')].features";

        assertThat(response, hasJsonPath(features, is(List.of())));
    }


    private String slimPagesUrl(CabinetType cabinetType, long uid) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/cabinet/{cabinet}/slim-pages?uid={uid}")
                .buildAndExpand(cabinetType.getId(), uid)
                .toUriString();
    }

    private String getSlimPages(CabinetType cabinetType, long uid) {
        String pagesUrl = slimPagesUrl(cabinetType, uid);

        return FunctionalTestHelper.get(pagesUrl).getBody();
    }
}
