package ru.yandex.market.wms.picking.modules.controller;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.common.model.dto.SortPickingAssignmentsRequest;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.exception.AssignmentNotFoundException;
import ru.yandex.market.wms.common.spring.exception.ZoneNotAllowedException;
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PickingAssignmentsControllerTest extends IntegrationTest {
    @Autowired
    @MockBean
    private JmsTemplate jmsTemplate;

    @BeforeEach
    public void before() {
        Mockito.reset(jmsTemplate);
    }

    @Test
    @DatabaseSetup(value = "/assignments/1/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/1/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getAssignmentAlreadyExistsHappyPath() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("assignments/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/1/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/assignments/2/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/2/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getAssignmentsAlreadyExistsHappyPath() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("assignments/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/2/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/assignments/3/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/3/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getNewAssignmentHappyPath() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("assignments/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/3/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("11111"))
        );
    }

    @Test
    @DatabaseSetup(value = "/assignments/4/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/4/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getNewAssignmentsHappyPath() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("assignments/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/4/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("11111", "22222"))
        );
    }

    @Test
    @DatabaseSetup(value = "/assignments/5/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/5/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getSingleAssignmentHappyPath() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("assignments/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/5/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("11111"))
        );
    }

    @Test
    @DatabaseSetup(value = "/assignments/6/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/6/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getAssignmentWithOneMaxAssignmentNumberSettingForPutAwayZone() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("assignments/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/6/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("11111"))
        );
    }

    @Test
    @DatabaseSetup(value = "/assignments/7/before.xml", connection = "wmwhseConnection")
    public void assignmentsInAreaNotFound() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("assignments/request.json")))
                .andExpect(status().is4xxClientError())
                .andExpect(result ->
                        Assert.assertTrue(result.getResolvedException() instanceof AssignmentNotFoundException))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/assignments/8/before.xml", connection = "wmwhseConnection")
    public void zoneNotFoundTest() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("assignments/bad-request.json")))
                .andExpect(status().is4xxClientError())
                .andExpect(result ->
                        Assert.assertTrue(result.getResolvedException() instanceof ZoneNotAllowedException))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/assignments/9/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/9/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getOutboundAssignment() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("assignments/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/9/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("11111"))
        );
    }

    @Test
    @DatabaseSetup(value = "/assignments/10/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/10/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void newAssignmentsWithOldContainers() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("assignments/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/10/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("11111"))
        );
    }

    @Test
    @DatabaseSetup(value = "/assignments/11/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/11/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getNewAssignmentWithHighestPriorityHappyPath() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("assignments/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/11/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("11111"))
        );
    }

    @Test
    @DatabaseSetup(value = "/assignments/12/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/12/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getNewAssignmentsWithLowerPriorityHappyPath() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("assignments/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/12/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("11111", "22222"))
        );
    }

    /**
     * У двух заказов равны все параметры, кроме даты отгрузки (SHIPMENTDATETIME)
     * Для отбора должен быть выбран заказ с более ранней датой отгрузки (он имеет больший приоритет)
     */
    @Test
    @DatabaseSetup(value = "/assignments/13/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/13/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getNewAssignmentWithEarliestShipmentDateTimeHappyPath() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("assignments/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/13/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("11111"))
        );
    }

    /**
     * Тест заказа на обратный отбор
     * При включенной настройке выберется задание из той же зоны, что и последнее задание (MEZONIN_2),
     * несмотря на то, что при сортировке свободных заданий с одним и тем же приоритетом раньше вернется
     * задание из зоны MEZONIN_1
     */
    @Test
    @DatabaseSetup(value = "/assignments/14/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/14/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getAssignmentsReversePicking() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("assignments/14/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/14/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("AN3"))
        );
    }

    /**
     * Тест заказа на обратный отбор
     * Так как настройка выключена, то вернется задание из зоны MEZONIN_1, несмотря на то, что последний
     * отбор проводился в зоне MEZONIN_2
     */
    @Test
    @DatabaseSetup(value = "/assignments/15/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/15/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getAssignmentsReversePickingTurnedOff() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("assignments/15/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/15/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("AN2"))
        );
    }

    /**
     * Тест заказа на обратный отбор
     * Последний отбор производился из зоны MEZONIN_2, но сейчас в зоне MEZONIN_1 есть задание
     * со строго большим приоритетом, чем задания в MEZONIN_2, поэтому вернется из MEZONIN_1
     */
    @Test
    @DatabaseSetup(value = "/assignments/16/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/16/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getAssignmentsReversePickingLowerPriority() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("assignments/16/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/16/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("AN2"))
        );
    }

    /**
     * Тест заказа на обратный отбор
     * Хоть настройка и включена, но userId не проходит проверку по регулярному выражению,
     * то вернется задание из зоны MEZONIN_1, несмотря на то, что последний
     * отбор проводился в зоне MEZONIN_2
     */
    @Test
    @DatabaseSetup(value = "/assignments/17/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/17/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getAssignmentsReversePickingUserRegexpNotMatched() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("assignments/17/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/17/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("AN2"))
        );
    }

    /**
     * Тест заказа на обратный отбор (флаг в path variables, а не в payload)
     * При включенной настройке выберется задание из той же зоны, что и последнее задание (MEZONIN_2),
     * несмотря на то, что при сортировке свободных заданий с одним и тем же приоритетом раньше вернется
     * задание из зоны MEZONIN_1
     */
    @Test
    @DatabaseSetup(value = "/assignments/18/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/18/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getAssignmentsReversePickingWithFlagInPath() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                        .param("pickContinued", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("assignments/18/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/18/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("AN3"))
        );
    }

    /**
     * Тест заказа на обратный отбор
     * Так как последнее задание выполнялось слишком давно, то вернется задание из зоны MEZONIN_1,
     * несмотря на то, что последний отбор проводился в зоне MEZONIN_2
     */
    @Test
    @DatabaseSetup(value = "/assignments/19/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/19/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getAssignmentsReversePickingOldLastAssignment() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("assignments/19/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/19/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("AN2"))
        );
    }

    /**
     * Тест заказа на обратный отбор
     * Так как последнее назначение было в MEZONIN_2, для которого настройка выключена,
     * то вернется задание из зоны MEZONIN_1, несмотря на то, что последний отбор проводился в зоне MEZONIN_2
     */
    @Test
    @DatabaseSetup(value = "/assignments/20/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/assignments/20/after.xml", assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getAssignmentsReversePickingAreaIsTurnedOff() throws Exception {
        mockMvc.perform(post("/picking-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("assignments/20/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("assignments/20/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("AN2"))
        );
    }

    private SortPickingAssignmentsRequest buildRequest(String... o) {
        return SortPickingAssignmentsRequest.builder().assignmentNumbers(List.of(o)).build();
    }
}
