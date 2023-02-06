package ru.yandex.market.crm.campaign.http.controller;

import java.time.LocalDateTime;
import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.tasks.domain.TaskIncident;
import ru.yandex.market.crm.tasks.services.TaskIncidentsDAO;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author apershukov
 */
public class ClusterTasksControllerTest extends AbstractControllerMediumTest {

    @Inject
    private TaskIncidentsDAO taskIncidentsDAO;

    @Inject
    private JsonDeserializer jsonDeserializer;

    /**
     * В выдаче ручки GET /api/cluster_tasks/incidents
     * Присутствуют все инциденты, имеющиеся в системе
     */
    @Test
    public void testGetAllIncidents() throws Exception {
        TaskIncident incident = prepareIncident();

        MvcResult result = mockMvc.perform(get("/api/cluster_tasks/incidents"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        List<TaskIncident> incidents = jsonDeserializer.readObject(
                new TypeReference<>() {
                },
                result.getResponse().getContentAsString()
        );

        Assertions.assertEquals(1, incidents.size());

        TaskIncident responseIncident = incidents.get(0);
        Assertions.assertEquals(incident.getTaskId(), responseIncident.getTaskId());
        Assertions.assertEquals(incident.getTaskInstanceId(), responseIncident.getTaskInstanceId());
        Assertions.assertEquals(incident.getMessage(), responseIncident.getMessage());
        Assertions.assertNotNull(responseIncident.getTime());
    }

    /**
     * Ручка DELETE /api/cluster_tasks/incidents удаляет все инциденты
     */
    @Test
    public void testDeleteAllIncidents() throws Exception {
        prepareIncident();

        mockMvc.perform(delete("/api/cluster_tasks/incidents"))
                .andDo(print())
                .andExpect(status().isOk());

        List<TaskIncident> incidents = taskIncidentsDAO.getAllIncidents();
        Assertions.assertTrue(incidents.isEmpty(), "Some incidents are still in system");
    }

    private TaskIncident prepareIncident() {
        TaskIncident incident = new TaskIncident()
                .setTaskId("ClusterTask")
                .setTaskInstanceId(111)
                .setMessage("Error happened")
                .setTime(LocalDateTime.now());

        taskIncidentsDAO.addIncident(incident);
        return incident;
    }
}
