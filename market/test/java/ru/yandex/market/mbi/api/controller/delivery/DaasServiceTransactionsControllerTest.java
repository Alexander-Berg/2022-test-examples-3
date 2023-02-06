package ru.yandex.market.mbi.api.controller.delivery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.api.config.FunctionalTest;

class DaasServiceTransactionsControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Получение транзакций за указанный диапазон дат")
    @DbUnitDataSet(before = "csv/DaasServiceTransactionsController.get.before.csv")
    void getDaasServiceTransactions() {
        String expected = StringTestUtil.getString(
                DaasServiceTransactionsControllerTest.class,
                "response/daas_transactions_response.json"
        );

        try {
            ResponseEntity<String> response = FunctionalTestHelper.get(
                    "http://localhost:"
                            + port +
                            "/daas-service-transactions?from_date=2018-05-07 00:00:00&till_date=2018-05-08 23:59:59"
            );
            System.out.println(response.getBody());
            JsonTestUtil.assertEquals(response.getBody(), expected);
        } catch (HttpClientErrorException e) {
            e.printStackTrace();
            System.out.println(e.getResponseBodyAsString());
        }

    }
}
