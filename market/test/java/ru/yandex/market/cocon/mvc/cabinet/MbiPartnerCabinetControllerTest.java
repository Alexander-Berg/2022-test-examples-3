package ru.yandex.market.cocon.mvc.cabinet;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
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

import ru.yandex.market.cocon.FunctionalTest;
import ru.yandex.market.cocon.model.CabinetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.CoreMatchers.is;

@DbUnitDataSet(before = "mbiPages.csv")
public class MbiPartnerCabinetControllerTest extends FunctionalTest {

    private static final long UID = 10000L;
    private static final String PAGE = "market-partner:html:messages:get";

    @Autowired
    private ClientAndServer mockServer;

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
                                StringTestUtil.getString(getClass(),
                                        "MbiPartnerCabinetControllerTest.withLinks.response.json")
                        ))
        );
    }

    @Test
    void testAuthsWithLinks() {
        String response = getPage(CabinetType.USER, PAGE, UID);
        MatcherAssert.assertThat(response, hasJsonPath(
                "$.result.roles.result",
                is(true)
        ));
    }

}
