package ru.yandex.market.logistics.lom.service.yt;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YtTransferServiceImplTest extends AbstractContextualTest {

    @Autowired
    YtTransferService ytTransferService;

    @Autowired
    RestTemplate ytTransferRestTemplate;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(ytTransferRestTemplate);
    }

    @Test
    public void getTablesTest() {
        mockServer.expect(requestTo("http://local/v1/endpoint/endpId"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess()
                    .body("{\"managed_postgres_source\": {\"tables\": [\"public.orders\"]}}")
                    .contentType(MediaType.APPLICATION_JSON)
            );

        List<String> table = ytTransferService.getTables();

        softly.assertThat(table).containsExactly("orders");

        mockServer.verify();
    }
}
