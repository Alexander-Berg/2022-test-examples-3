package ru.yandex.market.cocon.mvc;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.cocon.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;

@DbUnitDataSet(before = "actions.csv")
class AllowedActionsControllerTest extends FunctionalTest {

    @Autowired
    private ClientAndServer mockServer;

    void initMockServer(Map<String, List<String>> expectedParams) {
        String requestBody = readJsonAsString("requestBody");
        String responseBody = readJsonAsString("responseBody");

        mockServer.when(
                HttpRequest.request()
                .withMethod("POST")
                .withPath("/checker/result")
                .withBody(JsonBody.json(requestBody))
                .withQueryStringParameters(expectedParams),
                Times.exactly(2)
        ).respond(
                HttpResponse.response()
                .withStatusCode(HttpStatus.OK.value())
                .withBody(JsonBody.json(responseBody))
                .withContentType(MediaType.APPLICATION_JSON)
        );
    }

    @Test
    void testGetAllowedActions() {
        initMockServer(Map.of(
                "uid", List.of("1005"),
                "format", List.of("json"),
                "ADDED_INFO", List.of("campaigns/steps@GET,uploadContent@POST"),
                "id", List.of("4")
        ));
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl() +
                        "/getAllowedActions?format=json&id={campaignId}&uid={userId}&ADDED_INFO={actions}",
                4, 1005, new StringJoiner(",")
                        .add("campaigns/steps@GET")
                        .add("uploadContent@POST")
                        .toString());
        JsonTestUtil.assertEquals(response, "[\"campaigns/steps@GET\"]");
    }

    @Test
    void testTrashAction() {
        initMockServer(Map.of(
                "uid", List.of("1005"),
                "format", List.of("json"),
                "ADDED_INFO", List.of("campaigns/steps@GET,uploadContent@POST,dasdaf"),
                "id", List.of("4")
        ));
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl() +
                        "/getAllowedActions?format=json&id={campaignId}&uid={userId}&ADDED_INFO={actions}",
                4, 1005, new StringJoiner(",")
                        .add("campaigns/steps@GET")
                        .add("uploadContent@POST")
                        .add("dasdaf")
                        .toString());
        JsonTestUtil.assertEquals(response, "[\"campaigns/steps@GET\"]");
    }

    private String readJsonAsString(String jsonFile) {
        return StringTestUtil.getString(AllowedActionsControllerTest.class, jsonFile + ".json");
    }
}
