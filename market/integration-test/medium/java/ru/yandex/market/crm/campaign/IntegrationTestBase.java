package ru.yandex.market.crm.campaign;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.crm.core.test.TestEnvironmentResolver;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.crm.core.yt.paths.YtFolders;

/**
 * Created by vivg on 23.06.17.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = TestAppConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("/test.properties")
public abstract class IntegrationTestBase {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    @Inject
    protected TestEnvironmentResolver environmentResolver;

    @Inject
    protected YtFolders ytFolders;

    @Inject
    protected YtClient ytClient;

    @Before
    public void setUp() throws Exception {
        environmentResolver.reset();
        try {
            cleanTestDirectory();
        } catch (Exception ex) {
            LOG.warn("Cannot clean test directory", ex);
        }
    }

    private void cleanTestDirectory() {
        ytClient.cleanFolder(ytFolders.getHome());
    }

}
