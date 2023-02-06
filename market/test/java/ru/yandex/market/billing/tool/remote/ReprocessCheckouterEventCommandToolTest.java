package ru.yandex.market.billing.tool.remote;

import java.io.IOException;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.checkout.GetOrderEventsService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.MockClientHttpRequestFactory;
import ru.yandex.market.core.util.tool.ToolRequest;

/**
 * Тесты для {@link ReprocessCheckouterEventCommandTool}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class ReprocessCheckouterEventCommandToolTest extends FunctionalTest {

    @Autowired
    private GetOrderEventsService getOrderEventsService;

    @Autowired
    private RestTemplate checkouterRestTemplate;

    @Mock
    private ToolRequest request;

    /**
     * Проверяем, что механика ignore в env не аффектит команды.
     * Для проверки взято наиболее простое с точки зрения подготовки данных событие, и проверяется что оно не было
     * отфильтровано и данные доехали до базы.
     * Сама логика его обрабтки в данном лучае не имеет никакого значения.
     */
    @Test
    @DbUnitDataSet(
            before = "db/EventCommandTool.ignoreTest.before.csv",
            after = "db/EventCommandTool.ignoreTest.after.csv"
    )
    void test_eventReprocessNotAffectedByIgnoreList() throws IOException {
        checkouterRestTemplate.setRequestFactory(new MockClientHttpRequestFactory(
                new ClassPathResource("ru/yandex/market/billing/tool/remote/event-command.json")
        ));
        ReprocessCheckouterEventCommandTool tool = new ReprocessCheckouterEventCommandTool(getOrderEventsService);

        Mockito.when(request.getParam("event")).thenReturn("1");

        tool.doToolAction(request);
    }
}
