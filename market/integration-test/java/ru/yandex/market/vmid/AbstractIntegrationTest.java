package ru.yandex.market.vmid;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.vmid.config.DatabaseConfigTest;
import ru.yandex.market.vmid.config.PGaaSZonkyInitializer;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(initializers = {PGaaSZonkyInitializer.class},
        classes = {DatabaseConfigTest.class})
@AutoConfigureMockMvc(addFilters = false)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Before
    public void init() {
        jdbcTemplate.execute("DELETE FROM IDS");
        jdbcTemplate.execute("ALTER SEQUENCE CURR_VMID RESTART");
    }

}

