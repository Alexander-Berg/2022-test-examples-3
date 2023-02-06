package ru.yandex.market.mbo.db.modelstorage.http;

import com.googlecode.protobuf.format.JsonFormat;
import org.hamcrest.CoreMatchers;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.common.util.log.LogWriter;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface.ModelStoreException;
import ru.yandex.market.mbo.db.modelstorage.http.utils.ModelLoaderUtils;
import ru.yandex.market.mbo.db.modelstorage.http.utils.ProtobufHelper;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageService;
import ru.yandex.market.mbo.user.AutoUser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author amaslak
 * @timestamp 9/27/15 7:10 PM
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:mbo-card-api/test-config.xml"})
@Ignore("MBO-13432")
public class ModelCardApiServiceTest {

    private static final Logger log = LoggerFactory.getLogger(ModelCardApiServiceTest.class);

    private static final DateUtil.ThreadLocalDateFormat SESSION_ID_FORMAT =
            new DateUtil.ThreadLocalDateFormat("yyyyMMdd_HHmm");

    private static final AtomicInteger SESSION_MINUTES = new AtomicInteger(-100);

    private static final int MAX_ROWS = 10000;
    private static final int SOURCE_CATEGORY_ID = 7920677;
    private static final int CATEGORY_ID = 42;
    private static final int VENDOR_ID = 132;

    @Autowired
    @Qualifier("masterModelStore")
    private ModelStoreInterface masterModelStore;

    @Autowired
    @Qualifier("modelStorageService")
    private ModelStorageService modelStorageService;

    @Autowired
    @Qualifier("modelCardApiService")
    private ModelCardApiServiceImpl modelCardApiService;

    @Autowired
    @Qualifier("autoUser")
    private AutoUser autoUser;

    @Value("${models.file.card.api}")
    private String modelsFilePath;

    private final List<Long> createdModelIds = new ArrayList<>();
    private final List<Long> createdNeverPublishedModelIds = new ArrayList<>();

    private final AtomicInteger fetchExistingModelsCounter = new AtomicInteger();
    private final AtomicInteger fetchCreatedModelsCounter = new AtomicInteger(3);
    private final AtomicInteger fetchCreatedNeverPublishedModelsCounter = new AtomicInteger(3);

    private List<ModelStorage.Model> existingModels;

    @Before
    public void prepare() throws Exception {
        existingModels = ModelLoaderUtils.loadModels(modelsFilePath).stream()
                .filter(m -> m.getCurrentType().equals(CommonModel.Source.CLUSTER.toString())
                        && m.getCategoryId() == SOURCE_CATEGORY_ID)
                .limit(MAX_ROWS)
                .collect(Collectors.toList());

        log.info(String.format("Loaded %d models from %d category", existingModels.size(), SOURCE_CATEGORY_ID));
    }

    /**
     * get new models, not existing in CATEGORY_ID.
     */
    private List<ModelStorage.Model> fetchModelsFromAnotherCategory(int numToFetch) {
        int counter = fetchExistingModelsCounter.getAndAdd(numToFetch);
        return existingModels.subList(counter, counter + numToFetch);
    }

    /**
     * get models, existing in CATEGORY_ID.
     */
    private List<ModelStorage.Model> fetchCreatedModels(int numToFetch) throws ModelStoreException {
        int counter = fetchCreatedModelsCounter.getAndAdd(numToFetch);
        List<Long> ids = createdModelIds.subList(counter, counter + numToFetch);
        List<ModelStorage.Model> models = masterModelStore.getModels(CATEGORY_ID, ids);
        Assert.assertEquals("Created models not found in YT category " + CATEGORY_ID, ids.size(), models.size());
        return models;
    }

    /**
     * get models with long IDs (never published), existing in CATEGORY_ID.
     */
    private List<ModelStorage.Model> fetchNeverPublishedModels(int numToFetch) throws ModelStoreException {
        int counter = fetchCreatedNeverPublishedModelsCounter.getAndAdd(numToFetch);
        List<Long> ids = createdNeverPublishedModelIds.subList(counter, counter + numToFetch);
        List<ModelStorage.Model> sourceModels = masterModelStore.getModels(CATEGORY_ID, ids);
        if (ids.size() != sourceModels.size()) {
            throw new AssertionError("created and never published models not found in YT category " + CATEGORY_ID);
        }
        return sourceModels;
    }

    @Test
    public void testMain() throws Exception {
        log.info(" ********** testDiffNew **********");
        doInSession(this::testDiffNew);
        log.info(" ********** testDiffOld **********");
        doInSession(this::testDiffOld);
        log.info(" ********** testDiffDivision **********");
        doInSession(this::testDiffDivision);
        log.info(" ********** testDiffUnion **********");
        doInSession(this::testDiffUnion);
    }

    @Test
    public void testApiV2() throws Exception {
        log.info(" ********** testDiffCreate **********");
        doInSession(this::testDiffCreate);
        log.info(" ********** testDiffUpdate **********");
        doInSession(this::testDiffUpdate);
        log.info(" ********** testDiffPublish **********");
        doInSession(this::testDiffPublish);
        log.info(" ********** testDiffDelete **********");
        doInSession(this::testDiffDelete);
    }

    @Test
    public void testApiV3() throws Exception {
        log.info(" ********** testDiffCreateV3 **********");
        doInSession(this::testDiffCreateV3);
        log.info(" ********** testDiffUpdateV3 **********");
        doInSession(this::testDiffUpdateV3);
        log.info(" ********** testDiffModificationTimeV3 **********");
        doInSession(this::testDiffModificationTimeV3);
        log.info(" ********** testDiffPublishV3 **********");
        doInSession(this::testDiffPublishV3);
    }

    private interface VoidCallback<T> {
        void execute(T t) throws IOException, ModelStoreException;
    }

