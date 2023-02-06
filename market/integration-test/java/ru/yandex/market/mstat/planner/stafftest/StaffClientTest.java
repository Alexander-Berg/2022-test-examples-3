package ru.yandex.market.mstat.planner.stafftest;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.mstat.planner.client.StaffClient;
import ru.yandex.market.mstat.planner.config.TestConfig;
import ru.yandex.market.starter.postgres.config.ZonkyEmbeddedPostgresConfiguration;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ZonkyEmbeddedPostgresConfiguration.class, TestConfig.class})
@TestPropertySource({"classpath:properties/10_application_test.properties"})
public class StaffClientTest {

    @Autowired
    private StaffClient client;

    @Test
    public void testPersonLoad() {
        JsonNode em = client.getPersons(null, null,"pavellysenko");

        assertTrue(em.has("login"));
        assertTrue(em.has("department_group"));
    }

    @Test
    public void testPersonsLoad() {
        JsonNode employees = client.getPersons("yandex", "56458", null);

        assertTrue(employees.has("result"));
        assertTrue(employees.has("pages"));
        assertTrue(employees.has("page"));

    }


}
