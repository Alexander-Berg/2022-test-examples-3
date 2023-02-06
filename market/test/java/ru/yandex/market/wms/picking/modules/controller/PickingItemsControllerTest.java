package ru.yandex.market.wms.picking.modules.controller;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.wms.common.model.dto.SortPickingItemsRequest;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.picking.core.model.PickingOrderItemDTO;
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PickingItemsControllerTest extends IntegrationTest {
    @Autowired
    @MockBean
    private JmsTemplate jmsTemplate;

    @BeforeEach
    public void before() {
        Mockito.reset(jmsTemplate);
    }

    @Test
    @DatabaseSetup(value = "/picking-item/happy/init_db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/picking-item/happy/init_db.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void getPickingItemsHappyPath() throws Exception {
        mockMvc.perform(get("/picking-items")
                        .param("assignmentNumbers", "01", "02"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("picking-item/happy/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/picking-item/one-assignment/init_db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/picking-item/one-assignment/init_db.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void getPickingItemsOneAssignment() throws Exception {
        mockMvc.perform(get("/picking-items")
                        .param("assignmentNumbers", "02"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("picking-item/one-assignment/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/picking-item/wrong-assignments/init_db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/picking-item/wrong-assignments/init_db.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void getPickingItemsWrongAssignment() throws Exception {
        mockMvc.perform(get("/picking-items")
                        .param("assignmentNumbers", "03"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("picking-item/wrong-assignments/response.json")))
                .andReturn();
    }

    @Test
    public void getPickingItemsNoAssignment() throws Exception {
        mockMvc.perform(get("/picking-items"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/picking-item/multiple-locs/init_db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/picking-item/multiple-locs/init_db.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void getPickingItemsMultipleLocations() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/picking-items")
                        .param("assignmentNumbers", "AN1", "AN2"))
                .andExpect(content().json(getFileContent("picking-item/multiple-locs/response.json")))
                .andReturn();
        List<PickingOrderItemDTO> pickingItems = new ObjectMapper()
                .readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<>() { });
        assertEquals(5, pickingItems.size());
        assertPickingItemParams(pickingItems.get(0), "1-05", "AN2");
        assertPickingItemParams(pickingItems.get(1), "1-04", "AN1");
        assertPickingItemParams(pickingItems.get(2), "1-03", "AN1");
        assertPickingItemParams(pickingItems.get(3), "1-02", "AN2");
        assertPickingItemParams(pickingItems.get(4), "1-01", "AN1");
    }

    @Test
    @DatabaseSetup(value = "/picking-item/multiple-locs-item-added/init_db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/picking-item/multiple-locs-item-added/init_db.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void getPickingItemsMultipleLocations_itemAddedToExistingAssignment() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/picking-items")
                        .param("assignmentNumbers", "AN1", "AN2"))
                .andExpect(content().json(getFileContent("picking-item/multiple-locs-item-added/response.json")))
                .andReturn();
        List<PickingOrderItemDTO> pickingItems = new ObjectMapper()
                .readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<>() { });
        assertEquals(5, pickingItems.size());
        assertPickingItemParams(pickingItems.get(0), "1-05", "AN2");
        assertPickingItemParams(pickingItems.get(1), "1-03", "AN1");
        assertPickingItemParams(pickingItems.get(2), "1-02", "AN2");
        assertPickingItemParams(pickingItems.get(3), "1-01", "AN1");
        assertPickingItemParams(pickingItems.get(4), "1-04", "AN1");
        SortPickingItemsRequest request = buildRequest(
                List.of("000000005", "000000003", "000000002", "000000001", "000000004"));
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ITEMS),
                Mockito.eq(request)
        );
    }

    @Test
    @DatabaseSetup(value = "/picking-item/multiple-locs-already-sorted/init_db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/picking-item/multiple-locs-already-sorted/init_db.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void getPickingItemsMultipleLocationsAlreadySorted() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/picking-items")
                        .param("assignmentNumbers", "AN1", "AN2"))
                .andExpect(content().json(getFileContent("picking-item/multiple-locs-already-sorted/response.json")))
                .andReturn();
        List<PickingOrderItemDTO> pickingItems = new ObjectMapper()
                .readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<>() { });
        assertEquals(5, pickingItems.size());
        assertPickingItemParams(pickingItems.get(0), "1-01", "AN1");
        assertPickingItemParams(pickingItems.get(1), "1-03", "AN1");
        assertPickingItemParams(pickingItems.get(2), "1-04", "AN1");
        assertPickingItemParams(pickingItems.get(3), "1-02", "AN2");
        assertPickingItemParams(pickingItems.get(4), "1-05", "AN2");
        Mockito.verify(jmsTemplate, Mockito.times(0)).convertAndSend(
                Mockito.eq(QueueNameConstants.SORT_PICKING_ITEMS),
                Mockito.any(Object.class)
        );
    }

    private void assertPickingItemParams(PickingOrderItemDTO itemDTO, String fromLoc, String assignmentNumber) {
        assertEquals(fromLoc, itemDTO.getFromLoc());
        assertEquals(assignmentNumber, itemDTO.getAssignmentNumber());
    }

    private SortPickingItemsRequest buildRequest(List<String> taskdetailKeys) {
        return SortPickingItemsRequest.builder()
                .taskDetailKeys(taskdetailKeys)
                .firstItemTaskDetailKey(taskdetailKeys.get(0))
                .build();
    }
}
