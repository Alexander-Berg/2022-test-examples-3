package ru.yandex.market.logistic.api.client.fulfillment;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.Param;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetTransactionsOrdersResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.Transaction;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransactionType;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetTransactionsOrdersTest extends CommonServiceClientTest {

    private static final ResourceId ORDER_ID = new ResourceId.ResourceIdBuilder().setYandexId("123").build();
    private static final DateTimeInterval DATE_TIME_INTERVAL =
        DateTimeInterval.fromFormattedValue("2019-08-07/2019-08-10");
    private static final DateTimeInterval INVALID_DATE_TIME_INTERVAL =
        DateTimeInterval.fromFormattedValue("2019-08-01/2019-08-10");
    private static final Integer TRANSACTION_LIMIT = 1;

    private static final Integer TRANSACTION_OFFSET = 0;

    @Test
    void testSuccessfulResponse() throws Exception {
        prepareMockServiceNormalized("ff_get_transactions_orders", PARTNER_URL);

        GetTransactionsOrdersResponse response = fulfillmentClient.getTransactionsOrders(
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
    void testInvalidParametersInterval() {
        assertThrows(
            ValidationException.class,
            () -> fulfillmentClient.getTransactionsOrders(
                ORDER_ID,
                INVALID_DATE_TIME_INTERVAL,
                TRANSACTION_LIMIT,
                TRANSACTION_OFFSET,
                getPartnerProperties()
            )
        );
    }

    @Test
    void testInvalidParametersLimit() throws Exception {

        List<Pair> invalidLimitParameters = Arrays.asList(
            Pair.of(-1, "Значение limit не может быть отрицательным"),
            Pair.of(1001, "Значение limit не может быть больше 1000")
        );

        int caught = 0;
        for (Pair tuple : invalidLimitParameters) {
            try {
                fulfillmentClient.getTransactionsOrders(
                    ORDER_ID,
                    DATE_TIME_INTERVAL,
                    (Integer) tuple.getFirst(),
                    TRANSACTION_OFFSET,
                    getPartnerProperties()
                );
            } catch (ValidationException e) {
                caught++;
                assertTrue(e.getMessage().contains(tuple.getSecond().toString()));
            }
        }
        assertEquals(invalidLimitParameters.size(), caught);
    }

    @Test
    void testInvalidParametersOffset() {

        List<Pair> invalidOffsetParameters = Collections.singletonList(
            Pair.of(-1, "Значение offset не может быть отрицательным")
        );

        int caught = 0;
        for (Pair tuple : invalidOffsetParameters) {
            try {
                fulfillmentClient.getTransactionsOrders(
                    ORDER_ID,
                    DATE_TIME_INTERVAL,
                    TRANSACTION_LIMIT,
                    (Integer) tuple.getFirst(),
                    getPartnerProperties()
                );
            } catch (ValidationException e) {
                caught++;
                assertTrue(e.getMessage().contains(tuple.getSecond().toString()));
            }
        }
        assertEquals(invalidOffsetParameters.size(), caught);
    }


    @Test
    void testErrorResponse() throws Exception {
        prepareMockServiceNormalized(
            "ff_get_transactions_orders",
            "ff_get_transactions_orders_error",
            PARTNER_URL
        );

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getTransactionsOrders(
                ORDER_ID,
                DATE_TIME_INTERVAL,
                TRANSACTION_LIMIT,
                TRANSACTION_OFFSET,
                getPartnerProperties()
            )
        );
    }

    private List<Transaction> getTransactions() {

        return Arrays.asList(
            new Transaction.TransactionBuilder(new ResourceId.ResourceIdBuilder().setYandexId("12345").build(),
                new DateTime("2019-08-08T12:30"),
                "6ea161f870ba6574d3bd9bdd19e1e9d8",
                TransactionType.INTAKE,
                new BigDecimal(12345.00))
                .setParams(Collections.singletonList(
                    new Param.ParamBuilder("weight").setValue("15").build()
                ))
                .build(),
            new Transaction.TransactionBuilder(new ResourceId.ResourceIdBuilder().setYandexId("12345").build(),
                new DateTime("2019-08-08T12:30"),
                "6ea161f870ba6574d3bd9bdd19e1e9d8",
                TransactionType.SERVICE,
                new BigDecimal(12345.00))
                .setParams(
                    Arrays.asList(
                        new Param.ParamBuilder("uniformName").setValue("SORT").build(),
                        new Param.ParamBuilder("serviceDateTime").setValue("2019-08-05T11:59:00").build(),
                        new Param.ParamBuilder("serviceCategory").setValue("INBOUND").build(),
                        new Param.ParamBuilder("nativeName").setValue("inbound_local").build(),
                        new Param.ParamBuilder("weight").setValue("10").build(),
                        new Param.ParamBuilder("length").setValue("100").build(),
                        new Param.ParamBuilder("width").setValue("80").build(),
                        new Param.ParamBuilder("height").setValue("20").build()
                    )
                )
                .build()
        );

    }

}