    public void doInSession(VoidCallback<String> callback) throws IOException {
        String sessionId = getSession(0);
        String scSessionId = getSession(-3); // fake generation_data session is always 3 hours old

        modelCardApiService.openSession(ProtobufHelper.getSessionOpen(sessionId));
        modelCardApiService.openCategorySession(ProtobufHelper.getCategoryOpen(CATEGORY_ID, sessionId, scSessionId));

        modelCardApiService.updateCategorySessionStatus(ProtobufHelper.getSessionStatusUpdate(
                CATEGORY_ID, sessionId, ModelCardApi.CategorySessionStatus.STARTED_READING_MODELS
        ));

        modelCardApiService.updateCategorySessionStatus(ProtobufHelper.getSessionStatusUpdate(
                CATEGORY_ID, sessionId, ModelCardApi.CategorySessionStatus.FINISHED_READING_MODELS_SUCCESS
        ));

        modelCardApiService.updateCategorySessionStatus(ProtobufHelper.getSessionStatusUpdate(
                CATEGORY_ID, sessionId, ModelCardApi.CategorySessionStatus.STARTED_WRITING_DIFF
        ));
        try {
            callback.execute(sessionId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        modelCardApiService.updateCategorySessionStatus(ProtobufHelper.getSessionStatusUpdate(
                CATEGORY_ID, sessionId, ModelCardApi.CategorySessionStatus.FINISHED_WRITING_DIFF_SUCCESS
        ));

        modelCardApiService.closeCategorySession(ProtobufHelper.getCategoryClose(CATEGORY_ID, sessionId));
        modelCardApiService.closeSession(ProtobufHelper.getSessionClose(sessionId));
    }

    public void testDiffCreate(String sessionId) throws IOException, ModelStoreException {
        List<ModelCardApi.CreateOrUpdateModelDiff> createDiffs = fetchModelsFromAnotherCategory(30).stream()
                .map(m -> toCreateOrUpdateModelDiff(m, "CREATE"))
                .map(m -> m.build())
                .collect(Collectors.toList());

        AtomicLong i = new AtomicLong();
        fetchModelsFromAnotherCategory(10).stream()
                .map(m -> toCreateOrUpdateModelDiff(m, "CREATE_FAKE_ID"))
                .map(m -> m.setId(i.incrementAndGet()))
                .map(m -> m.build())
                .forEach(createDiffs::add);

        fetchModelsFromAnotherCategory(10).stream()
                .map(m -> toCreateOrUpdateModelDiff(m, "CREATE_PUBLISHED", true))
                .map(m -> m.setId(i.incrementAndGet()))
                .map(m -> m.build())
                .forEach(createDiffs::add);

        fetchModelsFromAnotherCategory(20).stream()
                .map(m -> toCreateOrUpdateModelDiff(m, "CREATE_NEVER_PUBLISHED", false))
                .map(m -> m.setId(i.incrementAndGet()))
                .map(m -> m.build())
                .forEach(createDiffs::add);

        ModelCardApi.ImportModelsResponse importModelsResponse = modelCardApiService.importModelsV2(
                ProtobufHelper.getImportV2(CATEGORY_ID, sessionId, createDiffs, new ArrayList<>(), new ArrayList<>(),
                        new ArrayList<>())
        );
        List<Long> modelIdsList = importModelsResponse.getModelIdsList();
        checkCreateResult(createDiffs, new ArrayList<>(), modelIdsList, new HashMap<>());

        createdModelIds.addAll(modelIdsList.subList(0, 50));
        createdNeverPublishedModelIds.addAll(modelIdsList.subList(50, 70));
    }

    public void testDiffCreateV3(String sessionId) throws IOException, ModelStoreException {
        List<ModelCardApi.CreateOrUpdateModelDiff> createDiffs = fetchModelsFromAnotherCategory(30).stream()
                .map(m -> toCreateOrUpdateModelDiff(m, "CREATE"))
                .map(m -> m.build())
                .collect(Collectors.toList());

        AtomicLong i = new AtomicLong();
        fetchModelsFromAnotherCategory(10).stream()
                .map(m -> toCreateOrUpdateModelDiff(m, "CREATE_FAKE_ID"))
                .map(m -> m.setId(i.incrementAndGet()))
                .map(m -> m.build())
                .forEach(createDiffs::add);

        fetchModelsFromAnotherCategory(10).stream()
                .map(m -> toCreateOrUpdateModelDiff(m, "CREATE_PUBLISHED", true))
                .map(m -> m.setId(i.incrementAndGet()))
                .map(m -> m.build())
                .forEach(createDiffs::add);

        fetchModelsFromAnotherCategory(20).stream()
                .map(m -> toCreateOrUpdateModelDiff(m, "CREATE_NEVER_PUBLISHED", false))
                .map(m -> m.setId(i.incrementAndGet()))
                .map(m -> m.build())
                .forEach(createDiffs::add);

        ModelCardApi.ImportModelsV3Response importModelsResponse = modelCardApiService.importModelsV3(
                ProtobufHelper.getImportV3(CATEGORY_ID, sessionId, createDiffs, new ArrayList<>(), new ArrayList<>())
        );
        List<Long> modelIdsList = importModelsResponse.getCreatedModelIdsList();
        Map<Long, Long> modelIdChanges = createModelIdChanges(importModelsResponse.getChangedModelIdsList());
        checkCreateResult(createDiffs, new ArrayList<>(), modelIdsList, modelIdChanges);

        createdModelIds.addAll(modelIdsList.subList(0, 50));
        createdNeverPublishedModelIds.addAll(modelIdsList.subList(50, 70));
    }

    public void testDiffUpdate(String sessionId) throws IOException, ModelStoreException {
        List<ModelStorage.Model> sourceModels = fetchCreatedModels(3);
        List<ModelStorage.Model> modelsBeforeUpdate = new ArrayList<>();

        // update models 5 to 10 with type OLD
        List<ModelCardApi.CreateOrUpdateModelDiff> modelDiffs = new ArrayList<>();
        for (ModelStorage.Model m : sourceModels) {
            modelsBeforeUpdate.add(m);
            ModelCardApi.CreateOrUpdateModelDiff.Builder b = toCreateOrUpdateModelDiff(m, "UPDATE");
            b.setId(m.getId());
            modelDiffs.add(b.build());
        }

        ModelCardApi.ImportModelsResponse importModelsResponse = modelCardApiService.importModelsV2(
                ProtobufHelper.getImportV2(CATEGORY_ID, sessionId, new ArrayList<>(), modelDiffs, new ArrayList<>(),
                        new ArrayList<>())
        );

        List<Long> modelIdsList = importModelsResponse.getModelIdsList();
        Map<Long, Long> modelIdChanges = createModelIdChanges(importModelsResponse.getChangedModelIdsList());
        Assert.assertTrue(createdModelIds.containsAll(modelIdsList));
        Assert.assertEquals(0, importModelsResponse.getFailedModelIdsCount());

        final List<ModelStorage.Model> newModels = checkCreateResult(
                new ArrayList<>(), modelDiffs, modelIdsList, modelIdChanges);
        Assert.assertEquals(modelsBeforeUpdate.size(), newModels.size());
        for (int i = 0; i < newModels.size(); i++) {
            ModelStorage.Model oldModel = modelsBeforeUpdate.get(i);
            ModelStorage.Model newModel = newModels.get(i);
            Assert.assertEquals(oldModel.getDeleted(), newModel.getDeleted());
            if (oldModel.getModifiedTs() != 0) {
                Assert.assertTrue(oldModel.getModifiedTs() < newModel.getModifiedTs());
            }
            Assert.assertEquals(autoUser.getId(), newModel.getModifiedUserId());
        }
    }

    public void testDiffPublish(String sessionId) throws IOException, ModelStoreException {
        List<ModelStorage.Model> toPublishTroughUpdateModels = fetchCreatedModels(3);
        List<ModelStorage.Model> toPublishTroughUpdateNeverPublishedModels = fetchNeverPublishedModels(3);
        List<ModelStorage.Model> toPublishModels = fetchCreatedModels(3);
        List<ModelStorage.Model> toPublishNeverPublishedModels = fetchNeverPublishedModels(3);
        List<ModelStorage.Model> modelsBeforeUpdate = new ArrayList<>();

        List<ModelCardApi.CreateOrUpdateModelDiff> updateModelDiffs = new ArrayList<>();
        for (ModelStorage.Model m : toPublishTroughUpdateModels) {
            modelsBeforeUpdate.add(m);
            ModelCardApi.CreateOrUpdateModelDiff.Builder b = toCreateOrUpdateModelDiff(m, "UPDATE", true);
            b.setId(m.getId());
            updateModelDiffs.add(b.build());
        }
        for (ModelStorage.Model m : toPublishTroughUpdateNeverPublishedModels) {
            modelsBeforeUpdate.add(m);
            ModelCardApi.CreateOrUpdateModelDiff.Builder b =
                    toCreateOrUpdateModelDiff(m, "UPDATE_NEVER_PUBLISHED", true);
            b.setId(m.getId());
            updateModelDiffs.add(b.build());
        }

        List<ModelCardApi.PublishModelDiff> publishModelDiffs = new ArrayList<>();
        for (ModelStorage.Model m : toPublishModels) {
            ModelCardApi.PublishModelDiff.Builder b = toPublishModelDiff(m, true);
            publishModelDiffs.add(b.build());
        }

        for (ModelStorage.Model m : toPublishNeverPublishedModels) {
            ModelCardApi.PublishModelDiff.Builder b = toPublishModelDiff(m, true);
            publishModelDiffs.add(b.build());
        }

        ModelCardApi.ImportModelsResponse importModelsResponse = modelCardApiService.importModelsV2(
                ProtobufHelper.getImportV2(CATEGORY_ID, sessionId, new ArrayList<>(), updateModelDiffs,
                        new ArrayList<>(), publishModelDiffs)
        );

        List<Long> modelIdsList = importModelsResponse.getModelIdsList();
        Map<Long, Long> modelIdChanges = createModelIdChanges(importModelsResponse.getChangedModelIdsList());
        Assert.assertTrue(createdModelIds.containsAll(modelIdsList));
        Assert.assertEquals(0, importModelsResponse.getFailedModelIdsCount());

        final List<ModelStorage.Model> newModels = checkCreateResult(
                new ArrayList<>(), updateModelDiffs, modelIdsList, modelIdChanges);
        Assert.assertEquals(modelsBeforeUpdate.size(), newModels.size());
        for (int i = 0; i < newModels.size(); i++) {
            ModelStorage.Model oldModel = modelsBeforeUpdate.get(i);
            ModelStorage.Model newModel = newModels.get(i);
            Assert.assertEquals(oldModel.getDeleted(), newModel.getDeleted());
            if (oldModel.getModifiedTs() != 0) {
                Assert.assertTrue(oldModel.getModifiedTs() < newModel.getModifiedTs());
            }
            Assert.assertEquals(autoUser.getId(), newModel.getModifiedUserId());
        }
        checkPublishResult(updateModelDiffs, publishModelDiffs, modelIdChanges);
    }

    public void testDiffUpdateV3(String sessionId) throws IOException, ModelStoreException {
        List<ModelStorage.Model> sourceModels = fetchCreatedModels(3);
        List<ModelStorage.Model> modelsBeforeUpdate = new ArrayList<>();

        // update models 5 to 10 with type OLD
        List<ModelCardApi.CreateOrUpdateModelDiff> modelDiffs = new ArrayList<>();
        for (ModelStorage.Model m : sourceModels) {
            modelsBeforeUpdate.add(m);
            ModelCardApi.CreateOrUpdateModelDiff.Builder b = toCreateOrUpdateModelDiff(m, "UPDATE");
            b.setId(m.getId());
            modelDiffs.add(b.build());
        }

        ModelCardApi.ImportModelsV3Response importModelsResponse = modelCardApiService.importModelsV3(
                ProtobufHelper.getImportV3(CATEGORY_ID, sessionId, new ArrayList<>(), modelDiffs, new ArrayList<>())
        );

        List<Long> modelIdsList = importModelsResponse.getCreatedModelIdsList();
        Map<Long, Long> modelIdChanges = createModelIdChanges(importModelsResponse.getChangedModelIdsList());
        Assert.assertTrue(createdModelIds.containsAll(modelIdsList));

        final List<ModelStorage.Model> newModels = checkCreateResult(
                new ArrayList<>(), modelDiffs, modelIdsList, modelIdChanges);
        Assert.assertEquals(modelsBeforeUpdate.size(), newModels.size());
        for (int i = 0; i < newModels.size(); i++) {
            ModelStorage.Model oldModel = modelsBeforeUpdate.get(i);
            ModelStorage.Model newModel = newModels.get(i);
            Assert.assertEquals(oldModel.getDeleted(), newModel.getDeleted());
            if (oldModel.getModifiedTs() != 0) {
                Assert.assertTrue(oldModel.getModifiedTs() < newModel.getModifiedTs());
            }
            Assert.assertEquals(autoUser.getId(), newModel.getModifiedUserId());
        }
    }

    public void testDiffModificationTimeV3(String sessionId) throws IOException, ModelStoreException {
        List<ModelStorage.Model> sourceModels = fetchCreatedModels(3);
        List<ModelStorage.Model> modelsBeforeUpdate = new ArrayList<>();

        List<ModelCardApi.CreateOrUpdateModelDiff> modelDiffs = new ArrayList<>();
        int z = 1;
        for (ModelStorage.Model m : sourceModels) {
            modelsBeforeUpdate.add(m);
            ModelCardApi.CreateOrUpdateModelDiff.Builder b = toCreateOrUpdateModelDiff(m, "UPDATE");
            b.setId(m.getId());
            b.setModificationTime(m.getModifiedTs() - z); // First will be failed and rest should be ok
            z = z / 2;
            modelDiffs.add(b.build());
        }

        ModelCardApi.ImportModelsV3Response importModelsResponse = modelCardApiService.importModelsV3(
                ProtobufHelper.getImportV3(CATEGORY_ID, sessionId, new ArrayList<>(), modelDiffs, new ArrayList<>())
        );

        List<Long> modelIdsList = importModelsResponse.getCreatedModelIdsList();
        List<Long> failed = importModelsResponse.getFailedModelIdsList();
        Map<Long, Long> modelIdChanges = createModelIdChanges(importModelsResponse.getChangedModelIdsList());
        Assert.assertTrue(failed.size() == 1);
        Assert.assertTrue(createdModelIds.containsAll(modelIdsList));

        List<ModelCardApi.CreateOrUpdateModelDiff> successDiffs =
                modelDiffs.stream().filter(d -> !failed.contains(d.getId())).collect(Collectors.toList());

        final List<ModelStorage.Model> newModels = checkCreateResult(
                new ArrayList<>(), successDiffs, modelIdsList, modelIdChanges);
        Assert.assertEquals(modelsBeforeUpdate.size(), newModels.size() + failed.size());
        for (int i = 0; i < newModels.size(); i++) {
            ModelStorage.Model oldModel = modelsBeforeUpdate.get(i);
            ModelStorage.Model newModel = newModels.get(i);
            Assert.assertEquals(oldModel.getDeleted(), newModel.getDeleted());
            if (oldModel.getModifiedTs() != 0) {
                Assert.assertTrue(oldModel.getModifiedTs() < newModel.getModifiedTs());
            }
            Assert.assertEquals(autoUser.getId(), newModel.getModifiedUserId());
        }
    }

    public void testDiffPublishV3(String sessionId) throws IOException, ModelStoreException {
        List<ModelStorage.Model> toPublishTroughUpdateModels = fetchCreatedModels(3);
        List<ModelStorage.Model> toPublishTroughUpdateNeverPublishedModels = fetchNeverPublishedModels(3);
        List<ModelStorage.Model> toPublishModels = fetchCreatedModels(3);
        List<ModelStorage.Model> toPublishNeverPublishedModels = fetchNeverPublishedModels(3);
        List<ModelStorage.Model> modelsBeforeUpdate = new ArrayList<>();

        List<ModelCardApi.CreateOrUpdateModelDiff> updateModelDiffs = new ArrayList<>();
        for (ModelStorage.Model m : toPublishTroughUpdateModels) {
            modelsBeforeUpdate.add(m);
            ModelCardApi.CreateOrUpdateModelDiff.Builder b = toCreateOrUpdateModelDiff(m, "UPDATE");
            b.setId(m.getId());
            updateModelDiffs.add(b.build());
        }
        for (ModelStorage.Model m : toPublishTroughUpdateNeverPublishedModels) {
            modelsBeforeUpdate.add(m);
            ModelCardApi.CreateOrUpdateModelDiff.Builder b =
                    toCreateOrUpdateModelDiff(m, "UPDATE_NEVER_PUBLISHED", true);
            b.setId(m.getId());
            updateModelDiffs.add(b.build());
        }

        List<ModelCardApi.PublishModelDiff> publishModelDiffs = new ArrayList<>();
        for (ModelStorage.Model m : toPublishModels) {
            ModelCardApi.PublishModelDiff.Builder b = toPublishModelDiff(m, true);
            publishModelDiffs.add(b.build());
        }
        for (ModelStorage.Model m : toPublishNeverPublishedModels) {
            ModelCardApi.PublishModelDiff.Builder b = toPublishModelDiff(m, true);
            publishModelDiffs.add(b.build());
        }

        ModelCardApi.ImportModelsV3Response importModelsResponse = modelCardApiService.importModelsV3(
                ProtobufHelper.getImportV3(CATEGORY_ID, sessionId, new ArrayList<>(), updateModelDiffs,
                        publishModelDiffs)
        );

        List<Long> modelIdsList = importModelsResponse.getCreatedModelIdsList();
        Map<Long, Long> modelIdChanges = createModelIdChanges(importModelsResponse.getChangedModelIdsList());
        Assert.assertTrue(createdModelIds.containsAll(modelIdsList));
        Assert.assertEquals(0, importModelsResponse.getFailedModelIdsCount());
        Assert.assertEquals(modelIdChanges.size(),
                toPublishTroughUpdateNeverPublishedModels.size() + toPublishNeverPublishedModels.size());

        final List<ModelStorage.Model> newModels = checkCreateResult(
                new ArrayList<>(), updateModelDiffs, modelIdsList, modelIdChanges);
        Assert.assertEquals(modelsBeforeUpdate.size(), newModels.size());
        for (int i = 0; i < newModels.size(); i++) {
            ModelStorage.Model oldModel = modelsBeforeUpdate.get(i);
            ModelStorage.Model newModel = newModels.get(i);
            Assert.assertEquals(oldModel.getDeleted(), newModel.getDeleted());
            if (oldModel.getModifiedTs() != 0) {
                Assert.assertTrue(oldModel.getModifiedTs() < newModel.getModifiedTs());
            }
            Assert.assertEquals(autoUser.getId(), newModel.getModifiedUserId());
        }
        checkPublishResult(updateModelDiffs, publishModelDiffs, modelIdChanges);
    }

    public void testDiffDelete(String sessionId) throws IOException, ModelStoreException {
        // create some models
        List<ModelCardApi.CreateOrUpdateModelDiff.Builder> createDiffs = fetchModelsFromAnotherCategory(5).stream()
                .map(m -> toCreateOrUpdateModelDiff(m, "CREATE"))
                .collect(Collectors.toList());

        AtomicLong fakeIds = new AtomicLong();
        List<ModelCardApi.CreateOrUpdateModelDiff.Builder> createFakeDiffs = fetchModelsFromAnotherCategory(6).stream()
                .map(m -> toCreateOrUpdateModelDiff(m, "CREATE_FAKE_ID").setId(fakeIds.incrementAndGet()))
                .collect(Collectors.toList());

        // update some models
        List<ModelCardApi.CreateOrUpdateModelDiff.Builder> updateDiffs = fetchCreatedModels(2).stream()
                .map(m -> toCreateOrUpdateModelDiff(m, "UPDATE").setId(m.getId()))
                .collect(Collectors.toList());

        fetchNeverPublishedModels(2).stream()
                .map(m -> toCreateOrUpdateModelDiff(m, "UPDATE_NEVER_PUBLISHED", true).setId(m.getId()))
                .forEach(updateDiffs::add);

        List<ModelStorage.Model> neverPublishedToPublish = fetchNeverPublishedModels(2);
        neverPublishedToPublish.stream()
                .map(m -> toCreateOrUpdateModelDiff(m, "UPDATE_NEVER_PUBLISHED", false).setId(m.getId()))
                .forEach(updateDiffs::add);

        // delete some models and make relations to created and updated models
        List<ModelCardApi.DeleteModelDiff.Builder> deleteDiffs = new ArrayList<>();
        List<ModelStorage.Model> neverPublishedAndDeleted = fetchNeverPublishedModels(2);
        List<ModelStorage.Model> deleteModels = CollectionUtils.join(fetchCreatedModels(4), neverPublishedAndDeleted);
        for (int i = 0; i < deleteModels.size(); i++) {
            ModelStorage.Model m = deleteModels.get(i);
            ModelCardApi.DeleteModelDiff.Builder b = ModelCardApi.DeleteModelDiff.newBuilder();
            b.setId(m.getId());
            deleteDiffs.add(b);

            addRelation(b, m, createFakeDiffs.get(i), true, i % 2 == 0, 0);
            addRelation(b, m, updateDiffs.get(i), false, i % 2 == 1, 1);
        }

        List<ModelCardApi.CreateOrUpdateModelDiff> create = CollectionUtils.join(createDiffs, createFakeDiffs).stream()
                .map(b -> b.build()).collect(Collectors.toList());
        List<ModelCardApi.CreateOrUpdateModelDiff> update = updateDiffs.stream()
                .map(b -> b.build()).collect(Collectors.toList());
        List<ModelCardApi.DeleteModelDiff> delete = deleteDiffs.stream()
                .map(b -> b.build()).collect(Collectors.toList());

        ModelCardApi.ImportModelsResponse importModelsResponse = modelCardApiService.importModelsV2(
                ProtobufHelper.getImportV2(CATEGORY_ID, sessionId, create, update, delete, new ArrayList<>())
        );

        List<Long> modelIdsList = importModelsResponse.getModelIdsList();
        Map<Long, Long> modelIdChanges = createModelIdChanges(importModelsResponse.getChangedModelIdsList());
        Assert.assertEquals(0, importModelsResponse.getFailedModelIdsCount());

        checkCreateResult(create, update, modelIdsList, modelIdChanges);
        checkDeleteResult(create, delete, modelIdsList, modelIdChanges);

        // Now let's publish the ones we have relations in our deleted categories
        // Need to check that relations are updated.
        List<ModelCardApi.PublishModelDiff> publishModelDiffs = new ArrayList<>();
        for (ModelStorage.Model m : neverPublishedToPublish) {
            ModelCardApi.PublishModelDiff.Builder b = toPublishModelDiff(m, true);
            publishModelDiffs.add(b.build());
        }

        importModelsResponse = modelCardApiService.importModelsV2(ProtobufHelper.getImportV2(CATEGORY_ID, sessionId,
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), publishModelDiffs)
        );
        Set<Long> oldIds = importModelsResponse.getChangedModelIdsList().stream()
                .map(changedId -> changedId.getPreviousId())
                .collect(Collectors.toSet());
        Assert.assertEquals(publishModelDiffs.size(), oldIds.size());
        checkRelationsUpdated(oldIds, neverPublishedAndDeleted);
    }

    private void addRelation(ModelCardApi.DeleteModelDiff.Builder delete,
                             ModelStorage.Model deleteModel,
                             ModelCardApi.CreateOrUpdateModelDiff.Builder sourse,
                             boolean fake, boolean strong, int offerNum) {

        ModelCardApi.DeleteModelDiff.Relation.Builder relation = ModelCardApi.DeleteModelDiff.Relation.newBuilder()
                .setOffersCount(1);
        if (fake) {
            relation.setCreateId(sourse.getId());
        } else {
            relation.setId(sourse.getId());
        }
        if (strong) {
            delete.setStrongRelation(relation);
        } else {
            delete.addRelations(relation);
        }

        sourse.addOffersIds(deleteModel.getClusterizerOfferIdsList().get(offerNum));
    }

    public void testDiffNew(String sessionId) throws IOException, ModelStoreException {
        List<ModelCardApi.ModelDiff> modelDiffs = fetchModelsFromAnotherCategory(50).stream()
                .map(m -> toModelDiff(m, ModelCardApi.ModelChangeType.NEW))
                .map(m -> m.build())
                .collect(Collectors.toList());

        ModelCardApi.ImportModelsResponse importModelsResponse = modelCardApiService.importModels(
                ProtobufHelper.importModelRequest(CATEGORY_ID, sessionId, modelDiffs)
        );
        List<Long> modelIdsList = importModelsResponse.getModelIdsList();
        checkImportResult(modelDiffs, modelIdsList);

        createdModelIds.addAll(modelIdsList);
    }

    public void testDiffOld(String sessionId) throws IOException, ModelStoreException {
        List<ModelStorage.Model> sourceModels = fetchCreatedModels(3);
        List<ModelStorage.Model> oldModels = new ArrayList<>();

        // update models 5 to 10 with type OLD
        List<ModelCardApi.ModelDiff> modelDiffs = new ArrayList<>();
        for (ModelStorage.Model m : sourceModels) {
            oldModels.add(m);
            ModelCardApi.ModelDiff.Builder b = toModelDiff(m, ModelCardApi.ModelChangeType.OLD);
            b.setId(m.getId());
            modelDiffs.add(b.build());
        }

        ModelCardApi.ImportModelsResponse importModelsResponse = modelCardApiService.importModels(
                ProtobufHelper.importModelRequest(CATEGORY_ID, sessionId, modelDiffs)
        );

        List<Long> modelIdsList = importModelsResponse.getModelIdsList();
        List<Long> sourceModelIdsList = sourceModels.stream().map(m -> m.getId()).collect(Collectors.toList());
        Assert.assertThat(modelIdsList, CoreMatchers.is(sourceModelIdsList));

        final List<ModelStorage.Model> newModels = checkImportResult(modelDiffs, modelIdsList);
        Assert.assertEquals(oldModels.size(), newModels.size());
        for (int i = 0; i < newModels.size(); i++) {
            ModelStorage.Model oldModel = oldModels.get(i);
            ModelStorage.Model newModel = newModels.get(i);
            Assert.assertEquals(oldModel.getDeleted(), newModel.getDeleted());
            if (oldModel.getModifiedTs() != 0) {
                Assert.assertEquals(oldModel.getModifiedTs(), newModel.getModifiedTs());
            }
            if (oldModel.getModifiedUserId() != 0) {
                Assert.assertEquals(oldModel.getModifiedUserId(), newModel.getModifiedUserId());
            }
        }
    }

    public void testDiffDivision(String sessionId) throws IOException, ModelStoreException {
        List<ModelStorage.Model> newModels = fetchCreatedModels(2);

        // slit 10 models by 3
        List<ModelCardApi.ModelDiff> modelDiffs = new ArrayList<>();
        for (ModelStorage.Model m : newModels) {
            ModelCardApi.ModelDiff.Builder b = toModelDiff(m, ModelCardApi.ModelChangeType.DIVISION);
            b.addParentIds(m.getId());
            List<String> offersIdsList = new ArrayList<>(b.getOffersIdsList());
            for (String offerId : offersIdsList) {
                b.clearOffersIds();
                b.addOffersIds(offerId);
                modelDiffs.add(b.build());
            }
        }

        ModelCardApi.ImportModelsResponse importModelsResponse = modelCardApiService.importModels(
                ProtobufHelper.importModelRequest(CATEGORY_ID, sessionId, modelDiffs)
        );

        List<Long> modelIdsList = importModelsResponse.getModelIdsList();
        List<ModelStorage.Model> models = checkImportResult(modelDiffs, modelIdsList);

        for (ModelStorage.Model model : models) {
            Assert.assertFalse(createdModelIds.contains(model.getId()));
            Assert.assertEquals(1, model.getRelationsCount());
            checkAncestors(model);
        }
    }

    public void testDiffUnion(String sessionId) throws IOException, ModelStoreException {
        List<ModelStorage.Model> subList = fetchCreatedModels(10);

        // slit 10 models by 3
        List<ModelCardApi.ModelDiff> modelDiffs = new ArrayList<>();
        for (int i = 0, subListSize = subList.size(); i < subListSize; i += 2) {
            ModelStorage.Model m1 = subList.get(i);
            ModelStorage.Model m2 = subList.get(i + 1);
            ModelCardApi.ModelDiff.Builder b = toModelDiff(m1, ModelCardApi.ModelChangeType.UNION);
            b.addParentIds(m1.getId());
            b.addParentIds(m2.getId());
            b.addAllOffersIds(m2.getClusterizerOfferIdsList());
            modelDiffs.add(b.build());
        }

        ModelCardApi.ImportModelsResponse importModelsResponse = modelCardApiService.importModels(
                ProtobufHelper.importModelRequest(CATEGORY_ID, sessionId, modelDiffs)
        );

        List<Long> modelIdsList = importModelsResponse.getModelIdsList();
        List<ModelStorage.Model> models = checkImportResult(modelDiffs, modelIdsList);

        for (ModelStorage.Model model : models) {
            Assert.assertTrue(!createdModelIds.contains(model.getId()));
            Assert.assertTrue(model.getRelationsCount() == 2);
            checkAncestors(model);
        }
    }

    private void checkAncestors(ModelStorage.Model model) throws ModelStoreException {
        for (ModelStorage.Relation relation : model.getRelationsList()) {
            if (relation.getType() == ModelStorage.RelationType.ANCESTOR) {
                Assert.assertTrue(createdModelIds.contains(relation.getId()));
                ModelStorage.Model ancestor = masterModelStore.getModelById(
                        CATEGORY_ID, relation.getId()
                );
                Assert.assertTrue(ancestor.getDeleted());
            }
        }
    }

    @NotNull
    private ModelCardApi.ModelDiff.Builder toModelDiff(ModelStorage.Model model,
                                                       ModelCardApi.ModelChangeType type
    ) {
        String s = "Тест_" + type.name() + "_" + new SimpleDateFormat("yyyyMMdd_HHMMSS").format(new Date()) + "_";

        String title = s + model.getTitles(0).getValue();
        String description = s + model.getDescriptions(0).getValue();

        return ModelCardApi.ModelDiff.newBuilder()
                .setChangeType(type)
                .setVendorId(VENDOR_ID)
                .setShopsCount(2)
                .setArticle("Eq NOMAE")
                .setTitle(ProtobufHelper.toLocalizedString(title))
                .setDescription(ProtobufHelper.toLocalizedString(description))
                .addOffersIds("0000000000000000")
                .addOffersIds("0000000000000001")
                .addOffersIds("0000000000000002")
                .addAllParameterValue(createParams(title, description, VENDOR_ID));
    }

    @NotNull
    private ModelCardApi.PublishModelDiff.Builder toPublishModelDiff(ModelStorage.Model model, boolean published) {
        return ModelCardApi.PublishModelDiff.newBuilder()
                .setId(model.getId())
                .setPublished(published);
    }

    @NotNull
    private ModelCardApi.CreateOrUpdateModelDiff.Builder toCreateOrUpdateModelDiff(ModelStorage.Model model, String msg,
                                                                                   boolean published) {
        return toCreateOrUpdateModelDiff(model, msg)
                .setPublished(published);
    }

    @NotNull
    private ModelCardApi.CreateOrUpdateModelDiff.Builder toCreateOrUpdateModelDiff(ModelStorage.Model model,
                                                                                   String msg) {
        String s = "Тест_" + msg + "_" + new SimpleDateFormat("yyyyMMdd_HHMMSS").format(new Date()) + "_";

        String title = s + model.getTitles(0).getValue();
        String description = s + model.getDescriptions(0).getValue();

        return ModelCardApi.CreateOrUpdateModelDiff.newBuilder()
                .setVendorId(VENDOR_ID)
                .setShopsCount(2)
                .setArticle("Eq NOMAE")
                .setTitle(ProtobufHelper.toLocalizedString(title))
                .setDescription(ProtobufHelper.toLocalizedString(description))
                .setModificationTime(model.getModifiedTs())
                .addOffersIds("0000000000000000")
                .addOffersIds("0000000000000001")
                .addOffersIds("0000000000000002")
                .addAllParameterValue(createParams(title, description, VENDOR_ID));
    }

    private List<ModelStorage.ParameterValue> createParams(String title, String description, long vendorId) {
        List<ModelStorage.ParameterValue> params = new ArrayList<>();
        params.add(ProtobufHelper.createParameter(
                ProtobufHelper.NAME_PARAM_ID, XslNames.NAME, System.currentTimeMillis(), autoUser.getId()).
                addStrValue(ProtobufHelper.toLocalizedString(title)).
                setTypeId(ModelStorage.ParameterValueType.STRING_VALUE).build());
        params.add(ProtobufHelper.createParameter(
                ProtobufHelper.DESCRIPTION_PARAM_ID, XslNames.DESCRIPTION, System.currentTimeMillis(),
                autoUser.getId()).
                addStrValue(ProtobufHelper.toLocalizedString(description)).
                setTypeId(ModelStorage.ParameterValueType.STRING_VALUE).build());
        params.add(ProtobufHelper.createParameter(
                ProtobufHelper.VENDOR_PARAM_ID, XslNames.VENDOR, System.currentTimeMillis(), autoUser.getId()).
                setOptionId((int) vendorId).
                setTypeId(ModelStorage.ParameterValueType.ENUM_VALUE).build());
        return params;
    }


    private List<ModelStorage.Model> checkDeleteResult(List<ModelCardApi.CreateOrUpdateModelDiff> createList,
                                                       List<ModelCardApi.DeleteModelDiff> deleteList,
                                                       List<Long> createIds, Map<Long, Long> modelIdChanges)
            throws ModelStoreException {

        List<Long> modelIds = new ArrayList<>(deleteList.size());
        deleteList.stream().map(m -> m.getId()).map(id -> modelIdChanges.getOrDefault(id, id)).
                forEachOrdered(modelIds::add);

        List<ModelStorage.Model> allModels = masterModelStore.getModels(CATEGORY_ID, modelIds);

        Set<Long> foundIds = new HashSet<>();
        masterModelStore.processCategoryModels(CATEGORY_ID, CommonModel.Source.CLUSTER, true,
                /*limit*/ null, m -> {
                    final long id = m.getId();
                    if (modelIds.contains(id)) {
                        foundIds.add(id);
                        ModelCardApi.DeleteModelDiff diff = deleteList.get(modelIds.indexOf(id));
                        Assert.assertTrue(m.getDeleted());
                    }
                });

        Assert.assertEquals(foundIds, new HashSet<>(modelIds));
        return allModels;
    }

    private void checkRelationsUpdated(Set<Long> oldIds,
                                       List<ModelStorage.Model> deletedModels) throws ModelStoreException {

        List<Long> modelIds = new ArrayList<>(deletedModels.size());
        deletedModels.stream().map(m -> m.getId()).forEachOrdered(modelIds::add);

        List<ModelStorage.Model> allModels = masterModelStore.getModels(CATEGORY_ID, modelIds);

        for (ModelStorage.Model model : allModels) {
            boolean foundOldId = false;
            for (ModelStorage.Relation relation : model.getRelationsList()) {
                if (oldIds.contains(relation.getId())) {
                    foundOldId = true;
                }
            }
            Assert.assertFalse(foundOldId);
        }
    }

    private List<ModelStorage.Model> checkCreateResult(List<ModelCardApi.CreateOrUpdateModelDiff> createList,
                                                       List<ModelCardApi.CreateOrUpdateModelDiff> updateList,
                                                       List<Long> createIds, Map<Long, Long> changedIdsMap)
            throws IOException, ModelStoreException {

        LogWriter writer = LogWriter.info(log);
        Assert.assertEquals(createList.size(), createIds.size());

        List<ModelStorage.Model> newModels = masterModelStore.getModels(CATEGORY_ID, createIds);
        Assert.assertEquals(newModels.size(), createIds.size());

        Set<Long> createdModels = new HashSet<>(createIds);

        List<Long> modelIds = new ArrayList<>(createIds);
        updateList.stream().map(m -> changedIdsMap.getOrDefault(m.getId(), m.getId()))
                .forEachOrdered(modelIds::add);

        List<ModelStorage.Model> allModels = masterModelStore.getModels(CATEGORY_ID, modelIds);

        List<ModelCardApi.CreateOrUpdateModelDiff> diffs = new ArrayList<>(createList);
        diffs.addAll(updateList);

        for (Map.Entry<Long, Long> changedId : changedIdsMap.entrySet()) {
            Assert.assertFalse(isPublishedId(changedId.getKey()));
            Assert.assertTrue(isPublishedId(changedId.getValue()));
        }

        Set<Long> foundIds = new HashSet<>();
        masterModelStore.processCategoryModels(CATEGORY_ID, CommonModel.Source.CLUSTER, false,
                /*limit*/ null, m -> {
                    final long id = m.getId();
                    if (modelIds.contains(id)) {
                        foundIds.add(id);
                        ModelCardApi.CreateOrUpdateModelDiff diff = diffs.get(modelIds.indexOf(id));
                        CommonModel commonModel = ModelProtoConverter.convert(m);

                        Assert.assertNotNull(commonModel);
                        Assert.assertEquals(commonModel.getDescription(), diff.getDescription().getValue());
                    }
                });

        Assert.assertEquals(foundIds, new HashSet<>(modelIds));

        for (int i = 0; i < diffs.size(); i++) {
            ModelCardApi.CreateOrUpdateModelDiff diff = diffs.get(i);
            log.info("==== diff ====");
            JsonFormat.print(diff, writer);
            writer.flush();

            log.info("==== id ====");
            long id = modelIds.get(i);

            ModelStorage.Model newModel = allModels.get(i);
            log.info("==== model ====");
            JsonFormat.print(newModel, writer);
            writer.flush();

            ModelStorage.GetModelsResponse getModelsResponse = modelStorageService.getModels(
                    ProtobufHelper.getModelsRequestBuilder(CATEGORY_ID, id).build());

            log.info("Found model in YT ");
            JsonFormat.print(getModelsResponse, writer);
            writer.flush();
            ModelStorage.Model model = getModelsResponse.getModels(0);
            Assert.assertEquals(model.getId(), id);
            Assert.assertEquals(model.getTitles(0), diff.getTitle());
            Assert.assertEquals(model.getArticle(), diff.getArticle());
            Assert.assertEquals(model.getShopCount(), diff.getShopsCount());
            Assert.assertEquals(model.getDoubtful(), diff.getDoubtful());
            Assert.assertEquals(model.getVendorId(), diff.getVendorId());
            Assert.assertEquals(
                    model.getBarcodesList().stream().map(String::valueOf).
                            collect(Collectors.toCollection(HashSet::new)),
                    new HashSet<>(diff.getBarcodesList()));

            Assert.assertEquals(model.getDescriptions(0), diff.getDescription());
            Assert.assertEquals(new HashSet<>(model.getClusterizerOfferIdsList()),
                    new HashSet<>(diff.getOffersIdsList()));
            if (diff.hasPublished()) {
                if (diff.getPublished()) {
                    Assert.assertTrue(isPublishedId(model.getId()));
                } else {
                    if (createdModels.contains(diff.getId())) {
                        Assert.assertTrue(!isPublishedId(model.getId()));
                    }
                }
                Assert.assertEquals(model.getPublished(), diff.getPublished());
            }
            // Just make sure that existing models have correct IDs.
            // we don't know if unpublished has short ID or not.
            if (!model.hasPublished() || model.getPublished()) {
                Assert.assertTrue(isPublishedId(model.getId()));
            }
        }

        return allModels;
    }

    private void checkPublishResult(List<ModelCardApi.CreateOrUpdateModelDiff> updateList,
                                    List<ModelCardApi.PublishModelDiff> publishList,
                                    Map<Long, Long> modelIdChanges) throws ModelStoreException {
        List<Pair<Long, Boolean>> changes = new ArrayList<>(updateList.stream().
                map(m -> new Pair<>(modelIdChanges.getOrDefault(m.getId(), m.getId()), m.getPublished()))
                .collect(Collectors.toList()));
        publishList.stream().map(m -> new Pair<>(modelIdChanges.getOrDefault(m.getId(), m.getId()), m.getPublished()))
                .forEachOrdered(changes::add);

        Map<Long, ModelStorage.Model> allModels = masterModelStore.getModels(CATEGORY_ID,
                changes.stream().map(Pair::getFirst).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(ModelStorage.Model::getId, Function.identity()));

        for (Pair<Long, Boolean> change : changes) {
            ModelStorage.Model m = allModels.get(change.getFirst());
            Assert.assertEquals(change.getSecond(), m.getPublished());
            if (change.getSecond()) {
                Assert.assertTrue(isPublishedId(m.getId()));
            }
        }
    }

    private List<ModelStorage.Model> checkImportResult(List<ModelCardApi.ModelDiff> models,
                                                       List<Long> result) throws IOException, ModelStoreException {
        LogWriter writer = LogWriter.info(log);
        Assert.assertEquals(models.size(), result.size());

        List<ModelStorage.Model> newModels = masterModelStore.getModels(CATEGORY_ID, result);
        List<Long> newModelsIds = newModels.stream().map(ModelStorage.Model::getId).collect(Collectors.toList());
        Assert.assertThat(newModelsIds, CoreMatchers.is(result));

        Set<Long> foundIds = new HashSet<>();
        masterModelStore.processCategoryModels(CATEGORY_ID, CommonModel.Source.CLUSTER, false,
                /*limit*/ null, m -> {
                    final long id = m.getId();
                    if (result.contains(id)) {
                        foundIds.add(id);
                        ModelCardApi.ModelDiff diff = models.get(result.indexOf(id));
                        CommonModel commonModel = ModelProtoConverter.convert(m);

                        Assert.assertNotNull(commonModel);
                        Assert.assertEquals(commonModel.getDescription(), diff.getDescription().getValue());
                    }
                });

        Assert.assertEquals(foundIds, new HashSet<>(result));

        for (int i = 0; i < models.size(); i++) {
            ModelCardApi.ModelDiff diff = models.get(i);
            log.info("==== diff ====");
            JsonFormat.print(diff, writer);
            writer.flush();

            log.info("==== id ====");
            long id = result.get(i);

            ModelStorage.Model newModel = newModels.get(i);
            log.info("==== model ====");
            JsonFormat.print(newModel, writer);
            writer.flush();

            ModelStorage.GetModelsResponse getModelsResponse = modelStorageService.getModels(
                    ModelStorage.GetModelsRequest.newBuilder()
                            .setCategoryId(CATEGORY_ID)
                            .addModelIds(id)
                            .build()
            );

            log.info("Found model in YT ");
            JsonFormat.print(getModelsResponse, writer);
            writer.flush();
            ModelStorage.Model model = getModelsResponse.getModels(0);
            Assert.assertEquals(model.getId(), id);
            Assert.assertEquals(model.getTitles(0), diff.getTitle());
            Assert.assertEquals(model.getArticle(), diff.getArticle());
            Assert.assertEquals(model.getShopCount(), diff.getShopsCount());
            Assert.assertEquals(model.getDoubtful(), diff.getDoubtful());
            Assert.assertEquals(model.getVendorId(), diff.getVendorId());
            Assert.assertEquals(
                    model.getBarcodesList().stream().map(String::valueOf).
                            collect(Collectors.toCollection(HashSet::new)),
                    new HashSet<>(diff.getBarcodesList()));

            Assert.assertEquals(model.getDescriptions(0), diff.getDescription());
            Assert.assertEquals(new HashSet<>(model.getClusterizerOfferIdsList()),
                    new HashSet<>(diff.getOffersIdsList()));

        }

        return newModels;
    }

    private Map<Long, Long> createModelIdChanges(List<ModelCardApi.ModelIdChange> changedModelIdsList) {
        return changedModelIdsList.stream()
                .collect(Collectors.toMap(
                        ModelCardApi.ModelIdChange::getPreviousId, ModelCardApi.ModelIdChange::getNewId));
    }

    private boolean isPublishedId(Long id) {
        return id < ModelStoreInterface.GENERATED_ID_MIN_VALUE;
    }

    public static String getSession(int addHours) {
        return SESSION_ID_FORMAT.format(new Date(
                System.currentTimeMillis()
                        + TimeUnit.HOURS.toMillis(addHours)
                        + TimeUnit.MINUTES.toMillis(SESSION_MINUTES.incrementAndGet())
        ));
    }
}
