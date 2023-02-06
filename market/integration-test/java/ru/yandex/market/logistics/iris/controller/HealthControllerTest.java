package ru.yandex.market.logistics.iris.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.service.health.HealthUtil;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class HealthControllerTest extends AbstractContextualTest {

    @Autowired
    private TvmClient tvmClient;

    @Autowired
    private TvmTicketChecker tvmTicketChecker;

    @BeforeEach
    public void init() {
        when(tvmTicketChecker.isLogOnlyMode()).thenReturn(false);
    }

    @Test
    public void pingWhenTvmClientStatusIsOk() throws Exception {
        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.OK, ""));
        httpOperationWithResult(
                get("/ping"),
                status().isOk(),
                content().string(HealthUtil.OK_ANSWER)
        );
    }

    @Test
    public void pingWhenTvmClientStatusIsExpiringCache() throws Exception {
        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.WARNING, ""));
        httpOperationWithResult(
                get("/ping"),
                status().isOk(),
                content().string(HealthUtil.OK_ANSWER)
        );
    }

    @Test
    public void pingWhenTvmClientStatusIsInvalidCache() throws Exception {
        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.ERROR, "InvalidCache"));
        httpOperationWithResult(
                get("/ping"),
                status().isOk(),
                content().string(HealthUtil.errorToAnswer("Tvm client has invalid status: InvalidCache"))
        );
    }


    @Test
    public void pingWhenTvmClientStatusIsIncompleteListOfServiceTickets() throws Exception {
        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.ERROR, "IncompleteListOfServiceTickets"));
        httpOperationWithResult(
                get("/ping"),
                status().isOk(),
                content().string(HealthUtil
                        .errorToAnswer("Tvm client has invalid status: IncompleteListOfServiceTickets"))
        );
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/logbroker_checker/empty.xml")
    @ExpectedDatabase(value = "classpath:fixtures/setup/logbroker_checker/empty.xml", assertionMode = NON_STRICT)
    public void mdmToIrisRecordsEmpty() throws Exception {
        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.OK, ""));
        httpOperationWithResult(
                get("/health/mdm-to-iris-records"),
                status().isOk(),
                content().string(HealthUtil.OK_ANSWER)
        );
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/logbroker_checker/empty.xml")
    @ExpectedDatabase(value = "classpath:fixtures/setup/logbroker_checker/empty.xml", assertionMode = NON_STRICT)
    public void marketDatacampOffersEmpty() throws Exception {
        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.OK, ""));
        httpOperationWithResult(
                get("/health/market-datacamp-offers"),
                status().isOk(),
                content().string(HealthUtil.OK_ANSWER)
        );
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/logbroker_checker/future.xml")
    @ExpectedDatabase(value = "classpath:fixtures/setup/logbroker_checker/future.xml", assertionMode = NON_STRICT)
    public void mdmToIrisRecordsFuture() throws Exception {
        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.OK, ""));
        httpOperationWithResult(
                get("/health/mdm-to-iris-records"),
                status().isOk(),
                content().string(HealthUtil.OK_ANSWER)
        );
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/logbroker_checker/future.xml")
    @ExpectedDatabase(value = "classpath:fixtures/setup/logbroker_checker/future.xml", assertionMode = NON_STRICT)
    public void marketDatacampOffersFuture() throws Exception {
        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.OK, ""));
        httpOperationWithResult(
                get("/health/market-datacamp-offers"),
                status().isOk(),
                content().string(HealthUtil.OK_ANSWER)
        );
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/logbroker_checker/past.xml")
    @ExpectedDatabase(value = "classpath:fixtures/setup/logbroker_checker/past.xml", assertionMode = NON_STRICT)
    public void mdmToIrisRecordsPast() throws Exception {
        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.OK, ""));
        httpOperationWithResult(
                get("/health/mdm-to-iris-records"),
                status().isOk(),
                content().string(HealthUtil.errorToAnswer("Last successful reading from Logbroker topic" +
                        " rt3.kafka-bs--market-mdm@test--mdm-to-iris-records was at 2020-01-01T00:00," +
                        " more than 60 minutes ago"))
        );
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/logbroker_checker/past.xml")
    @ExpectedDatabase(value = "classpath:fixtures/setup/logbroker_checker/past.xml", assertionMode = NON_STRICT)
    public void marketDatacampOffersPast() throws Exception {
        when(tvmClient.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.OK, ""));
        httpOperationWithResult(
                get("/health/market-datacamp-offers"),
                status().isOk(),
                content().string(HealthUtil.errorToAnswer("Last successful reading from Logbroker topic" +
                        " rt3.vla--market-ff@testing--market-datacamp-offers was at 2020-01-01T00:00," +
                        " more than 60 minutes ago"))
        );
    }
}
