package ru.yandex.market.cpa;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.FileUtils;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BankOrderItemsImportServiceTest extends FunctionalTest {

    private static final String COMMENT_LINE_PREFIX = "#";

    private static final String BALANCE_RESPONSE_FILENAME =
            "/ru/yandex/market/cpa/BankOrderItemsImportServiceTest.paymentBatchId%s.balanceResponse.csv";

    private static final String[] PAYMENT_BATCH_IDS = {"108", "107", "106", "104", "105"};

    @Autowired
    private BankOrderItemsImportService bankOrderItemsImportService;

    @Autowired
    private RestTemplate balanceCsvClientRestTemplate;

    private MockRestServiceServer mockRestServiceServer;

    private static String getResponseFilename(String paymentBatchId) {
        return String.format(BALANCE_RESPONSE_FILENAME, paymentBatchId);
    }

    @BeforeEach
    void setUp() {
        mockRestServiceServer = MockRestServiceServer.createServer(balanceCsvClientRestTemplate);
    }

    @Test
    @DbUnitDataSet(
            before = "BankOrderItemsImportServiceTest.before.csv",
            after = "BankOrderItemsImportServiceTest.after.csv"
    )
    void testImportBankOrderItems() throws IOException {
        createMockedBalanceCsvRestService(PAYMENT_BATCH_IDS);

        bankOrderItemsImportService.importBankOrderItems();

        mockRestServiceServer.verify();
    }
    @DisplayName("Принятие от баланса нового вида SERVICE_ORDER_ID payment-order-%d ")
    @Test
    @DbUnitDataSet(
            before = "BankOrderItemsImportServiceTest.paymentId.before.csv",
            after = "BankOrderItemsImportServiceTest.paymentId.after.csv"
    )
    void testImportBankOrderItemsParsePaymentId() throws IOException {
        createMockedBalanceCsvRestService(PAYMENT_BATCH_IDS);

        bankOrderItemsImportService.importBankOrderItems();

        mockRestServiceServer.verify();
    }

    private void createMockedBalanceCsvRestService(String[] paymentBatchIds) throws IOException {
        for (String paymentBatchId : paymentBatchIds) {
            String responseFilename = getResponseFilename(paymentBatchId);
            String response =
                    FileUtils.readFilteredLinesFromFile(responseFilename, line -> !line.startsWith(COMMENT_LINE_PREFIX));

            mockRestServiceServer.expect(anything())
                    .andExpect(queryParam("payment_batch_id", String.valueOf(paymentBatchId)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(response, MediaType.TEXT_PLAIN));
        }
    }
}
