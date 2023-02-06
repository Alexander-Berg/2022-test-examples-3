package ru.yandex.market.delivery.mdbapp.testutils;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;

import static org.hamcrest.core.StringContains.containsString;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MockUtils {

    private MockUtils() {
    }

    public static void prepareMockServer(MockRestServiceServer mockServer,
                                         String urlPath) throws IOException {
        prepareMockServer(mockServer, urlPath, null, null, null);
    }

    public static void prepareMockServer(MockRestServiceServer mockServer,
                                         String urlPath,
                                         String responseFilePath) throws IOException {
        prepareMockServer(mockServer, urlPath, null, responseFilePath, MediaType.APPLICATION_JSON_UTF8);
    }

    public static void prepareMockServer(MockRestServiceServer mockServer,
                                         String urlPath,
                                         String requestFilePath,
                                         String responseFilePath,
                                         MediaType mediaType) throws IOException {
        ResponseActions responseActions = mockServer.expect(manyTimes(), requestTo(containsString(urlPath)));

        if (requestFilePath != null) {
            responseActions.andExpect(content().json(
                ResourceUtils.getFileContent(requestFilePath)
            ));
        }

        if (responseFilePath != null) {
            responseActions.andRespond(
                withSuccess(
                    ResourceUtils.getFileContent(responseFilePath),
                    mediaType
                )
            );
        } else {
            responseActions.andRespond(withStatus(HttpStatus.OK));
        }
    }
}
