package ru.yandex.market.logistic.gateway.service.executor.fulfillment.sync;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.model.fulfillment.Param;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetTransactionsOrdersResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.Transaction;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransactionType;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;

import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест для {@link GetTransactionsOrdersRequestExecutor}
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetTransactionsOrdersRequestExecutorTest extends AbstractIntegrationTest {

    @SpyBean
    private FulfillmentClient fulfillmentClient;

    @Test
    public void executeWithOrderId() throws Exception {
        doReturn(getApiResponse()).when(fulfillmentClient).getTransactionsOrders(
            any(ResourceId.class),
            isNull(),
            any(Integer.class),
            any(Integer.class),
            any(PartnerProperties.class)
        );

        mockMvc.perform(
            post("/fulfillment/getTransactionsOrders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                    "fixtures/request/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders_orderid.json"
                )))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent(
                "fixtures/response/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders.json"
            )));

        verify(fulfillmentClient).getTransactionsOrders(
            refEq(new ResourceId("123", null)),
            isNull(),
            eq(10),
            eq(0),
            any(PartnerProperties.class)
        );
    }

    @Test
    public void executeWithInterval() throws Exception {
        doReturn(getApiResponse()).when(fulfillmentClient).getTransactionsOrders(
            isNull(),
            any(DateTimeInterval.class),
            any(Integer.class),
            any(Integer.class),
            any(PartnerProperties.class)
        );

        mockMvc.perform(
            post("/fulfillment/getTransactionsOrders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                    "fixtures/request/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders_interval.json"
                )))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent(
                "fixtures/response/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders.json"
            )));

        verify(fulfillmentClient).getTransactionsOrders(
            isNull(),
            refEq(DateTimeInterval.fromFormattedValue("2019-08-07T00:00:00+03:00/2019-08-10T00:00:00+03:00")),
            eq(10),
            eq(0),
            any(PartnerProperties.class)
        );
    }

    @Test
    public void executeWithBothOrderIdAndInterval() throws Exception {
        executeWithValidationError(
            "fixtures/request/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders_both.json",
            "Exactly one of orderId or interval must be set in request"
        );
    }

    @Test
    public void executeWithNoneOrderIdAndInterval() throws Exception {
        executeWithValidationError(
            "fixtures/request/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders_none.json",
            "Exactly one of orderId or interval must be set in request"
        );
    }

    @Test
    public void executeWithInvalidInterval() throws Exception {
        executeWithValidationError(
            "fixtures/request/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders_invalid_interval.json",
            "Interval length must not be greater than 7 days"
        );
    }

    private void executeWithValidationError(String requestPath, String message) throws Exception {
        mockMvc.perform(
            post("/fulfillment/getTransactionsOrders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(requestPath)))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString(message)));
        verifyNoInteractions(fulfillmentClient);
    }


    private GetTransactionsOrdersResponse getApiResponse() {
        return new GetTransactionsOrdersResponse(
            Arrays.asList(
                new Transaction(
                    new ResourceId("1234", null),
                    new DateTime("2019-08-08T12:30"),
                    "6ea161f870ba6574d3bd9bdd19e1e9d8",
                    TransactionType.INTAKE,
                    Collections.singletonList(new Param("weight", "15", null)),
                    new BigDecimal("12345.00")
                ),
                new Transaction(
                    new ResourceId("12345", null),
                    new DateTime("2019-08-08T12:30"),
                    "6ea161f870ba6574d3bd9bdd19e1e9d8",
                    TransactionType.SERVICE,
                    Arrays.asList(
                        new Param("uniformName", "SORT", null),
                        new Param("serviceDateTime", "2019-08-05T11:59:00", null),
                        new Param("serviceCategory", "INBOUND", null),
                        new Param("nativeName", "inbound_local", null),
                        new Param("weight", "10", null),
                        new Param("length", "100", null),
                        new Param("width", "80", null),
                        new Param("height", "20", null)
                    ),
                    new BigDecimal(12345.00)
                )
            ));
    }
}
