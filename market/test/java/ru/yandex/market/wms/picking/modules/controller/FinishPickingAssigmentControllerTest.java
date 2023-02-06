package ru.yandex.market.wms.picking.modules.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.exception.NonConveyableException;
import ru.yandex.market.wms.consolidation.client.ConsolidationClient;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;
import static ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants.DEFINE_SORT_STATION_FOR_WAVE;

public class FinishPickingAssigmentControllerTest extends IntegrationTest {

    @Autowired
    @SpyBean
    private ConsolidationClient consolidationClient;

    @Autowired
    @MockBean
    private JmsTemplate jmsTemplate;

    @BeforeEach
    private void reset() {
        Mockito.reset(consolidationClient);
    }
    /**
     * Отбор завершён. Другая модель смены статуса, без лишней записи про 52й статус
     */
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/1/before_finish_newflow.xml")
    @ExpectedDatabase(value = "/controller/finishPicking/1/after_finish_newflow.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void finishAssignmentHappyPathNewFlow() throws Exception {
        mockMvc.perform(post("/finish-picking-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("finishPicking/1/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    /**
     * Отбор завершен частично. Другая модель смены статуса, обновится статус у orderdetail
     */
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/2/before_finish_newflow.xml")
    @ExpectedDatabase(value = "/controller/finishPicking/2/after_finish_newflow.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void finishAssignmentHappyPathSwcondNewFlow() throws Exception {
        mockMvc.perform(post("/finish-picking-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("finishPicking/2/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    /**
     * заказ уже отобран частично, продолжили отбирать
     */
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/5/before_continuepickingandfinish.xml")
    @ExpectedDatabase(value = "/controller/finishPicking/5/after_continuepickingandfinish.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void finishAssignmentAllPickedAfterPartialPicking() throws Exception {
        mockMvc.perform(post("/finish-picking-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/finishPicking/5/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }


    /**
     * Назначение не найдено, ничего не сделали, ошибки нет
     */
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/3/before_finish.xml")
    @ExpectedDatabase(value = "/controller/finishPicking/3/before_finish.xml", assertionMode = NON_STRICT_UNORDERED)
    public void finishAssignmentNotFound() throws Exception {
        mockMvc.perform(post("/finish-picking-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("finishPicking/3/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    /**
     * финальная локация не найдена
     */
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/4/before_finish.xml")
    public void finishAssignmentFinalLocNotFound() throws Exception {
        mockMvc.perform(post("/finish-picking-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("finishPicking/4/request.json")))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    /**
     * отбор завершен в нонсортовой ячейке консолидации, возвращается callConsolidation=true
     */
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/7/before.xml")
    @ExpectedDatabase(value = "/controller/finishPicking/7/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void finishAssignmentInNonSortConsLoc() throws Exception {
        Mockito.doNothing()
                .when(consolidationClient).moveContainerToLine(anyString(), anyString());
        mockMvc.perform(post("/finish-picking-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/finishPicking/7/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/finishPicking/7/response.json"), true))
                .andReturn();
        Mockito.verify(consolidationClient, Mockito.times(1)).moveContainerToLine(anyString(), anyString());
    }

    /**
     * отбор завершен, тот отвязан от пользователя. Другая модель смены статуса, без лишней записи про 52й статус
     */
    @Test
    @DatabaseSetup(value = "/finishPicking/5/before_newflow.xml")
    @ExpectedDatabase(value = "/finishPicking/5/after_newflow.xml", assertionMode = NON_STRICT_UNORDERED)
    public void finishAssignmentAndUnassignSingleContainerHappyPathNewFlow() throws Exception {
        mockMvc.perform(put("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("finishPicking/5/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("finishPicking/5/response.json")))
                .andReturn();
    }

    /**
     * отбор завершен, все тоты отвязаны пользователя. Другая модель смены статуса, без лишней записи про 52й статус
     */
    @Test
    @DatabaseSetup(value = "/finishPicking/6/before_newflow.xml")
    @ExpectedDatabase(value = "/finishPicking/6/after_newflow.xml", assertionMode = NON_STRICT_UNORDERED)
    public void finishAssignmentAndUnassignAllContainerHappyPathNewFlow() throws Exception {
        mockMvc.perform(put("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("finishPicking/6/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("finishPicking/6/response.json")))
                .andReturn();
    }

    /**
     * Доотбор заказа, у которого отбираемый товар частично уже сортирован по службе доставки.
     * Когда надо отобрать много единиц одного и того же товара,
     * и это растягивается как по упаковкам, так и по времени, по сменам.
     */
    @Test
    @DatabaseSetup(value = "/finishPicking/7/before.xml")
    @ExpectedDatabase(value = "/finishPicking/7/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void finishAssignmentForPartiallySortedForDelivery() throws Exception {
        mockMvc.perform(put("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("finishPicking/7/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("finishPicking/7/response.json")))
                .andReturn();
    }

    /**
     * Контейнер с нонсортом перемещаем не в PICK_TO, волна SINGLE
     */
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/nonsort/before.xml")
    @ExpectedDatabase(value = "/controller/finishPicking/nonsort/after_induct_finish.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void finishNonSortAssignmentNotToPickTo_Old() throws Exception {
        mockMvc.perform(post("/finish-picking-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/finishPicking/nonsort/not-pickto-request-old.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    /**
     * Контейнер с нонсортом перемещаем не в PICK_TO, волна SINGLE, асинхронное создание TO
     */
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/async/before.xml")
    @ExpectedDatabase(value = "/controller/finishPicking/async/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void finishAssignmentAsync() throws Exception {
        mockMvc.perform(post("/finish-picking-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/finishPicking/nonsort/not-pickto-request-old.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    /**
     * Контейнер с нонсортом перемещаем не в PICK_TO, волна All
     */
    @Test
    @Disabled
    @DatabaseSetup(value = "/controller/finishPicking/nonsort/before_finish_all.xml")
    @DatabaseSetup(value = "/controller/finishPicking/nonsort/before_finish_all.xml")
    public void finishNonSortAssignmentNotToPickTo_Old_DefaultWave() throws Exception {
        mockMvc.perform(post("/finish-picking-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/finishPicking/nonsort/not-pickto-request-old.json")))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    /**
     * Контейнер с нонсортом перемещаем в PICK_TO
     */
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/nonsort/before_finish.xml")
    @ExpectedDatabase(value = "/controller/finishPicking/nonsort/after_finish_pickto.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void finishNonSortAssignmentToPickTo_Old() throws Exception {
        mockMvc.perform(post("/finish-picking-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/finishPicking/nonsort/pickto-request-old.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }


    /**
     * Контейнер с нонсортом перемещаем в PICK_TO
     */
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/nonsort/before.xml")
    @ExpectedDatabase(value = "/controller/finishPicking/nonsort/after_finish.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void finishNonSortAssignmentToPickTo() throws Exception {
        mockMvc.perform(put("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/finishPicking/nonsort/pickto-request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    // отбор завершен, нет TaskDetails
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/notaskdetails/before.xml")
    @ExpectedDatabase(value = "/controller/finishPicking/notaskdetails/after.xml", assertionMode = NON_STRICT)
    public void finishPickingOrderNoTaskDetails() throws Exception {
        mockMvc.perform(post("/finish-picking-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/finishPicking/notaskdetails/finish-picking-order-request.json")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/finishPicking/notaskdetails/finish-picking-order-response.json")))
                .andReturn();
        Mockito.verify(consolidationClient, Mockito.times(0)).moveContainerToLine(anyString(), anyString());
    }

    // отбор завершен, нет TaskDetails
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/notaskdetails/before.xml")
    @ExpectedDatabase(value = "/controller/finishPicking/notaskdetails/after.xml", assertionMode = NON_STRICT)
    public void dropContainerNoTaskDetails() throws Exception {
        mockMvc.perform(put("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/finishPicking/notaskdetails/drop-containers-request.json")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/finishPicking/notaskdetails/drop-containers-response.json")))
                .andReturn();
    }

    // (МОНО-отбор) Контейнер перемещаем на конвейер, станция назначения неконвеерная
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/8/before.xml")
    @ExpectedDatabase(value = "/controller/finishPicking/8/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void finishAssignmentConv_StrongConv_old() throws Exception {
        mockMvc.perform(post("/finish-picking-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/finishPicking/8/request-1.json")))
                .andExpect(status().is4xxClientError())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof NonConveyableException))
                .andReturn();
    }

    // (МУЛЬТИ-отбор) Контейнер перемещаем на конвейер, станция назначения неконвеерная
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/8/before.xml")
    @ExpectedDatabase(value = "/controller/finishPicking/8/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void finishAssignmentConv_StrongConv() throws Exception {
        mockMvc.perform(put("/containers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/finishPicking/8/request-2.json")))
                .andExpect(status().is4xxClientError())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof NonConveyableException))
                .andReturn();
    }

    // (МУЛЬТИ-отбор) Контейнер перемещаем на конвейер, станция назначения не назначена
    @Test
    @DatabaseSetup(value = "/controller/finishPicking/8-no-sort/before.xml")
    public void finishAssignmentConv_StrongConv_NoSortStation() throws Exception {
        mockMvc.perform(put("/containers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/finishPicking/8/request-2.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Mockito.verify(jmsTemplate, Mockito.atLeastOnce())
                .convertAndSend(Mockito.eq(DEFINE_SORT_STATION_FOR_WAVE), any(), any());
    }

}
