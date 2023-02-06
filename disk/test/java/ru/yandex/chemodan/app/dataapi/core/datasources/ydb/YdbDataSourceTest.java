package ru.yandex.chemodan.app.dataapi.core.datasources.ydb;

import org.junit.Before;
import org.junit.Ignore;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.chemodan.app.dataapi.core.datasources.disk.DiskDataSourceTest;
import ru.yandex.chemodan.app.dataapi.core.datasources.migration.DsMigrationDataSourceRegistry;

import static ru.yandex.chemodan.app.dataapi.core.dao.test.ActivateDataApiEmbeddedPg.DATAAPI_EMBEDDED_PG;
import static ru.yandex.misc.db.embedded.ActivateEmbeddedPg.EMBEDDED_PG;

/**
 * @author tolmalev
 */
@ActiveProfiles({DATAAPI_EMBEDDED_PG, EMBEDDED_PG, "dataapi", "dataapi-ydb-test"})
@Ignore
public class YdbDataSourceTest extends DiskDataSourceTest {

    @Autowired
    private DsMigrationDataSourceRegistry dsMigrationDataSourceRegistry;

    @Autowired
    private YdbDataSource ydbDataSource;

    @Before
    public void Before() {
        super.Before();
        dataSource = ydbDataSource;

        Mockito.doReturn(ydbDataSource).when(dsMigrationDataSourceRegistry).getDataSource(Mockito.any());
    }
}
