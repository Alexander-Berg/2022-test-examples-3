package ru.yandex.chemodan.app.orchestrator;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.misc.db.embedded.ActivateEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;

import static ru.yandex.misc.db.embedded.ActivateEmbeddedPg.EMBEDDED_PG;

/**
 * @author yashunsky
 */
@ContextConfiguration(classes = {
        OrchestratorCoreTestConfig.class,
        OrchestratorEmbeddedPgContextConfiguration.class,
})
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles(EMBEDDED_PG)
@ActivateEmbeddedPg
public abstract class AbstractOrchestratorCoreTest {
    @Before
    public void initialize() {
        PreparedDbProvider.truncateDatabases("public");
    }
}
