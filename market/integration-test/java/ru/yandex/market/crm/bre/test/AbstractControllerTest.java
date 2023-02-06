package ru.yandex.market.crm.bre.test;

import javax.inject.Inject;

import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.mcrm.db.test.DbTestTool;
import ru.yandex.market.mcrm.http.HttpEnvironment;

/**
 * @author apershukov
 */
@WebAppConfiguration
@ContextConfiguration(classes = ControllerTestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("/bre_test.properties")
@ActiveProfiles("test")
public abstract class AbstractControllerTest {

    @Inject
    protected MockMvc mockMvc;

    @Inject
    private DbTestTool dbTestTool;

    @Inject
    private HttpEnvironment httpEnvironment;

    @After
    public void commonTearDown() {
        dbTestTool.clearDatabase();
        httpEnvironment.tearDown();
    }
}
