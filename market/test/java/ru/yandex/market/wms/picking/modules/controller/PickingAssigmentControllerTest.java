package ru.yandex.market.wms.picking.modules.controller;

import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.common.model.dto.SortPickingAssignmentsRequest;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PickingAssigmentControllerTest extends IntegrationTest {
    @Autowired
    @MockBean
    private JmsTemplate jmsTemplate;

    @BeforeEach
    public void before() {
        Mockito.reset(jmsTemplate);
    }

    @Test
    @DatabaseSetup("/controller/assign-task/1/user_activity.xml")
    @DatabaseSetup("/controller/assign-task/1/task_detail.xml")
    @ExpectedDatabase(value = "/controller/assign-task/1/after.xml", assertionMode = NON_STRICT)
    public void getAssignmentAlreadyExistsHappyPath() throws Exception {
        mockMvc.perform(get("/picking-order/TEST_WORKING_AREA"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/assign-task/response.json")))
                .andReturn();
    }

    // workingArea param is empty
    @Test
    @DatabaseSetup("/controller/assign-task/1/user_activity.xml")
    @DatabaseSetup("/controller/assign-task/1/task_detail.xml")
    @ExpectedDatabase(value = "/controller/assign-task/1/after.xml", assertionMode = NON_STRICT)
    public void getAssignmentFromFreeHappyPath() throws Exception {
        mockMvc.perform(get("/picking-order"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/assign-task/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/assign-task/2/user_activity.xml")
    @DatabaseSetup("/controller/assign-task/2/task_detail.xml")
    @DatabaseSetup("/controller/assign-task/2/area_detail.xml")
    @DatabaseSetup("/controller/assign-task/2/task_manager_user_detail.xml")
    public void getAssignmentNotFoundPutAwayZone() throws Exception {
        mockMvc.perform(get("/picking-order/TEST_WORKING_AREA"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/assign-task/3/user_activity_before.xml")
    @DatabaseSetup("/controller/assign-task/3/task_detail.xml")
    @DatabaseSetup("/controller/assign-task/3/area_detail.xml")
    @DatabaseSetup("/controller/assign-task/3/task_manager_user_detail.xml")
    @DatabaseSetup("/controller/assign-task/3/orders.xml")
    @DatabaseSetup("/controller/assign-task/3/wave_detail.xml")
    @DatabaseSetup("/controller/assign-task/3/delivery_service_cutoffs.xml")
    public void getAssignmentWithNullWorkingZone() throws Exception {
        mockMvc.perform(get("/picking-order"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/assign-task/4/user_activity.xml")
    @DatabaseSetup("/controller/assign-task/4/task_detail.xml")
    @DatabaseSetup("/controller/assign-task/4/area_detail.xml")
    @DatabaseSetup("/controller/assign-task/4/task_manager_user_detail.xml")
    public void getAssignmentNotFound() throws Exception {
        mockMvc.perform(get("/picking-order/TEST_WORKING_AREA"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/assign-task/3/user_activity_before.xml")
    @DatabaseSetup("/controller/assign-task/3/task_detail.xml")
    @DatabaseSetup("/controller/assign-task/3/area_detail.xml")
    @DatabaseSetup("/controller/assign-task/3/task_manager_user_detail.xml")
    @DatabaseSetup("/controller/assign-task/3/orders.xml")
    @DatabaseSetup("/controller/assign-task/3/wave_detail.xml")
    @DatabaseSetup("/controller/assign-task/3/delivery_service_cutoffs.xml")
    public void getAssignmentFromNullZones() throws Exception {
        mockMvc.perform(get("/picking-order"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    /**
     * У двух заказов равны все параметры, кроме даты отгрузки (SHIPMENTDATETIME)
     * Для отбора должен быть выбран заказ с более ранней датой отгрузки (он имеет больший приоритет)
     */
    @Test
    @DatabaseSetup(value = "/controller/assign-task/5/before.xml")
    @ExpectedDatabase(value = "/controller/assign-task/5/after.xml", assertionMode = NON_STRICT)
    public void getNewAssignmentWithEarliestShipmentDateTimeHappyPath() throws Exception {
        mockMvc.perform(get("/picking-order/MEZ1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/assign-task/5/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("11111"))
        );
    }

    @Test
    @DatabaseSetup("/controller/assign-task/6/before.xml")
    @ExpectedDatabase(value = "/controller/assign-task/6/after.xml", assertionMode = NON_STRICT)
    public void getAssignmentIgnoringOldTaskWithoutPickDetails() throws Exception {
        mockMvc.perform(get("/picking-order/FLOOR1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/assign-task/6/response.json")))
                .andReturn();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ASSIGNMENTS),
                Mockito.eq(buildRequest("AN2"))
        );
    }

    /**
     * Оригинальный заказ имеет тип OUTBOUND_AUCTION, возвращается флаг arbitrarySerialPick = true
     */
    @Test
    @DatabaseSetup("/controller/assign-task/7/user_activity.xml")
    @DatabaseSetup("/controller/assign-task/7/task_detail.xml")
    @ExpectedDatabase(value = "/controller/assign-task/7/after.xml", assertionMode = NON_STRICT)
    public void getAssignmentAlreadyExistsOutboundAuction() throws Exception {
        mockMvc.perform(get("/picking-order/TEST_WORKING_AREA"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/assign-task/7/response.json")))
                .andReturn();
    }

    private SortPickingAssignmentsRequest buildRequest(String o) {
        return SortPickingAssignmentsRequest.builder().assignmentNumbers(Collections.singletonList(o)).build();
    }
}
