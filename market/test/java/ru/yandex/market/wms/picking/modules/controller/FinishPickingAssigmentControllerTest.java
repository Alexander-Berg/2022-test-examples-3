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
     * ?????????? ????????????????. ???????????? ???????????? ?????????? ??????????????, ?????? ???????????? ???????????? ?????? 52?? ????????????
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
     * ?????????? ???????????????? ????????????????. ???????????? ???????????? ?????????? ??????????????, ?????????????????? ???????????? ?? orderdetail
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
     * ?????????? ?????? ?????????????? ????????????????, ???????????????????? ????????????????
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
     * ???????????????????? ???? ??????????????, ???????????? ???? ??????????????, ???????????? ??????
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
     * ?????????????????? ?????????????? ???? ??????????????
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
     * ?????????? ???????????????? ?? ?????????????????????? ???????????? ????????????????????????, ???????????????????????? callConsolidation=true
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
     * ?????????? ????????????????, ?????? ?????????????? ???? ????????????????????????. ???????????? ???????????? ?????????? ??????????????, ?????? ???????????? ???????????? ?????? 52?? ????????????
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
     * ?????????? ????????????????, ?????? ???????? ???????????????? ????????????????????????. ???????????? ???????????? ?????????? ??????????????, ?????? ???????????? ???????????? ?????? 52?? ????????????
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
     * ?????????????? ????????????, ?? ???????????????? ???????????????????? ?????????? ???????????????? ?????? ???????????????????? ???? ???????????? ????????????????.
     * ?????????? ???????? ???????????????? ?????????? ???????????? ???????????? ?? ???????? ???? ????????????,
     * ?? ?????? ?????????????????????????? ?????? ???? ??????????????????, ?????? ?? ???? ??????????????, ???? ????????????.
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
     * ?????????????????? ?? ?????????????????? ???????????????????? ???? ?? PICK_TO, ?????????? SINGLE
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
     * ?????????????????? ?? ?????????????????? ???????????????????? ???? ?? PICK_TO, ?????????? SINGLE, ?????????????????????? ???????????????? TO
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
     * ?????????????????? ?? ?????????????????? ???????????????????? ???? ?? PICK_TO, ?????????? All
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
     * ?????????????????? ?? ?????????????????? ???????????????????? ?? PICK_TO
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
     * ?????????????????? ?? ?????????????????? ???????????????????? ?? PICK_TO
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

    // ?????????? ????????????????, ?????? TaskDetails
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

    // ?????????? ????????????????, ?????? TaskDetails
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

    // (????????-??????????) ?????????????????? ???????????????????? ???? ????????????????, ?????????????? ???????????????????? ????????????????????????
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

    // (????????????-??????????) ?????????????????? ???????????????????? ???? ????????????????, ?????????????? ???????????????????? ????????????????????????
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

    // (????????????-??????????) ?????????????????? ???????????????????? ???? ????????????????, ?????????????? ???????????????????? ???? ??????????????????
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
