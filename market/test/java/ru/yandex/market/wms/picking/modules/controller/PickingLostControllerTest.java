package ru.yandex.market.wms.picking.modules.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.core.base.request.MoveToLostRequest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PickingLostControllerTest extends IntegrationTest {

    @MockBean
    @Autowired
    protected JmsTemplate defaultJmsTemplate;

    @BeforeEach
    void reset() {
        Mockito.reset(defaultJmsTemplate);
    }

    @Test
    @DatabaseSetup("/controller/lost/happy/before.xml")
    @ExpectedDatabase(value = "/controller/lost/happy/after.xml", assertionMode = NON_STRICT, connection =
            "wmwhseConnection")
    public void lostHappyPath() throws Exception {
        mockMvc.perform(post("/lost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/lost/happy/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/lost/happy/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/lost/last_item/before.xml")
    @ExpectedDatabase(value = "/controller/lost/last_item/after.xml", assertionMode = NON_STRICT, connection =
            "wmwhseConnection")
    public void lostLastItem() throws Exception {
        mockMvc.perform(post("/lost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/lost/last_item/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/lost/last_item/response.json")))
                .andReturn();
    }

    @Test
    public void lostWrongRequest() throws Exception {
        mockMvc.perform(post("/lost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/lost/last_item/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/lost/last_item/response.json")))
                .andReturn();
    }

    /**
     * Завершаем только задания отборщика, балансы не трогаем, pickdetail не удаляем.
     * Отправляем в очередь move-to-lost запрос на асинхронную обработку инцидента.
     */
    @Test
    @DatabaseSetup("/controller/lost/happy/before-distributed-flow.xml")
    @ExpectedDatabase(value = "/controller/lost/happy/after-distributed-flow.xml",
            assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void lostHappyPathJustTerminateCurrentTask() throws Exception {
       mockMvc.perform(post("/lost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/lost/happy/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/lost/happy/response.json")));

        verify(defaultJmsTemplate, times(1)).convertAndSend(endsWith("move-to-lost"),
                any(MoveToLostRequest.class), any());
    }

    /**
     * Завершаем только задания отборщика, балансы не трогаем, pickdetail не удаляем.
     * Отправляем в очередь move-to-lost запрос на асинхронную обработку инцидента.
     * Флаг выключен, переключение режима через GET-параметр
     */
    @Test
    @DatabaseSetup("/controller/lost/happy/before-distributed-flow-noflag.xml")
    @ExpectedDatabase(value = "/controller/lost/happy/after-distributed-flow.xml",
            assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void lostHappyPathJustTerminateCurrentTaskViaGetParam() throws Exception {
        mockMvc.perform(post("/lost?short=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/lost/happy/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/lost/happy/response.json")));

        verify(defaultJmsTemplate, times(1)).convertAndSend(endsWith("move-to-lost"),
                any(MoveToLostRequest.class), any());
    }

    /**
     * Когда шортирование по заказу, которого уже нет в системе
     * Например, батч по какой-то причине успел расформироваться, а отборщик отбирает неактуальное,
     * но увидел, что ничего нет на полке
     */
    @Test
    @DatabaseSetup("/controller/lost/happy/before-distributed-flow-order-not-found.xml")
    @ExpectedDatabase(value = "/controller/lost/happy/after-distributed-flow-order-not-found.xml",
            assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void lostJustTerminateCurrentTaskWithoutOrder() throws Exception {
        mockMvc.perform(post("/lost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/lost/happy/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/lost/happy/response.json")));

        verify(defaultJmsTemplate, times(1)).convertAndSend(endsWith("move-to-lost"),
                any(MoveToLostRequest.class), any());
    }
}
