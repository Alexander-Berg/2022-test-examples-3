package ru.yandex.market.tpl.common.transferact.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.tpl.common.transferact.client.api.DocumentApi;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "external.market-transfer-act.url=http://dev/null",
        "external.market-transfer-act.tvmServiceId=123",
        "external.market-transfer-act.connectTimeoutMillis=100",
        "external.market-transfer-act.readTimeoutMillis=100",
        "external.market-transfer-act.maxConnTotal=1",
        "external.market-transfer-act.apiKey=key",
})
@AutoConfigureMockRestServiceServer
@RestClientTest(DocumentApi.class)
@ContextConfiguration(classes = MarketTransferActClientConfiguration.class)
public class DocumentClientTest {

    @Autowired
    private RestTemplate marketTransferActRestTemplate;

    @Autowired
    private DocumentApi documentApi;

    @MockBean
    private TvmClient tvmClient;

    @Test
    void test() {
        ByteArrayResource zip = new ByteArrayResource(new byte[]{1, 2, 3});
        MockRestServiceServer mockServer =
                MockRestServiceServer.bindTo(marketTransferActRestTemplate).build();
        mockServer
                .expect(ExpectedCount.once(), requestTo("http://dev/null/document?itemExternalId=123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("apiKey", "key"))
                .andRespond(withStatus(HttpStatus.OK)
                        .body(zip)
                        .contentType(MediaType.parseMediaType("application/zip"))
                );

        documentApi.documentGet("123", null);
    }
}
