package ru.yandex.market.crm.triggers.services.bpm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.DeploymentWithDefinitions;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.crm.triggers.test.AbstractServiceTest;
import ru.yandex.market.mcrm.db.Constants;

import static org.junit.Assert.assertEquals;

/**
 * @author apershukov
 */
public class TriggersHealthServiceTest extends AbstractServiceTest {

    private static final String PROCESS_KEY = "idkfa";
    private static final String OK = "0;OK";

    @Inject
    private TriggersHealthService healthService;
    @Inject
    private RepositoryService repositoryService;

    @Inject
    @Named(Constants.DEFAULT_JDBC_TEMPLATE)
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testOkIfNoIncidentsFound() {
        assertEquals(OK, healthService.calculateState());
    }

    @Test
    public void testNotOkOnEmptyDB() {
        String processId = registerProcess();

        jdbcTemplate.update(
                """
                        INSERT INTO act_ru_incident (
                            id_,
                            rev_,
                            incident_timestamp_,
                            incident_msg_,
                            incident_type_,
                            proc_def_id_
                        ) VALUES (?, ?, ?, ?, ?, ?)""",
                UUID.randomUUID().toString(),
                1,
                LocalDateTime.now(),
                "Job have failed",
                "failedJob",
                processId
        );

        String status = healthService.calculateState();
        assertEquals("2;Unresolved incident of process 'idkfa'. Message: 'Job have failed'", status);
    }

    @Test
    @Ignore
    public void testNotOkIfJobMissedItsDueTimeByHalfHour() {
        registerJob(LocalDateTime.now().minusHours(1));

        String status = healthService.calculateState();
        assertEquals("2;Job of process 'idkfa' seems to hang", status);
    }

    @Test
    public void testOkIfJobMissesItsDueTimeBy15Minutes() {
        registerJob(LocalDateTime.now().minusMinutes(15));
        assertEquals(OK, healthService.calculateState());
    }

    private void registerJob(LocalDateTime dueTime) {
        jdbcTemplate.update(
                """
                        INSERT INTO act_ru_job (
                            id_,
                            type_,
                            duedate_,
                            process_def_key_,
                            suspension_state_
                        ) VALUES (?, ?, ?, ?, 1)""",
                UUID.randomUUID().toString(),
                "timer",
                dueTime,
                PROCESS_KEY
        );
    }

    private String registerProcess() {
        try {
            DeploymentWithDefinitions deployment = repositoryService.createDeployment()
                    .addString(
                            "crm.bpmn",
                            IOUtils.toString(getClass().getResourceAsStream("bpmn.xml"), StandardCharsets.UTF_8)
                    )
                    .name("Main deployment")
                    .enableDuplicateFiltering(true)
                    .deployWithResult();

            return deployment.getDeployedProcessDefinitions().get(0).getId();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
