package ru.yandex.chemodan.app.dataapi.core.datasources.ydb;

import com.yandex.ydb.table.TableClient;
import org.junit.After;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.dataapi.api.datasource.DataSourceSessionImplTest;
import ru.yandex.chemodan.app.dataapi.api.datasource.DataSourceType;
import ru.yandex.chemodan.app.dataapi.api.datasource.SessionProviderTestContextConfiguration;
import ru.yandex.chemodan.ydb.dao.ThreadLocalYdbTransactionManager;

/**
 * @author tolmalev
 */
@RunWith(Parameterized.class)
@ContextConfiguration(classes = SessionProviderTestContextConfiguration.class)
@ActiveProfiles({"dataapi", "dataapi-ydb-test"})
@Ignore
public class YdbDataSourceSessionImplTest extends DataSourceSessionImplTest {

    @Autowired
    private ThreadLocalYdbTransactionManager transactionManager;

    public YdbDataSourceSessionImplTest(DataSourceType type) {
        super(type);
    }

    @After
    public void checkAllSessionsIdle() {
        TableClient tableClient = transactionManager.getTableClientForTests();

        // please rely only on public interface
        // Object sessionPool = ReflectionUtils.getField(tableClient, "sessionPool");
        // Object idleSessions = ReflectionUtils.getField(sessionPool, "idleSessions");
        // for (Object idleSession : (ArrayList) idleSessions) {
        //     Object state = ReflectionUtils.getField(idleSession, "state");
        //     Assert.equals("IDLE", state.toString(), "Found not idle session!");
        // }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return Cf.list(DataSourceType.YDB).map(t -> new Object[]{t});
    }
}
