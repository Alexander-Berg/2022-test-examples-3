package ru.yandex.canvas.service;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.CreativeDocumentBatch;
import ru.yandex.canvas.model.CreativeDocumentBatches;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.canvas.steps.CreativeDocumentBatchSteps.createCreativeDocumentBatch;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CreativesServiceDbQueryTest {
    private static final String CREATIVE_BATCH_COLLECTION = "creative_batch";
    private static final CompareStrategy BATCH_COMPARE_STRATEGY = allFieldsExcept(
            newPath("items/\\d+/date/time"),
            newPath("items/\\d+/previewURL"),
            newPath("items/\\d+/clientId")
    );

    @Autowired
    CreativesService service;

    @Autowired
    PreviewUrlBuilder previewUrlBuilder;

    @Autowired
    MongoOperations mongoOperations;

    private long clientId;

    @Before
    public void setUp() {
        clientId = new Random().nextLong();
    }

    @Test
    public void createBatchTest() {
        CreativeDocumentBatch batch = createCreativeDocumentBatch(clientId);

        CreativeDocumentBatch result = service.createBatch(batch, clientId);

        assertThat(result.getItems().get(0).getCreativeURL()).isNotNull();
        assertThat(result.getItems().get(0).getPreviewURL()).isNotNull();
        assertThat(result.getItems().get(0).getScreenshotURL()).isNotNull();
        assertThat(result.getItems().get(0).getId()).isNotEqualTo(0L);

        Query query = new Query().addCriteria(Criteria.where("_id").is(batch.getId()));
        List<CreativeDocumentBatch> inDb = mongoOperations.find(query, CreativeDocumentBatch.class,
                CREATIVE_BATCH_COLLECTION);
        assertThat(inDb.size()).isEqualTo(1);
    }

    @Test
    public void getBatchTest() {
        CreativeDocumentBatch batch = createCreativeDocumentBatch(clientId);
        mongoOperations.insert(batch, CREATIVE_BATCH_COLLECTION);

        CreativeDocumentBatch result = service.getBatch(batch.getId(), clientId);

        assertThat(result).is(matchedBy(beanDiffer(batch)
                .useCompareStrategy(BATCH_COMPARE_STRATEGY)));
    }

    @Test
    public void updateBatchNameTest() {
        CreativeDocumentBatch batch = createCreativeDocumentBatch(clientId);
        mongoOperations.insert(batch, CREATIVE_BATCH_COLLECTION);

        String newBatchName = "newBatchName";
        batch.withName(newBatchName);
        service.updateBatchName(batch, batch.getId(), clientId);

        batch.getItems().forEach(item -> {
            item.setName(newBatchName);
        });
        CreativeDocumentBatch result = service.getBatch(batch.getId(), clientId);

        assertThat(result).is(matchedBy(beanDiffer(batch)
                .useCompareStrategy(BATCH_COMPARE_STRATEGY)));
    }

    @Test
    public void getBatchesTest_filterByPresetId() {
        CreativeDocumentBatch batchToReturn = createCreativeDocumentBatch(clientId);
        batchToReturn.getItems().forEach(item -> item.withPresetId(10));
        CreativeDocumentBatch batchToIgnore = createCreativeDocumentBatch(clientId);
        batchToIgnore.getItems().forEach(item -> item.withPresetId(20));

        mongoOperations.insert(Arrays.asList(batchToReturn, batchToIgnore), CREATIVE_BATCH_COLLECTION);

        CreativeDocumentBatches result = service
                .getBatches(clientId, 0, 10, emptySet(), emptySet(), "date", true, singleton(10));

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getItems().get(0)).is(matchedBy(beanDiffer(batchToReturn)
                .useCompareStrategy(BATCH_COMPARE_STRATEGY)));
    }

    @Test
    public void getBatchesTest_filterByPresetId_emptyAllowed() {
        CreativeDocumentBatch batchToReturn = createCreativeDocumentBatch(clientId);
        batchToReturn.getItems().forEach(item -> item.withPresetId(null));
        CreativeDocumentBatch batchToIgnore = createCreativeDocumentBatch(clientId);
        batchToIgnore.getItems().forEach(item -> item.withPresetId(20));

        mongoOperations.insert(Arrays.asList(batchToReturn, batchToIgnore), CREATIVE_BATCH_COLLECTION);

        CreativeDocumentBatches result = service
                .getBatches(clientId, 0, 10, emptySet(), emptySet(), "date", true, Arrays.asList(10, null));

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getItems().get(0)).is(matchedBy(beanDiffer(batchToReturn)
                .useCompareStrategy(BATCH_COMPARE_STRATEGY)));
    }

    @Test
    public void getBatchesTest_filterByPresetId_multipleIds() {
        CreativeDocumentBatch batchToReturn = createCreativeDocumentBatch(clientId, "1_first");
        batchToReturn.getItems().forEach(item -> item.withPresetId(10));
        CreativeDocumentBatch secondBatchToReturn = createCreativeDocumentBatch(clientId, "2_second");
        secondBatchToReturn.getItems().forEach(item -> item.withPresetId(20));

        mongoOperations.insert(batchToReturn, CREATIVE_BATCH_COLLECTION);
        mongoOperations.insert(secondBatchToReturn, CREATIVE_BATCH_COLLECTION);

        CreativeDocumentBatches result = service
                .getBatches(clientId, 0, 10, emptySet(), emptySet(), "name", false, Arrays.asList(10, 20));

        assertThat(result.getTotal()).isEqualTo(2L);
        assertThat(result.getItems().get(0)).is(matchedBy(beanDiffer(batchToReturn)
                .useCompareStrategy(BATCH_COMPARE_STRATEGY)));
        assertThat(result.getItems().get(1)).is(matchedBy(beanDiffer(secondBatchToReturn)
                .useCompareStrategy(BATCH_COMPARE_STRATEGY)));
    }
}
