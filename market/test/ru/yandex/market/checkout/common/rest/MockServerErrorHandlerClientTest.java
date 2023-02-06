package ru.yandex.market.checkout.common.rest;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Смысл этого теста, что ситуации неправильного ответа сервера, и ситуации отвалившегося load balancer,
 * в клиенте различаются.
 *
 * @author Alexander Semenov (alxsemn@yandex-team.ru)
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:WEB-INF/checkouter-client.xml")
public class MockServerErrorHandlerClientTest {

    private static final String MOCK_SERVER_URL = "http://fake.server.com";

    @Autowired
    private CheckouterClient checkouterClient;
    @Autowired
    private RestTemplate checkouterRestTemplate;

    private MockRestServiceServer server;

    @BeforeEach
    public void setUp() throws IOException {
        checkouterClient.setServiceURL(MOCK_SERVER_URL);
        server = MockRestServiceServer.createServer(checkouterRestTemplate);
    }

    @Test
    public void handleIncorrectJsonWithDifferentFields() {
        server.expect(
                requestTo(
                        containsString(MOCK_SERVER_URL + "/orders/cancellation-substatuses")
                )
        ).andExpect(method(HttpMethod.GET)).andRespond(
                withSuccess("{\"cancellationRules\": incorrect_value}",
                        MediaType.APPLICATION_JSON));


        try {
            checkouterClient.getCancellationRules(ClientRole.SYSTEM);
            fail("Didn't throw RestClientException");
        } catch (RestClientException expectedException) {
            assertTrue(expectedException.getCause() instanceof HttpMessageNotReadableException);
        } catch (Exception e) {
            fail("Didn't throw RestClientException");
        }
    }

    @Test
    public void handle503ServerResponse() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            server.expect(
                    requestTo(
                            containsString(MOCK_SERVER_URL + "/orders/cancellation-substatuses")
                    )
            ).andExpect(method(HttpMethod.GET)).andRespond(
                    withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                            .body("<!DOCTYPE html>\n" +
                                    "<html>\n" +
                                    "   <head>\n" +
                                    "      <meta charset=\"utf-8\" />\n" +
                                    "      <title>HTML Document</title>\n" +
                                    "   </head>\n" +
                                    "   <body>\n" +
                                    "      <p>\n" +
                                    "        Bad response from Load Balancer\n" +
                                    "      </p>\n" +
                                    "   </body>\n" +
                                    "</html>").contentType(MediaType.TEXT_HTML));

            checkouterClient.getCancellationRules(ClientRole.SYSTEM);
            fail("Didn't throw RestClientException");
        });
    }
}
