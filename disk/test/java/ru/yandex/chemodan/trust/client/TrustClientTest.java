package ru.yandex.chemodan.trust.client;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.trust.client.requests.CreateProductRequest;
import ru.yandex.chemodan.trust.client.requests.OrderRequest;
import ru.yandex.chemodan.trust.client.requests.Price;
import ru.yandex.chemodan.trust.client.responses.BasicResponse;
import ru.yandex.chemodan.trust.client.responses.TrustResponseStatus;
import ru.yandex.devtools.test.annotations.YaExternal;
import ru.yandex.misc.test.Assert;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ContextConfiguration(classes = {TrustClientIntegrationTestConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class TrustClientTest {
    @Autowired
    private TrustClient trustClient;

    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;

    @Before
    public void init() {
        objectMapper = new ObjectMapper();
        mockServer = MockRestServiceServer.createServer(trustClient.getRestTemplate());
    }


    @Test
    public void testSuccess() throws Exception {
        BasicResponse expected = new BasicResponse();
        expected.setStatus(TrustResponseStatus.success.name());

        for (MediaType type : Cf.list(MediaType.APPLICATION_JSON_UTF8, MediaType.APPLICATION_JSON)) {

            mockServer.expect(ExpectedCount.once(),
                            MockRestRequestMatchers.requestTo(new URI("https://trust-payments-test.paysys.yandex" +
                                    ".net:8028/trust-payments/v2/subscriptions/test")))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.OK)
                            .contentType(type)
                            .body(objectMapper.writeValueAsString(expected))
                    );

            BasicResponse actual = trustClient.getSubscription(OrderRequest.builder()
                    .orderId("test")
                    .trustServiceId(690)
                    .build());

            Assert.equals(expected.getStatus(), actual.getStatus());

            mockServer.reset();
        }
    }

    @Test
    public void testError5xx() throws Exception {
        BasicResponse expected = new BasicResponse();
        expected.setStatus(TrustResponseStatus.error.name());
        HttpStatus expectedCode = HttpStatus.INTERNAL_SERVER_ERROR;

        for (MediaType type : Cf.list(MediaType.APPLICATION_JSON_UTF8, MediaType.APPLICATION_JSON)) {
            mockServer.expect(ExpectedCount.once(),
                            MockRestRequestMatchers.requestTo(new URI("https://trust-payments-test.paysys.yandex" +
                                    ".net:8028/trust-payments/v2/subscriptions/test")))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(expectedCode)
                            .contentType(type)
                            .body(objectMapper.writeValueAsString(expected)));

            Assert.assertThrows(() -> trustClient.getSubscription(OrderRequest.builder()
                            .orderId("test")
                            .trustServiceId(690)
                            .build()),
                    TrustException.class,
                    e -> e.getHttpCode().isMatch(c -> Objects.equals(c, expectedCode))
                            && Objects.equals(e.getStatus(), expected.getStatus()));

            mockServer.reset();
        }
    }

    @Test
    public void testError429() throws Exception {
        HttpStatus expectedCode = HttpStatus.TOO_MANY_REQUESTS;

        mockServer.expect(ExpectedCount.once(),
                        MockRestRequestMatchers.requestTo(new URI("https://trust-payments-test.paysys.yandex" +
                                ".net:8028/trust-payments/v2/subscriptions/test")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(expectedCode)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body("limited"));

        Assert.assertThrows(() -> trustClient.getSubscription(OrderRequest.builder()
                        .orderId("test")
                        .trustServiceId(690)
                        .build()),
                TrustException.class,
                e -> e.getHttpCode().isMatch(c -> Objects.equals(c, expectedCode))
                        && Objects.isNull(e.getStatus()));
    }


    @Test
    public void testErrorParseBody() throws Exception {
        HttpStatus expectedCode = HttpStatus.INTERNAL_SERVER_ERROR;

        for (MediaType type : Cf.list(MediaType.APPLICATION_JSON_UTF8, MediaType.APPLICATION_JSON)) {
            mockServer.expect(ExpectedCount.once(),
                            MockRestRequestMatchers.requestTo(new URI("https://trust-payments-test.paysys.yandex" +
                                    ".net:8028/trust-payments/v2/subscriptions/test")))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(expectedCode)
                            .contentType(type)
                            .body("You shall not parse!"));

            Assert.assertThrows(() -> trustClient.getSubscription(OrderRequest.builder()
                            .orderId("test")
                            .trustServiceId(690)
                            .build()),
                    TrustException.class,
                    e -> e.getHttpCode().isMatch(c -> Objects.equals(c, expectedCode))
                            && Objects.isNull(e.getStatus()));

            mockServer.reset();
        }
    }

    @Test
    public void testErrorWithoutContentType() throws Exception {
        HttpStatus expectedCode = HttpStatus.INTERNAL_SERVER_ERROR;

            mockServer.expect(ExpectedCount.once(),
                            MockRestRequestMatchers.requestTo(new URI("https://trust-payments-test.paysys.yandex" +
                                    ".net:8028/trust-payments/v2/subscriptions/test")))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(expectedCode)
                            .contentType(null)
                            .body("You shall not parse!"));

            Assert.assertThrows(() -> trustClient.getSubscription(OrderRequest.builder()
                            .orderId("test")
                            .trustServiceId(690)
                            .build()),
                    TrustException.class,
                    e -> e.getHttpCode().isMatch(c -> Objects.equals(c, expectedCode))
                            && Objects.isNull(e.getStatus()));

            mockServer.reset();
    }


    @Test
    @YaExternal
    @Ignore
    public void testCreateProduct() {
        trustClient.createProduct(CreateProductRequest.builder()
                .productId("blabla").name("blabla2")
                .prices(Cf.list(new Price("225", Instant.now(), BigDecimal.TEN, "RUB"))).build());
    }


}
