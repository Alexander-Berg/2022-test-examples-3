package ru.yandex.market.billing.tool.remote;

import java.io.IOException;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.checkout.EventProcessorSupportFactory;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.MockClientHttpRequestFactory;
import ru.yandex.market.core.util.tool.ToolRequest;

/**
 * Тесты для {@link ReprocessCheckouterEventCommandTool}.
 *
 * @author adjanybekov
 */
@ExtendWith(MockitoExtension.class)
class ReprocessCheckouterOrderCommandToolTest extends FunctionalTest {

    @Autowired
    private RestTemplate checkouterRestTemplate;

    @Mock
    private ToolRequest request;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private RetryTemplate retryTemplate;

    @Autowired
    private EventProcessorSupportFactory supportFactory;
    /**
     * Проверка механики
     */
    @Test
    @DbUnitDataSet(
            before = "db/OrderCommandTool.ignoreTest.before.csv",
            after = "db/OrderCommandTool.ignoreTest.after.csv"
    )
    void test_eventReprocessNotAffectedByIgnoreList() throws IOException {
        checkouterRestTemplate.setRequestFactory(new MockClientHttpRequestFactory(
                new ClassPathResource("ru/yandex/market/billing/tool/remote/order-command.json")
        ));
        ReprocessCheckouterOrderCommandTool tool = new ReprocessCheckouterOrderCommandTool(supportFactory,checkouterAPI,retryTemplate);

        Mockito.when(request.getParam("order")).thenReturn("1");

        tool.doToolAction(request);
    }

    @Test
    @DbUnitDataSet(
            before = "db/OrderCommandTool.saveReceipt.before.csv",
            after = "db/OrderCommandTool.saveReceipt.after.csv"
    )
    void test_saveReceiptForArchivedOrder() throws IOException {
        checkouterRestTemplate.setRequestFactory(new MockClientHttpRequestFactory(
                new ClassPathResource("ru/yandex/market/billing/tool/remote/archived-order-command.json")
        ));
        ReprocessCheckouterOrderCommandTool tool = new ReprocessCheckouterOrderCommandTool(supportFactory,checkouterAPI,retryTemplate);

        Mockito.when(request.getParam("order")).thenReturn("1");

        tool.doToolAction(request);
    }
}
