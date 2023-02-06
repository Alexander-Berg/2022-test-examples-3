package ru.yandex.market.crm.campaign.http.controller;

import java.time.LocalDateTime;

import javax.inject.Inject;
import javax.inject.Named;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.tasks.domain.TaskIncident;
import ru.yandex.market.crm.tasks.domain.TaskStatus;
import ru.yandex.market.crm.tasks.services.ClusterTasksHealthService;
import ru.yandex.market.crm.tasks.services.TaskIncidentsDAO;
import ru.yandex.market.mcrm.db.Constants;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author apershukov
 */
public class HealthControllerTest extends AbstractControllerMediumTest {

    private static final String TASK_ID = "ClusterTask";
    private static final String OK_RESPONSE = "0;OK";

    @Inject
    private TaskIncidentsDAO taskIncidentsDAO;

    @Inject
    @Named(Constants.DEFAULT_JDBC_TEMPLATE)
    private JdbcTemplate jdbcTemplate;

    @Inject
    private ClusterTasksHealthService clusterTasksHealthService;

    /**
     * В случае если инцидентов по кластерным таскам в системе нет ручка GET /api/health/cluster-tasks-status
     * возвращает "0;OK"
     */
    @Test
    public void testIfNoIncidentsTaskStatusIsOk() throws Exception {
        assertTaskStatusResponse(OK_RESPONSE);
    }

    /**
     * В случае если есть хотя бы один инцидент по кластерным таскам ручка GET /api/health/cluster-tasks-status
     * возвращает ответ вида:
     * <p>
     * "2;Cluster task incident. TASK: ${Id таски}, INSTANCE: ${Id инстанса таски}, MESSAGE: ${Текст ошибки}"
     */
    @Test
    public void testIncidentsExistTaskStatusIsError() throws Exception {
        TaskIncident incident = new TaskIncident()
                .setTaskId("ClusterTask")
                .setTaskInstanceId(111)
                .setMessage("Error");

        taskIncidentsDAO.addIncident(incident);

        String expected = "2;Cluster task incident. TASK: ClusterTask, INSTANCE: 111, MESSAGE: Error";
        assertTaskStatusResponse(expected);
    }

    /**
     * В случае если выполняемая таска была залочена больше чем на час ручка
     * GET /api/health/cluster-tasks-status возвращает ответ вида:
     * <p>
     * 2;Cluster task seems to be stuck. TASK: ${Id таски}, INSTANCE: ${Id инстанса таски}
     */
    @Test
    public void testNotOkIfRunningInstanceIsLockedForMoreThan1Hour() throws Exception {
        long instanceId = insertRunningTask(LocalDateTime.now().minusHours(2));

        String expected = String.format(
                "2;Cluster task seems to be stuck. TASK: %s, INSTANCE: %s",
                TASK_ID,
                instanceId
        );

        assertTaskStatusResponse(expected);
    }

    /**
     * В случае если выполняемая таска была залочена менее чем на час ручка
     * GET /api/health/cluster-tasks-status возвращает "0;OK"
     */
    @Test
    public void testOkIfRunningInstanceIsLockedForLessThan1Hour() throws Exception {
        insertRunningTask(LocalDateTime.now().minusMinutes(20));
        assertTaskStatusResponse(OK_RESPONSE);
    }

    @NotNull
    private String requestClusterTaskStatus() throws Exception {
        return mockMvc.perform(get("/api/health/cluster-tasks-status"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString();
    }

    private long insertRunningTask(LocalDateTime lastAcquiredTime) {
        Long id = jdbcTemplate.queryForObject(
                """
                        INSERT INTO cluster_tasks (
                            task_id,
                            status,
                            creation_time,
                            next_run_time,
                            node_key,
                            last_acquired_time
                        )
                        VALUES (?, ?, current_timestamp, ?, ?, ?)
                        RETURNING id""",
                Long.class,
                TASK_ID,
                TaskStatus.RUNNING.name(),
                LocalDateTime.now(),
                "sas.search.yandex.net",
                lastAcquiredTime
        );

        // To make idea analyzer happy
        if (id == null) {
            throw new IllegalStateException("Unable to retrieve primary key");
        }

        return id;
    }

    private void assertTaskStatusResponse(String expectedResponse) throws Exception {
        clusterTasksHealthService.updateStatus();
        String status = requestClusterTaskStatus();
        Assertions.assertEquals(expectedResponse, status);
    }
}
