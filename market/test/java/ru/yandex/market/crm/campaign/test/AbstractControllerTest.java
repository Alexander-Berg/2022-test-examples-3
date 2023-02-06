package ru.yandex.market.crm.campaign.test;

import javax.inject.Inject;

import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.mcrm.db.test.DbTestTool;

/**
 * @author apershukov
 */
@WebAppConfiguration
@ContextConfiguration(classes = ControllerTestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractControllerTest {

    @Inject
    protected MockMvc mockMvc;
    @Inject
    private DbTestTool dbTestTool;

    @After
    public void tearDown() {
        dbTestTool.clearDatabase();
    }
}
