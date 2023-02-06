package ru.yandex.market.logistic.gateway.service.executor.delivery.sync;

import java.math.BigDecimal;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.model.delivery.Param;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.GetTransactionsOrdersResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.AdditionalServiceCode;
import ru.yandex.market.logistic.api.model.delivery.response.entities.Transaction;
import ru.yandex.market.logistic.api.model.delivery.response.entities.TransactionType;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetTransactionsOrdersRequest;
import ru.yandex.market.logistic.gateway.exceptions.ServiceInteractionRequestFormatException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест для {@link GetTransactionsOrdersRequestExecutor}
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetTransactionsOrdersRequestExecutorTest extends AbstractIntegrationTest {

    @MockBean
    private DeliveryServiceClient deliveryServiceClient;

    @Autowired
    private GetTransactionsOrdersRequestExecutor executor;

    @Test
    public void executeWithOrderId() throws Exception {

        when(deliveryServiceClient.getTransactionsOrders(
            any(ResourceId.class),
            isNull(DateTimeInterval.class),
            any(Integer.class),
            any(Integer.class),
            any(PartnerProperties.class)))
            .thenReturn(getApiResponse());

        mockMvc.perform(
            post("/delivery/getTransactionsOrders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                    "fixtures/request/delivery/get_transactions_orders/delivery_get_transactions_orders_orderid.json"
                )))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent(
                "fixtures/response/delivery/get_transactions_orders/delivery_get_transactions_orders.json"
            )));

        verify(deliveryServiceClient).getTransactionsOrders(
            refEq(new ResourceId.ResourceIdBuilder().setYandexId("123").build()),
            isNull(DateTimeInterval.class),
            eq(1),
            eq(0),
            any(PartnerProperties.class)
        );
    }

    @Test
    public void executeWithInterval() throws Exception {

        when(deliveryServiceClient.getTransactionsOrders(
            isNull(ResourceId.class),
            any(DateTimeInterval.class),
            any(Integer.class),
            any(Integer.class),
            any(PartnerProperties.class)))
            .thenReturn(getApiResponse());

        mockMvc.perform(
            post("/delivery/getTransactionsOrders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                    "fixtures/request/delivery/get_transactions_orders/delivery_get_transactions_orders_interval.json"
                )))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent(
                "fixtures/response/delivery/get_transactions_orders/delivery_get_transactions_orders.json"
            )));

        verify(deliveryServiceClient).getTransactionsOrders(
            isNull(ResourceId.class),
            refEq(DateTimeInterval.fromFormattedValue("2019-08-02T00:00:00+03:00/2019-08-10T00:00:00+03:00")),
            eq(1),
            eq(0),
            any(PartnerProperties.class)
        );
    }

    @Test(expected = ServiceInteractionRequestFormatException.class)
    public void executeWithOrderIdAndInterval() {

        executor.execute(
            new GetTransactionsOrdersRequest(
                new ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId.ResourceIdBuilder().setYandexId("123").build(),
                ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval.fromFormattedValue(
                    "2019-08-02T00:00:00+03:00/2019-08-10T00:00:00+03:00"
                ),
                1,
                0,
                new Partner(145L)
            ));

        verify(deliveryServiceClient, never()).getTransactionsOrders(
            refEq(new ResourceId.ResourceIdBuilder().setYandexId("123").build()),
            refEq(DateTimeInterval.fromFormattedValue("2019-08-02T00:00:00+03:00/2019-08-10T00:00:00+03:00")),
            eq(1),
            eq(0),
            any(PartnerProperties.class)
        );
    }

    private GetTransactionsOrdersResponse getApiResponse() {
        return new GetTransactionsOrdersResponse(
            Collections.singletonList(
                new Transaction.TransactionBuilder(new ResourceId.ResourceIdBuilder().setYandexId("12345").build(),
                    new DateTime("2019-08-05T12:30"),
                    "6ea161f870ba6574d3bd9bdd19e1e9d8",
                    TransactionType.SERVICE,
                    Collections.singletonList(new Param.ParamBuilder("weight").setValue("15").build()),
                    new BigDecimal(12345.00))
                    .setNativeName("get-transactions-test")
                    .setUniformName(AdditionalServiceCode.INSURANCE)
                    .build())
        );
    }
}
