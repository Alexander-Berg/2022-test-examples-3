package ru.yandex.market.logistic.api.client.delivery;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.delivery.Param;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.GetTransactionsOrdersResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.AdditionalServiceCode;
import ru.yandex.market.logistic.api.model.delivery.response.entities.Transaction;
import ru.yandex.market.logistic.api.model.delivery.response.entities.TransactionType;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetTransactionsOrdersTest extends CommonServiceClientTest {

    private static final ResourceId ORDER_ID = new ResourceId.ResourceIdBuilder().setYandexId("123").build();
    private static final DateTimeInterval DATE_TIME_INTERVAL =
        DateTimeInterval.fromFormattedValue("2019-08-07/2019-08-10");
    private static final Integer TRANSACTION_LIMIT = 1;
    private static final Integer TRANSACTION_OFFSET = 0;

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void testSuccessfulResponse() throws Exception {
        prepareMockServiceNormalized("ds_get_transactions_orders", PARTNER_URL);

        GetTransactionsOrdersResponse response = deliveryServiceClient.getTransactionsOrders(
            ORDER_ID,
            DATE_TIME_INTERVAL,
            TRANSACTION_LIMIT,
            TRANSACTION_OFFSET,
            getPartnerProperties()
        );

        GetTransactionsOrdersResponse expectedResponse = new GetTransactionsOrdersResponse(getTransactions());

        assertEquals(expectedResponse, response, "Ответ getTransactionsOrders должен быть корректный");
    }

    @Test
    void testErrorResponse() throws Exception {
        prepareMockServiceNormalized(
            "ds_get_transactions_orders",
            "ds_get_transactions_orders_error",
            PARTNER_URL
        );

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.getTransactionsOrders(
                ORDER_ID,
                DATE_TIME_INTERVAL,
                TRANSACTION_LIMIT,
                TRANSACTION_OFFSET,
                getPartnerProperties()
            )
        );
    }

    private List<Transaction> getTransactions() {
        return Collections.singletonList(
            new Transaction.TransactionBuilder(
                new ResourceId.ResourceIdBuilder().setYandexId("12345").build(),
                new DateTime("2019-08-05T12:30"),
                "6ea161f870ba6574d3bd9bdd19e1e9d8",
                TransactionType.SERVICE,
                Collections.singletonList(new Param.ParamBuilder("weight").setValue("15").build()),
                new BigDecimal(12345.00))
                .setNativeName("get-transactions-test")
                .setUniformName(AdditionalServiceCode.INSURANCE)
                .build()
        );
    }
}
