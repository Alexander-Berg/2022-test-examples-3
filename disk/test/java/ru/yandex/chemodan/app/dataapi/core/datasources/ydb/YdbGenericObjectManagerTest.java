package ru.yandex.chemodan.app.dataapi.core.datasources.ydb;

import org.junit.Before;
import org.junit.Ignore;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.chemodan.app.dataapi.api.db.ref.DatabaseRef;
import ru.yandex.chemodan.app.dataapi.core.datasources.migration.DsMigrationDataSourceRegistry;
import ru.yandex.chemodan.app.dataapi.core.datasources.ydb.dao.DataRecordsYdbDao;
import ru.yandex.chemodan.app.dataapi.core.generic.GenericObjectManagerTestBase;
import ru.yandex.chemodan.ydb.dao.ThreadLocalYdbTransactionManager;

import static ru.yandex.chemodan.app.dataapi.core.dao.test.ActivateDataApiEmbeddedPg.DATAAPI_EMBEDDED_PG;
import static ru.yandex.misc.db.embedded.ActivateEmbeddedPg.EMBEDDED_PG;

/**
 * @author tolmalev
 */
@ActiveProfiles({DATAAPI_EMBEDDED_PG, EMBEDDED_PG, "dataapi", "dataapi-ydb-test"})
@ContextConfiguration(classes = YdbGenericObjectManagerTest.Context.class)
@Ignore
public class YdbGenericObjectManagerTest extends GenericObjectManagerTestBase {

    @Autowired
    private DsMigrationDataSourceRegistry dsMigrationDataSourceRegistry;

    @Autowired
    private YdbDataSource ydbDataSource;
    @Autowired
    private DataRecordsYdbDao ydbRecordsDao;

    @Before
    public void Before() {
        super.before();

        Mockito.doReturn(ydbDataSource).when(dsMigrationDataSourceRegistry).getDataSource(Mockito.any());
        Mockito.clearInvocations(ydbRecordsDao);
    }

    protected void assertDbCallsCount() {
        Mockito
                .inOrder(ydbRecordsDao)
                .verify(ydbRecordsDao, Mockito.calls(1))
                .find(
                        Mockito.any(),
                        Mockito.any(DatabaseRef.class),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
                );
    }

    protected void assertDbCallsCount2(VerificationMode calls) {
        Mockito
                .inOrder(ydbRecordsDao)
                .verify(ydbRecordsDao, Mockito.calls(1))
                .find(
                        Mockito.any(),
                        Mockito.any(DatabaseRef.class),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
                );

        Mockito
                .inOrder(ydbRecordsDao)
                .verify(ydbRecordsDao, calls)
                .count(Mockito.any(), Mockito.any(DatabaseRef.class), Mockito.any(), Mockito.any());
    }

    @ContextConfiguration
    static class Context {
        @Bean
        @Primary
        public DataRecordsYdbDao dataRecordsYdbDao(ThreadLocalYdbTransactionManager transactionManager) {
            return Mockito.spy(new DataRecordsYdbDao(transactionManager));
        }
    }

}
