package ru.yandex.market.ff.tvm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.ff.base.TvmIntegrationTest;
import ru.yandex.market.ff.service.StockService;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class UsingTvmInIntegrationWithSSTest extends TvmIntegrationTest {

    @Autowired
    private StockService stockService;

    @Autowired
    @Qualifier("stockStorageRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private TvmClient iTvmClient;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void init() {
        Mockito.when(iTvmClient.getServiceTicketFor(2011222)).thenReturn("ServiceTicketSS");
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void testInteractionWithTvm() {
        ResponseActions responseActions = mockServer.expect(
            requestTo("https://bos.vs.market.yandex.net:443/outbound/1"));

        responseActions
            .andExpect(header("X-Ya-Service-Ticket", "ServiceTicketSS"));

        ResponseCreator response = withStatus(OK);

        responseActions.andRespond(response);
        stockService.unfreeze(1L);
    }
}
