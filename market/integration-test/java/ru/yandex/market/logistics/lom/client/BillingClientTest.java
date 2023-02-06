package ru.yandex.market.logistics.lom.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.model.dto.BillingTransactionDto;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class BillingClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Поиск транзакций")
    void getTransactions() {
        mockFind(extractFileContent("response/billing/transactions.json"));

        List<BillingTransactionDto> transactions = lomClient.findTransactions(
            LocalDateTime.of(2019, 10, 10, 0, 0, 0),
            LocalDateTime.of(2019, 10, 11, 0, 0, 0)
        );

        softly.assertThat(transactions)
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(List.of(
                createTransaction(
                    1L,
                    LocalDateTime.of(2019, 10, 9, 4, 15, 0),
                    new BigDecimal("100.13"),
                    (long) 1,
                    false
                ),
                createTransaction(
                    2L,
                    LocalDateTime.of(2019, 10, 9, 5, 0, 0),
                    new BigDecimal("200.24"),
                    (long) 2,
                    true
                )
            ));
    }

    @Test
    @DisplayName("Пустой ответ")
    void getTransactionsEmpty() {
        mockFind("[]");

        List<BillingTransactionDto> transactions = lomClient.findTransactions(
            LocalDateTime.of(2019, 10, 10, 0, 0, 0),
            LocalDateTime.of(2019, 10, 11, 0, 0, 0)
        );

        softly.assertThat(transactions.size()).isEqualTo(0);
    }

    @Nonnull
    private BillingTransactionDto createTransaction(
        long id,
        LocalDateTime time,
        BigDecimal amount,
        long productId,
        boolean isCorrection
    ) {
        return BillingTransactionDto.builder()
            .id(id)
            .entityId(100)
            .contractId(142L)
            .time(time)
            .amount(amount)
            .productId(productId)
            .correction(isCorrection)
            .build();
    }

    private void mockFind(String response) {
        mock.expect(method(HttpMethod.GET))
            .andExpect(queryParam("to_date", "2019-10-11T00:00:00"))
            .andExpect(queryParam("from_date", "2019-10-10T00:00:00"))
            .andExpect(requestTo(startsWith(uri + "/billing/transactions")))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }
}
