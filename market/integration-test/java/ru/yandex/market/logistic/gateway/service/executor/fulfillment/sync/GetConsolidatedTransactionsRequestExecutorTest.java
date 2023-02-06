package ru.yandex.market.logistic.gateway.service.executor.fulfillment.sync;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.model.fulfillment.Param;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetConsolidatedTransactionsResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ConsolidatedTransaction;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransactionType;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест для {@link GetTransactionsOrdersRequestExecutor}
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetConsolidatedTransactionsRequestExecutorTest extends AbstractIntegrationTest {

    @SpyBean
    private FulfillmentClient fulfillmentClient;

    @Test
    public void execute() throws Exception {
        doReturn(getApiResponse()).when(fulfillmentClient).getConsolidatedTransactions(
            any(DateTimeInterval.class),
            any(Integer.class),
            any(Integer.class),
            any(PartnerProperties.class)
        );

        mockMvc.perform(
            post("/fulfillment/getConsolidatedTransactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                    "fixtures/request/fulfillment/get_consolidated_transactions/" +
                        "fulfillment_get_consolidated_transactions.json"
                )))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent(
                "fixtures/response/fulfillment/get_consolidated_transactions/" +
                    "fulfillment_get_consolidated_transactions.json"
            )));

        verify(fulfillmentClient).getConsolidatedTransactions(
            refEq(DateTimeInterval.fromFormattedValue("2019-08-07T00:00:00+03:00/2019-08-10T00:00:00+03:00")),
            eq(10),
            eq(0),
            any(PartnerProperties.class)
        );
    }

    private GetConsolidatedTransactionsResponse getApiResponse() {
        return new GetConsolidatedTransactionsResponse(
            List.of(
                new ConsolidatedTransaction(
                    new DateTime("2019-08-08T12:30"),
                    "6ea161f870ba6574d3bd9bdd19e1e9d8",
                    TransactionType.INTAKE,
                    Collections.singletonList(new Param("weight", "15", null)),
                    new BigDecimal(12345.00)
                ),
                new ConsolidatedTransaction(
                    new DateTime("2019-08-08T12:30"),
                    "6ea161f870ba6574d3bd9bdd19e1e9d8",
                    TransactionType.SERVICE,
                    List.of(
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
                ),
                new ConsolidatedTransaction(
                    new DateTime("2019-08-08T12:30"),
                    "6ea161f870ba6574d3bd9bdd19e1e9d8",
                    TransactionType.CONSOLIDATED,
                    Collections.singletonList(new Param("weight", "15", null)),
                    new BigDecimal(12345.00)
                )
            ));
    }
}
