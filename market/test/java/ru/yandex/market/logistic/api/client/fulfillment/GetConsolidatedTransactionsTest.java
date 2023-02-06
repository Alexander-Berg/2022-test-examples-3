package ru.yandex.market.logistic.api.client.fulfillment;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.Param;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetConsolidatedTransactionsResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ConsolidatedTransaction;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransactionType;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetConsolidatedTransactionsTest extends CommonServiceClientTest {

    private static final DateTimeInterval DATE_TIME_INTERVAL =
        DateTimeInterval.fromFormattedValue("2019-08-07/2019-08-10");
    private static final Integer TRANSACTION_LIMIT = 1;
    private static final Integer TRANSACTION_INVALID_LIMIT = -1;
    private static final Integer TRANSACTION_OFFSET = 0;
    private static final Integer TRANSACTION_INVALID_OFFSET = -1;

    @Test
    void testSuccessfulResponse() throws Exception {
        prepareMockServiceNormalized("ff_get_consolidated_transactions", PARTNER_URL);

        GetConsolidatedTransactionsResponse response = fulfillmentClient.getConsolidatedTransactions(
            DATE_TIME_INTERVAL,
            TRANSACTION_LIMIT,
            TRANSACTION_OFFSET,
            getPartnerProperties()
        );

        GetConsolidatedTransactionsResponse expectedResponse =
            new GetConsolidatedTransactionsResponse(getConsolidatedTransactions());

        assertEquals(expectedResponse, response, "Ответ getConsolidatedTransactions должен быть корректный");
    }

    @Test
    void testSuccessfulResponseWithEmptyList() throws Exception {
        prepareMockServiceNormalized(
            "ff_get_consolidated_transactions",
            "ff_get_consolidated_transactions_empty_list",
            PARTNER_URL
        );

        GetConsolidatedTransactionsResponse response = fulfillmentClient.getConsolidatedTransactions(
            DATE_TIME_INTERVAL,
            TRANSACTION_LIMIT,
            TRANSACTION_OFFSET,
            getPartnerProperties()
        );

        GetConsolidatedTransactionsResponse expectedResponse =
            new GetConsolidatedTransactionsResponse(Collections.emptyList());

        assertEquals(expectedResponse, response, "Ответ getConsolidatedTransactions должен быть корректный");
    }

    @Test
    void testSuccessfulResponseWithNullList() throws Exception {
        prepareMockServiceNormalized(
            "ff_get_consolidated_transactions",
            "ff_get_consolidated_transactions_null_list",
            PARTNER_URL
        );

        GetConsolidatedTransactionsResponse response = fulfillmentClient.getConsolidatedTransactions(
            DATE_TIME_INTERVAL,
            TRANSACTION_LIMIT,
            TRANSACTION_OFFSET,
            getPartnerProperties()
        );

        GetConsolidatedTransactionsResponse expectedResponse = new GetConsolidatedTransactionsResponse(null);

        assertEquals(expectedResponse, response, "Ответ getConsolidatedTransactions должен быть корректный");
    }

    @Test
    void testInvalidParametersLimit() {
        assertThrows(
            ValidationException.class,
            () -> fulfillmentClient.getConsolidatedTransactions(
                DATE_TIME_INTERVAL,
                TRANSACTION_INVALID_OFFSET,
                TRANSACTION_OFFSET,
                getPartnerProperties()
            )
        );
    }

    @Test
    void testInvalidParametersOffset() {
        assertThrows(
            ValidationException.class,
            () -> fulfillmentClient.getConsolidatedTransactions(
                DATE_TIME_INTERVAL,
                TRANSACTION_LIMIT,
                TRANSACTION_INVALID_OFFSET,
                getPartnerProperties()
            )
        );
    }

    @Test
    void testErrorResponse() throws Exception {
        prepareMockServiceNormalized(
            "ff_get_consolidated_transactions",
            "ff_get_consolidated_transactions_error",
            PARTNER_URL
        );

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getConsolidatedTransactions(
                DATE_TIME_INTERVAL,
                TRANSACTION_LIMIT,
                TRANSACTION_OFFSET,
                getPartnerProperties()
            )
        );
    }

    private Collection<ConsolidatedTransaction> getConsolidatedTransactions() {
        return ImmutableList.of(
            new ConsolidatedTransaction.ConsolidatedTransactionBuilder(
                new DateTime("2019-08-08T12:30"),
                "6ea161f870ba6574d3bd9bdd19e1e9d8",
                TransactionType.INTAKE,
                new BigDecimal(12345.00)
            ).setParams(Collections.singletonList(
                new Param.ParamBuilder("weight").setValue("15").build()
            )).build(),
            new ConsolidatedTransaction.ConsolidatedTransactionBuilder(
                new DateTime("2019-08-08T12:30"),
                "6ea161f870ba6574d3bd9bdd19e1e9d8",
                TransactionType.SERVICE,
                new BigDecimal(12345.00)
            ).setParams(
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
            ).build()
        );
    }
}
