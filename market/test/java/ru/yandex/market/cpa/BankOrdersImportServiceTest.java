package ru.yandex.market.cpa;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feature.subsidies.impl.BalanceServiceId;
import ru.yandex.market.mbi.util.FileUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BankOrdersImportServiceTest extends FunctionalTest {

    private static final String COMMENT_LINE_PREFIX = "#";
    private static final String BALANCE_RESPONSE_FILENAME =
            "/ru/yandex/market/cpa/BankOrdersImportServiceTest.balanceResponse.csv";
    private static final BalanceServiceId BLUE_PAYMENTS = BalanceServiceId.BLUE_PAYMENTS_ID;

    private static final Instant TRANTIME_FROM = toInstant(LocalDateTime.of(2019, 3, 10, 0, 0));
    private static final Instant TRANTIME_TO = toInstant(LocalDateTime.of(2019, 3, 16, 0, 0));
    private static final Instant NEW_TRANTIME_FROM = toInstant(LocalDateTime.of(2019, 3, 15, 5, 0));

    @Autowired
    private BankOrdersImportService bankOrdersImportService;

    @Autowired
    private RestTemplate balanceCsvClientRestTemplate;

    private MockRestServiceServer mockRestServiceServer;

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    @BeforeEach
    void setUp() {
        mockRestServiceServer = MockRestServiceServer.createServer(balanceCsvClientRestTemplate);
    }

    @Test
    @DbUnitDataSet(
            before = "BankOrdersImportServiceTest.before.csv",
            after = "BankOrdersImportServiceTest.after.csv"
    )
    void testImportBankOrders() throws IOException {
        createMockedBalanceCsvRestService(BLUE_PAYMENTS.getId(), BALANCE_RESPONSE_FILENAME);

        Instant newTrantimeFrom
                = bankOrdersImportService.importBankOrders(BLUE_PAYMENTS, TRANTIME_FROM, TRANTIME_TO);

        assertEquals(NEW_TRANTIME_FROM, newTrantimeFrom);

        mockRestServiceServer.verify();
    }

    @Test
    @DbUnitDataSet(
            before = "ImportPaymentControlTest.before.csv",
            after = "ImportPaymentControlTest.after.csv"
    )
    void importPaymentControlTest() throws IOException {
        createMockedBalanceCsvRestService(BalanceServiceId.BLUE_PAYMENT_CONTROL.getId(),
                "/ru/yandex/market/cpa/ImportPaymentControlTest.balanceResponse.csv");

        Instant newTrantimeFrom = bankOrdersImportService.importBankOrders(
                BalanceServiceId.BLUE_PAYMENT_CONTROL, TRANTIME_FROM, TRANTIME_TO
        );

        assertEquals(NEW_TRANTIME_FROM, newTrantimeFrom);

        mockRestServiceServer.verify();
    }

    @Test
    @DisplayName("Импорт п/п с дубликатами")
    @DbUnitDataSet(
            before = "BankOrdersImportServiceTest.testImportBankOrdersWithDuplicates.before.csv",
            after = "BankOrdersImportServiceTest.testImportBankOrdersWithDuplicates.after.csv"
    )
    void testImportBankOrdersWithDuplicates() throws IOException {
        createMockedBalanceCsvRestService(BLUE_PAYMENTS.getId(),
                "/ru/yandex/market/cpa/BankOrdersImportServiceTest.balanceResponseWithDuplicates.csv");

        Instant newTrantimeFrom = bankOrdersImportService.importBankOrders(BLUE_PAYMENTS, TRANTIME_FROM, TRANTIME_TO);

        assertEquals(NEW_TRANTIME_FROM, newTrantimeFrom);

        mockRestServiceServer.verify();
    }

    private void createMockedBalanceCsvRestService(int serviceId, String responseFile) throws IOException {
        final String response =
                FileUtils.readFilteredLinesFromFile(responseFile, line -> !line.startsWith(COMMENT_LINE_PREFIX));

        mockRestServiceServer.expect(anything())
                .andExpect(queryParam("service_id", String.valueOf(serviceId)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(response, MediaType.TEXT_PLAIN));
    }
}
