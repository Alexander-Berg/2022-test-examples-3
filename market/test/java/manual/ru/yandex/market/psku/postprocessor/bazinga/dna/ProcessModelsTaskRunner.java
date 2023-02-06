package manual.ru.yandex.market.psku.postprocessor.bazinga.dna;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import manual.ru.yandex.market.psku.postprocessor.bazinga.dna.config.ManualTestDaoConfiguration;
import manual.ru.yandex.market.psku.postprocessor.bazinga.dna.config.ManualTestServicesConfiguration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.psku.postprocessor.bazinga.dna.ProcessModelsTask;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.CleanupStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.DeletedMappingModels;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        initializers = PGaaSZonkyInitializer.class,
        classes = {
            ManualTestServicesConfiguration.class,
            ManualTestDaoConfiguration.class
        }
)
@TestPropertySource(properties = {
    "user.agent=psku-post-processor",
//    "mbo.http-exporter.url=http://mbo-http-exporter.tst.vs.market.yandex.net:8084/categoryModels/",
    "mbo.http-exporter.url=http://mbo-http-exporter.market.yandex.net:8084/categoryModels/",
//    "ppp.card-api.url=http://mbo-card-api.tst.vs.market.yandex.net:33714/modelStorage/",
    "ppp.card-api.url=http://mbo-card-api.http.yandex.net:33714/modelStorage/",
//    "mboc.mappings.service.uri=http://cm-api.tst.vs.market.yandex.net/proto/mboMappingsService/",
    "mboc.mappings.service.uri=http://cm-api.vs.market.yandex.net/proto/mboMappingsService/",
    "mboc.category.service.uri=http://cm-api.vs.market.yandex.net/proto/mboCategoryService/",
    "market.psku-post-processor.jdbc.driverClassName=org.postgresql.Driver",
    "market.psku-post-processor.jdbc.url=jdbc:postgresql://sas-fxdvlcwgox9yblbf.db.yandex.net:6432," +
            "vla-we5mg7hpzz453gvk.db.yandex.net:6432/psku_post_process?" +
            "&targetServerType=master&ssl=true&sslmode=require&prepareThreshold=0&preparedStatementCacheQueries=0",
    "market.psku-post-processor.username=psku_post_process",
    "market.psku-post-processor.password="
        //https://yav.yandex-team.ru/secret/sec-01dg562mesjcngk0pr2cp64ayy/explore/versions
})
public class ProcessModelsTaskRunner {

    @Autowired
    private DeletedMappingModelsDao deletedMappingModelsDao;

    @Autowired
    private ProcessModelsTask processModelsTask;

    @Autowired
    private ModelStorageHelper modelStorageHelper;

    @Before
    public void setUp() throws Exception {

    }

    @Test
    @Ignore
    public void run() {
        List<DeletedMappingModels> all = deletedMappingModelsDao.findAll();
        System.out.println("All processing models info: " + all);
        processModelsTask.execute(null);
    }

    @Test
    @Ignore
    public void runBatch() {
        long start = Instant.now().toEpochMilli();

        Timestamp ts = Timestamp.from(Instant.now());
        long modelId = 1659727517L;
        long queueId = 1111L;
        List<DeletedMappingModels> batch = List.of(
            new DeletedMappingModels(modelId, CleanupStatus.READY_FOR_PROCESSING, ts, ts, queueId)
        );
        System.out.println("Processing batch: " + batch);

        processModelsTask.processBatch(batch, start);
    }

    @Test
    @Ignore
    public void readModelsTest() {
        Map<Long, ModelStorage.Model> modelsMap = modelStorageHelper.findModelsWithChildrenMap(Set.of(1659727517L));
    }
}
