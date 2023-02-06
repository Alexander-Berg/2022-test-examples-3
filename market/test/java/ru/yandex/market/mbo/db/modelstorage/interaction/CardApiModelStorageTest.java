package ru.yandex.market.mbo.db.modelstorage.interaction;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface.ModelStoreException;
import ru.yandex.market.mbo.db.modelstorage.interaction.data.ImportModelsStats;
import ru.yandex.market.mbo.db.modelstorage.merge.ModelMergeService;
import ru.yandex.market.mbo.db.modelstorage.stubs.ClusterTransitionsServiceStub;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelMergeServiceStub;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStoreInterfaceStub;
import ru.yandex.market.mbo.db.modelstorage.stubs.UpdateModelIndexInterfaceStub;
import ru.yandex.market.mbo.db.modelstorage.transitions.ClusterTransitionsService;
import ru.yandex.market.mbo.export.MboParameters.ValueType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelCardApi.CreateOrUpdateModelDiff;
import ru.yandex.market.mbo.http.ModelCardApi.PublishModelDiff;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.LocalizedString;
import ru.yandex.market.mbo.http.ModelStorage.ParameterValue;
import ru.yandex.market.mbo.user.AutoUser;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author moskovkin@yandex-team.ru
 * @since 26.04.18
 */
@RunWith(MockitoJUnitRunner.Silent.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class CardApiModelStorageTest {
    private CardApiModelStorage cardApiModelStorage;
    private ModelStoreInterface modelStore;
    private ClusterTransitionsService clusterTransitionsService;
    private ModelMergeService modelMergeService;
    private UpdateModelIndexInterfaceStub updateModelIndexInterface;
    private ModelStorageServiceStub modelStorageService;

    @Before
    public void init() {
        modelStorageService = new ModelStorageServiceStub();
        modelStore = new ModelStoreInterfaceStub(modelStorageService);
        clusterTransitionsService = new ClusterTransitionsServiceStub();
        modelMergeService = new ModelMergeServiceStub(null);
        updateModelIndexInterface = new UpdateModelIndexInterfaceStub();
        AutoUser user = new AutoUser(42);

        cardApiModelStorage = new CardApiModelStorage(
            modelStore,
            clusterTransitionsService,
            user,
            modelMergeService,
            updateModelIndexInterface
        );
    }

    private ModelStorage.Model createUnpublishedCluster(long categoryId, long id) {
        return ModelStorage.Model.newBuilder()
            .setCategoryId(categoryId)
            .setId(id)
            .setPublished(false)
            .setVendorId(42)
            .setSourceType(CommonModel.Source.CLUSTER.name())
            .setCurrentType(CommonModel.Source.CLUSTER.name())
            .build();
    }

    @Test
    public void testSimpleCreateV3() {
        long modelId1 = 12L;
        long modelId2 = 15L;
        long modelId3 = ModelStoreInterface.GENERATED_ID_MIN_VALUE + 10000L;
        List<CreateOrUpdateModelDiff> createDiffs = Arrays.asList(
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId1)
                .setPublished(false)
                .setTitle(loc("Meow"))
                .addParameterValue(title("Meow"))
                .build(),
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId2)
                .setPublished(true)
                .setTitle(loc("Woof"))
                .addParameterValue(title("Woof"))
                .build(),
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId3)
                .setPublished(true)
                .setTitle(loc("Moo"))
                .addParameterValue(title("Moo"))
                .build()
        );
        ImportModelsStats stats = cardApiModelStorage.applyImportV3(100500L, System.currentTimeMillis(),
            createDiffs, Collections.emptyList(), Collections.emptyList());

        // Т.к. IDшники могут меняться по смыслу ручки, будем для удобства идентифицировать модельки по тайтлам в тесте
        Map<String, CommonModel> allModels = byTitle(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(3);
        Assertions.assertThat(allModels.get("Meow").isPublished()).isFalse();
        Assertions.assertThat(allModels.get("Woof").isPublished()).isTrue();
        Assertions.assertThat(allModels.get("Moo").isPublished()).isTrue();
        Assertions.assertThat(allModels.get("Meow").getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);
        Assertions.assertThat(allModels.get("Woof").getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);
        Assertions.assertThat(allModels.get("Moo").getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);

        Assertions.assertThat(stats.getChangedIds()).isEmpty();
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEqualTo(ImmutableMap.of(
            modelId1, allModels.get("Meow").getId(),
            modelId2, allModels.get("Woof").getId(),
            modelId3, allModels.get("Moo").getId()
        ));
        Assertions.assertThat(stats.getCreatedIds()).containsExactlyInAnyOrder(
            allModels.get("Meow").getId(),
            allModels.get("Woof").getId(),
            allModels.get("Moo").getId()
        );
    }

    @Test
    public void testSimpleUpdateV3() throws ModelStoreException {
        long vendorMeowBefore = 123L;
        long vendorMeowAfter = 1234L;

        long vendorMooBefore = 567L;
        long vendorMooAfter = 5678L;

        long modelId1 = 10L;
        long modelId2 = ModelStoreInterface.GENERATED_ID_MIN_VALUE + 100L;

        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(100500L, modelId1).toBuilder()
                .addParameterValues(title("Meow"))
                .addParameterValues(vendor(vendorMeowBefore))
                .setPublished(true)
                .build(),
            createUnpublishedCluster(100500L, modelId2).toBuilder()
                .addParameterValues(title("Moo"))
                .addParameterValues(vendor(vendorMooBefore))
                .build()
        );
        modelStore.saveClusters(clusters);

        // Поменяем вендора. Обратите внимание, что флажок публикации не указан - он триггерит специальную логику,
        // поэтому с флажком будет отдельный тест-кейс.
        List<CreateOrUpdateModelDiff> updateDiffs = Arrays.asList(
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId1)
                .setTitle(loc("Meow"))
                .addParameterValue(title("Meow"))
                .setVendorId(vendorMeowAfter)
                .addParameterValue(vendor(vendorMeowAfter))
                .build(),
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId2)
                .setTitle(loc("Moo"))
                .addParameterValue(title("Moo"))
                .setVendorId(vendorMooAfter)
                .addParameterValue(vendor(vendorMooAfter))
                .build()
        );

        ImportModelsStats stats = cardApiModelStorage.applyImportV3(100500L, System.currentTimeMillis(),
            Collections.emptyList(), updateDiffs, Collections.emptyList());

        Map<String, CommonModel> allModels = byTitle(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(2);
        Assertions.assertThat(allModels.get("Meow").isPublished()).isTrue();
        Assertions.assertThat(allModels.get("Moo").isPublished()).isFalse();
        Assertions.assertThat(allModels.get("Meow").getId()).isEqualTo(modelId1);
        Assertions.assertThat(allModels.get("Moo").getId()).isEqualTo(modelId2);
        Assertions.assertThat(allModels.get("Meow").getVendorId()).isEqualTo(vendorMeowAfter);
        Assertions.assertThat(allModels.get("Moo").getVendorId()).isEqualTo(vendorMooAfter);

        Assertions.assertThat(stats.getCreatedIds()).isEmpty();
        Assertions.assertThat(stats.getChangedIds()).isEmpty();
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEmpty();
    }

    @Test
    public void testPubdateV3() throws ModelStoreException {
        long vendorMeowBefore = 123L;
        long vendorMeowAfter = 1234L;

        long vendorMooBefore = 567L;
        long vendorMooAfter = 5678L;

        long modelId1 = 10L;
        long modelId2 = ModelStoreInterface.GENERATED_ID_MIN_VALUE + 100L;

        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(100500L, modelId1).toBuilder()
                .addParameterValues(title("Meow"))
                .addParameterValues(vendor(vendorMeowBefore))
                .build(),
            createUnpublishedCluster(100500L, modelId2).toBuilder()
                .addParameterValues(title("Moo"))
                .addParameterValues(vendor(vendorMooBefore))
                .build()
        );
        modelStore.saveClusters(clusters);

        // Поменяем вендора. Обратите внимание, что флажок публикации указан - он триггерит специальную логику,
        // которая попытается опубликовать кластеры. При этом если у кластера лонговый ИДшник, то такой кластер будет
        // удалён, а на его основе создастся опубликованная копия с коротким ИД.
        List<CreateOrUpdateModelDiff> updateDiffs = Arrays.asList(
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId1)
                .setTitle(loc("Meow"))
                .addParameterValue(title("Meow"))
                .setVendorId(vendorMeowAfter)
                .addParameterValue(vendor(vendorMeowAfter))
                .setPublished(true)
                .build(),
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId2)
                .setTitle(loc("Moo"))
                .addParameterValue(title("Moo"))
                .setVendorId(vendorMooAfter)
                .addParameterValue(vendor(vendorMooAfter))
                .setPublished(true)
                .build()
        );

        ImportModelsStats stats = cardApiModelStorage.applyImportV3(100500L, System.currentTimeMillis(),
            Collections.emptyList(), updateDiffs, Collections.emptyList());

        // Для Moo-модели будет две копии - старая с невалидным ИДшником, которая будет удалена, и новая с правильным
        // коротким ИД, опубликованная и живая. Поэтому сгруппируем по тайтлу в мультимапу.
        Map<String, List<CommonModel>> allModels = byTitleMulti(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(2);
        Assertions.assertThat(allModels.get("Meow")).hasSize(1);
        Assertions.assertThat(allModels.get("Moo")).hasSize(2);

        CommonModel meowPubdated = allModels.get("Meow").get(0);
        CommonModel mooDeleted = allModels.get("Moo").stream().filter(CommonModel::isDeleted).findAny().get();
        CommonModel mooPubdated = allModels.get("Moo").stream().filter(m -> !m.isDeleted()).findAny().get();

        Assertions.assertThat(meowPubdated.isPublished()).isTrue();
        Assertions.assertThat(mooPubdated.isPublished()).isTrue();
        Assertions.assertThat(meowPubdated.getId()).isEqualTo(modelId1);
        Assertions.assertThat(mooPubdated.getId()).isNotEqualTo(modelId2);
        Assertions.assertThat(mooPubdated.getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);
        Assertions.assertThat(meowPubdated.getVendorId()).isEqualTo(vendorMeowAfter);
        Assertions.assertThat(mooPubdated.getVendorId()).isEqualTo(vendorMooAfter);

        // Старая версия Моо модели с длинным ИД теперь удалена. Больше нам про неё ничего не интересно знать.
        Assertions.assertThat(mooDeleted.isDeleted()).isTrue();

        Assertions.assertThat(stats.getCreatedIds()).isEmpty();
        Assertions.assertThat(stats.getChangedIds())
            .containsOnlyKeys(modelId2)
            .containsValues(mooPubdated.getId());
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEmpty();
    }

    @Test
    public void testSimplePublishV3() throws ModelStoreException {
        long modelId1 = 10L;
        long modelId2 = ModelStoreInterface.GENERATED_ID_MIN_VALUE + 100L;

        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(100500L, modelId1).toBuilder()
                .addParameterValues(title("Meow"))
                .build(),
            createUnpublishedCluster(100500L, modelId2).toBuilder()
                .addParameterValues(title("Moo"))
                .build()
        );
        modelStore.saveClusters(clusters);

        // Публикуем кластеры. При этом если у кластера лонговый ИДшник, то такой кластер будет удалён, а на его основе
        // создастся опубликованная копия с коротким ИД.
        List<PublishModelDiff> publishDiffs = Arrays.asList(
            PublishModelDiff.newBuilder()
                .setId(modelId1)
                .setPublished(true)
                .build(),
            PublishModelDiff.newBuilder()
                .setId(modelId2)
                .setPublished(true)
                .build()
        );

        ImportModelsStats stats = cardApiModelStorage.applyImportV3(100500L, System.currentTimeMillis(),
            Collections.emptyList(), Collections.emptyList(), publishDiffs);

        // Для Moo-модели будет две копии - старая с невалидным ИДшником, которая будет удалена, и новая с правильным
        // коротким ИД, опубликованная и живая. Поэтому сгруппируем по тайтлу в мультимапу.
        Map<String, List<CommonModel>> allModels = byTitleMulti(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(2);
        Assertions.assertThat(allModels.get("Meow")).hasSize(1);
        Assertions.assertThat(allModels.get("Moo")).hasSize(2);

        CommonModel meowPublished = allModels.get("Meow").get(0);
        CommonModel mooDeleted = allModels.get("Moo").stream().filter(CommonModel::isDeleted).findAny().get();
        CommonModel mooPublished = allModels.get("Moo").stream().filter(m -> !m.isDeleted()).findAny().get();

        Assertions.assertThat(meowPublished.isPublished()).isTrue();
        Assertions.assertThat(mooPublished.isPublished()).isTrue();
        Assertions.assertThat(meowPublished.getId()).isEqualTo(modelId1);
        Assertions.assertThat(mooPublished.getId()).isNotEqualTo(modelId2);
        Assertions.assertThat(mooPublished.getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);

        // Старая версия Моо модели с длинным ИД теперь удалена. Больше нам про неё ничего не интересно знать.
        Assertions.assertThat(mooDeleted.isDeleted()).isTrue();

        Assertions.assertThat(stats.getCreatedIds()).isEmpty();
        Assertions.assertThat(stats.getChangedIds())
            .containsOnlyKeys(modelId2)
            .containsValues(mooPublished.getId());
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEmpty();
    }

    @Test
    public void testPublishAlreadyPublishedV3() throws ModelStoreException {
        long modelId1 = 10L;
        long modelId2 = 11L;

        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(100500L, modelId1).toBuilder()
                .addParameterValues(title("Meow"))
                .setPublished(true)
                .build(),
            createUnpublishedCluster(100500L, modelId2).toBuilder()
                .addParameterValues(title("Moo"))
                .setPublished(true)
                .build()
        );
        modelStore.saveClusters(clusters);

        // Публикуем кластеры. Они и так опубликованы, ничего не должно произойти.
        List<PublishModelDiff> publishDiffs = Arrays.asList(
            PublishModelDiff.newBuilder()
                .setId(modelId1)
                .setPublished(true)
                .build(),
            PublishModelDiff.newBuilder()
                .setId(modelId2)
                .setPublished(true)
                .build()
        );

        ImportModelsStats stats = cardApiModelStorage.applyImportV3(100500L, System.currentTimeMillis(),
            Collections.emptyList(), Collections.emptyList(), publishDiffs);

        // Вообще ничего не поменяется.
        Map<String, CommonModel> allModels = byTitle(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(2);

        CommonModel meowPublished = allModels.get("Meow");
        CommonModel mooPublished = allModels.get("Moo");

        Assertions.assertThat(meowPublished.isPublished()).isTrue();
        Assertions.assertThat(mooPublished.isPublished()).isTrue();
        Assertions.assertThat(meowPublished.getId()).isEqualTo(modelId1);
        Assertions.assertThat(mooPublished.getId()).isEqualTo(modelId2);

        Assertions.assertThat(stats.getCreatedIds()).isEmpty();
        Assertions.assertThat(stats.getChangedIds()).isEmpty();
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEmpty();
    }

    @Test
    public void testCreateUpdatePublishV3() throws ModelStoreException {
        long modelId1 = 12L;
        long modelId2 = ModelStoreInterface.GENERATED_ID_MIN_VALUE + 10000L;
        long modelId3 = ModelStoreInterface.GENERATED_ID_MIN_VALUE + 10001L;
        long vendorMeowAfter = 2L;
        long vendorWoofBefore = 3L;
        long vendorWoofAfter = 4L;
        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(100500L, modelId2).toBuilder()
                .addParameterValues(title("Woof"))
                .addParameterValues(vendor(vendorWoofBefore))
                .build(),
            createUnpublishedCluster(100500L, modelId3).toBuilder()
                .addParameterValues(title("Moo"))
                .build()
        );
        modelStore.saveClusters(clusters);

        List<CreateOrUpdateModelDiff> createDiffs = Collections.singletonList(
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId1)
                .setPublished(true)
                .setTitle(loc("Meow"))
                .addParameterValue(title("Meow"))
                .addParameterValue(vendor(vendorMeowAfter))
                .setVendorId(vendorMeowAfter)
                .build()
        );
        List<CreateOrUpdateModelDiff> updateDiffs = Collections.singletonList(
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId2)
                .setTitle(loc("Woof"))
                .addParameterValue(title("Woof"))
                .setVendorId(vendorWoofAfter)
                .addParameterValue(vendor(vendorWoofAfter))
                .setPublished(true)
                .build()
        );
        List<PublishModelDiff> publishDiffs = Collections.singletonList(
            PublishModelDiff.newBuilder()
                .setId(modelId3)
                .setPublished(true)
                .build()
        );
        ImportModelsStats stats = cardApiModelStorage.applyImportV3(100500L, System.currentTimeMillis(),
            createDiffs, updateDiffs, publishDiffs);

        Map<String, List<CommonModel>> allModels = byTitleMulti(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(3);
        Assertions.assertThat(allModels.get("Meow")).hasSize(1);
        Assertions.assertThat(allModels.get("Woof")).hasSize(2);
        Assertions.assertThat(allModels.get("Moo")).hasSize(2);

        CommonModel meowCreated = allModels.get("Meow").get(0);
        CommonModel woofDeleted = allModels.get("Woof").stream().filter(CommonModel::isDeleted).findAny().get();
        CommonModel woofPubdated = allModels.get("Woof").stream().filter(m -> !m.isDeleted()).findAny().get();
        CommonModel mooDeleted = allModels.get("Moo").stream().filter(CommonModel::isDeleted).findAny().get();
        CommonModel mooPublished = allModels.get("Moo").stream().filter(m -> !m.isDeleted()).findAny().get();

        Assertions.assertThat(meowCreated.isPublished()).isTrue();
        Assertions.assertThat(woofPubdated.isPublished()).isTrue();
        Assertions.assertThat(mooPublished.isPublished()).isTrue();
        Assertions.assertThat(meowCreated.getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);
        Assertions.assertThat(woofPubdated.getId()).isNotEqualTo(modelId2);
        Assertions.assertThat(woofPubdated.getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);
        Assertions.assertThat(mooPublished.getId()).isNotEqualTo(modelId3);
        Assertions.assertThat(mooPublished.getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);

        Assertions.assertThat(woofDeleted.isDeleted()).isTrue();
        Assertions.assertThat(mooDeleted.isDeleted()).isTrue();

        Assertions.assertThat(stats.getCreatedIds()).containsExactlyInAnyOrder(meowCreated.getId());
        Assertions.assertThat(stats.getChangedIds()).isEqualTo(ImmutableMap.of(
            modelId2, woofPubdated.getId(),
            modelId3, mooPublished.getId()
        ));
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEqualTo(ImmutableMap.of(
            modelId1, meowCreated.getId()
        ));
    }

    @Test
    public void testUnpublishV3() throws ModelStoreException {
        long modelId1 = 10L;
        long modelId2 = 11L;

        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(100500L, modelId1).toBuilder()
                .addParameterValues(title("Meow"))
                .setPublished(true)
                .build(),
            createUnpublishedCluster(100500L, modelId2).toBuilder()
                .addParameterValues(title("Moo"))
                .setPublished(true)
                .build()
        );
        modelStore.saveClusters(clusters);

        List<PublishModelDiff> publishDiffs = Arrays.asList(
            PublishModelDiff.newBuilder()
                .setId(modelId1)
                .setPublished(false)
                .build(),
            PublishModelDiff.newBuilder()
                .setId(modelId2)
                .setPublished(false)
                .build()
        );

        ImportModelsStats stats = cardApiModelStorage.applyImportV3(100500L, System.currentTimeMillis(),
            Collections.emptyList(), Collections.emptyList(), publishDiffs);

        Map<String, CommonModel> allModels = byTitle(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(2);

        CommonModel meowUnpublished = allModels.get("Meow");
        CommonModel mooUnpublished = allModels.get("Moo");

        Assertions.assertThat(meowUnpublished.isPublished()).isFalse();
        Assertions.assertThat(mooUnpublished.isPublished()).isFalse();
        Assertions.assertThat(meowUnpublished.getId()).isEqualTo(modelId1);
        Assertions.assertThat(mooUnpublished.getId()).isEqualTo(modelId2);

        Assertions.assertThat(stats.getCreatedIds()).isEmpty();
        Assertions.assertThat(stats.getChangedIds()).isEmpty();
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEmpty();
    }

    @Test
    public void testUnpubdateV3() throws ModelStoreException {
        long vendorMeowBefore = 123L;
        long vendorMeowAfter = 1234L;

        long vendorMooBefore = 567L;
        long vendorMooAfter = 5678L;

        long modelId1 = 10L;
        long modelId2 = 11L;

        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(100500L, modelId1).toBuilder()
                .addParameterValues(title("Meow"))
                .addParameterValues(vendor(vendorMeowBefore))
                .setPublished(true)
                .build(),
            createUnpublishedCluster(100500L, modelId2).toBuilder()
                .addParameterValues(title("Moo"))
                .addParameterValues(vendor(vendorMooBefore))
                .setPublished(true)
                .build()
        );
        modelStore.saveClusters(clusters);

        List<CreateOrUpdateModelDiff> updateDiffs = Arrays.asList(
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId1)
                .setTitle(loc("Meow"))
                .addParameterValue(title("Meow"))
                .setVendorId(vendorMeowAfter)
                .addParameterValue(vendor(vendorMeowAfter))
                .setPublished(false)
                .build(),
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId2)
                .setTitle(loc("Moo"))
                .addParameterValue(title("Moo"))
                .setVendorId(vendorMooAfter)
                .addParameterValue(vendor(vendorMooAfter))
                .setPublished(false)
                .build()
        );

        ImportModelsStats stats = cardApiModelStorage.applyImportV3(100500L, System.currentTimeMillis(),
            Collections.emptyList(), updateDiffs, Collections.emptyList());

        Map<String, CommonModel> allModels = byTitle(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(2);

        CommonModel meowUnpublished = allModels.get("Meow");
        CommonModel mooUnpublished = allModels.get("Moo");

        Assertions.assertThat(meowUnpublished.isPublished()).isFalse();
        Assertions.assertThat(mooUnpublished.isPublished()).isFalse();
        Assertions.assertThat(meowUnpublished.getId()).isEqualTo(modelId1);
        Assertions.assertThat(mooUnpublished.getId()).isEqualTo(modelId2);

        Assertions.assertThat(stats.getCreatedIds()).isEmpty();
        Assertions.assertThat(stats.getChangedIds()).isEmpty();
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEmpty();

        Assertions.assertThat(meowUnpublished.getVendorId()).isEqualTo(vendorMeowAfter);
        Assertions.assertThat(mooUnpublished.getVendorId()).isEqualTo(vendorMooAfter);
    }

    @Test
    public void testSimpleCreateV2() {
        long modelId1 = 12L;
        long modelId2 = 15L;
        long modelId3 = ModelStoreInterface.GENERATED_ID_MIN_VALUE + 10000L;
        List<CreateOrUpdateModelDiff> createDiffs = Arrays.asList(
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId1)
                .setPublished(false)
                .setTitle(loc("Meow"))
                .addParameterValue(title("Meow"))
                .build(),
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId2)
                .setPublished(true)
                .setTitle(loc("Woof"))
                .addParameterValue(title("Woof"))
                .build(),
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId3)
                .setPublished(true)
                .setTitle(loc("Moo"))
                .addParameterValue(title("Moo"))
                .build()
        );
        ImportModelsStats stats = cardApiModelStorage.applyImportV2(100500L, System.currentTimeMillis(),
            createDiffs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        // Т.к. IDшники могут меняться по смыслу ручки, будем для удобства идентифицировать модельки по тайтлам в тесте
        Map<String, CommonModel> allModels = byTitle(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(3);
        Assertions.assertThat(allModels.get("Meow").isPublished()).isFalse();
        Assertions.assertThat(allModels.get("Woof").isPublished()).isTrue();
        Assertions.assertThat(allModels.get("Moo").isPublished()).isTrue();
        Assertions.assertThat(allModels.get("Meow").getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);
        Assertions.assertThat(allModels.get("Woof").getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);
        Assertions.assertThat(allModels.get("Moo").getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);

        Assertions.assertThat(stats.getChangedIds()).isEmpty();
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEqualTo(ImmutableMap.of(
            modelId1, allModels.get("Meow").getId(),
            modelId2, allModels.get("Woof").getId(),
            modelId3, allModels.get("Moo").getId()
        ));
        Assertions.assertThat(stats.getCreatedIds()).containsExactlyInAnyOrder(
            allModels.get("Meow").getId(),
            allModels.get("Woof").getId(),
            allModels.get("Moo").getId()
        );
    }

    @Test
    public void testSimpleUpdateV2() throws ModelStoreException {
        long vendorMeowBefore = 123L;
        long vendorMeowAfter = 1234L;

        long vendorMooBefore = 567L;
        long vendorMooAfter = 5678L;

        long modelId1 = 10L;
        long modelId2 = ModelStoreInterface.GENERATED_ID_MIN_VALUE + 100L;

        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(100500L, modelId1).toBuilder()
                .addParameterValues(title("Meow"))
                .addParameterValues(vendor(vendorMeowBefore))
                .setPublished(true)
                .build(),
            createUnpublishedCluster(100500L, modelId2).toBuilder()
                .addParameterValues(title("Moo"))
                .addParameterValues(vendor(vendorMooBefore))
                .build()
        );
        modelStore.saveClusters(clusters);

        // Поменяем вендора. Обратите внимание, что флажок публикации не указан - он триггерит специальную логику,
        // поэтому с флажком будет отдельный тест-кейс.
        List<CreateOrUpdateModelDiff> updateDiffs = Arrays.asList(
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId1)
                .setTitle(loc("Meow"))
                .addParameterValue(title("Meow"))
                .setVendorId(vendorMeowAfter)
                .addParameterValue(vendor(vendorMeowAfter))
                .build(),
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId2)
                .setTitle(loc("Moo"))
                .addParameterValue(title("Moo"))
                .setVendorId(vendorMooAfter)
                .addParameterValue(vendor(vendorMooAfter))
                .build()
        );

        ImportModelsStats stats = cardApiModelStorage.applyImportV2(100500L, System.currentTimeMillis(),
            Collections.emptyList(), updateDiffs, Collections.emptyList(), Collections.emptyList());

        Map<String, CommonModel> allModels = byTitle(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(2);
        Assertions.assertThat(allModels.get("Meow").isPublished()).isTrue();
        Assertions.assertThat(allModels.get("Moo").isPublished()).isFalse();
        Assertions.assertThat(allModels.get("Meow").getId()).isEqualTo(modelId1);
        Assertions.assertThat(allModels.get("Moo").getId()).isEqualTo(modelId2);
        Assertions.assertThat(allModels.get("Meow").getVendorId()).isEqualTo(vendorMeowAfter);
        Assertions.assertThat(allModels.get("Moo").getVendorId()).isEqualTo(vendorMooAfter);

        Assertions.assertThat(stats.getCreatedIds()).isEmpty();
        Assertions.assertThat(stats.getChangedIds()).isEmpty();
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEmpty();
    }

    @Test
    public void testPubdateV2() throws ModelStoreException {
        long vendorMeowBefore = 123L;
        long vendorMeowAfter = 1234L;

        long vendorMooBefore = 567L;
        long vendorMooAfter = 5678L;

        long modelId1 = 10L;
        long modelId2 = ModelStoreInterface.GENERATED_ID_MIN_VALUE + 100L;

        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(100500L, modelId1).toBuilder()
                .addParameterValues(title("Meow"))
                .addParameterValues(vendor(vendorMeowBefore))
                .build(),
            createUnpublishedCluster(100500L, modelId2).toBuilder()
                .addParameterValues(title("Moo"))
                .addParameterValues(vendor(vendorMooBefore))
                .build()
        );
        modelStore.saveClusters(clusters);

        // Поменяем вендора. Обратите внимание, что флажок публикации указан - он триггерит специальную логику,
        // которая попытается опубликовать кластеры. При этом если у кластера лонговый ИДшник, то такой кластер будет
        // удалён, а на его основе создастся опубликованная копия с коротким ИД.
        List<CreateOrUpdateModelDiff> updateDiffs = Arrays.asList(
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId1)
                .setTitle(loc("Meow"))
                .addParameterValue(title("Meow"))
                .setVendorId(vendorMeowAfter)
                .addParameterValue(vendor(vendorMeowAfter))
                .setPublished(true)
                .build(),
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId2)
                .setTitle(loc("Moo"))
                .addParameterValue(title("Moo"))
                .setVendorId(vendorMooAfter)
                .addParameterValue(vendor(vendorMooAfter))
                .setPublished(true)
                .build()
        );

        ImportModelsStats stats = cardApiModelStorage.applyImportV2(100500L, System.currentTimeMillis(),
            Collections.emptyList(), updateDiffs, Collections.emptyList(), Collections.emptyList());

        // Для Moo-модели будет две копии - старая с невалидным ИДшником, которая будет удалена, и новая с правильным
        // коротким ИД, опубликованная и живая. Поэтому сгруппируем по тайтлу в мультимапу.
        Map<String, List<CommonModel>> allModels = byTitleMulti(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(2);
        Assertions.assertThat(allModels.get("Meow")).hasSize(1);
        Assertions.assertThat(allModels.get("Moo")).hasSize(2);

        CommonModel meowPubdated = allModels.get("Meow").get(0);
        CommonModel mooDeleted = allModels.get("Moo").stream().filter(CommonModel::isDeleted).findAny().get();
        CommonModel mooPubdated = allModels.get("Moo").stream().filter(m -> !m.isDeleted()).findAny().get();

        Assertions.assertThat(meowPubdated.isPublished()).isTrue();
        Assertions.assertThat(mooPubdated.isPublished()).isTrue();
        Assertions.assertThat(meowPubdated.getId()).isEqualTo(modelId1);
        Assertions.assertThat(mooPubdated.getId()).isNotEqualTo(modelId2);
        Assertions.assertThat(mooPubdated.getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);
        Assertions.assertThat(meowPubdated.getVendorId()).isEqualTo(vendorMeowAfter);
        Assertions.assertThat(mooPubdated.getVendorId()).isEqualTo(vendorMooAfter);

        // Старая версия Моо модели с длинным ИД теперь удалена. Больше нам про неё ничего не интересно знать.
        Assertions.assertThat(mooDeleted.isDeleted()).isTrue();

        Assertions.assertThat(stats.getCreatedIds()).isEmpty();
        Assertions.assertThat(stats.getChangedIds())
            .containsOnlyKeys(modelId2)
            .containsValues(mooPubdated.getId());
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEmpty();
    }

    @Test
    public void testSimplePublishV2() throws ModelStoreException {
        long modelId1 = 10L;
        long modelId2 = ModelStoreInterface.GENERATED_ID_MIN_VALUE + 100L;

        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(100500L, modelId1).toBuilder()
                .addParameterValues(title("Meow"))
                .build(),
            createUnpublishedCluster(100500L, modelId2).toBuilder()
                .addParameterValues(title("Moo"))
                .build()
        );
        modelStore.saveClusters(clusters);

        // Публикуем кластеры. При этом если у кластера лонговый ИДшник, то такой кластер будет удалён, а на его основе
        // создастся опубликованная копия с коротким ИД.
        List<PublishModelDiff> publishDiffs = Arrays.asList(
            PublishModelDiff.newBuilder()
                .setId(modelId1)
                .setPublished(true)
                .build(),
            PublishModelDiff.newBuilder()
                .setId(modelId2)
                .setPublished(true)
                .build()
        );

        ImportModelsStats stats = cardApiModelStorage.applyImportV2(100500L, System.currentTimeMillis(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), publishDiffs);

        // Для Moo-модели будет две копии - старая с невалидным ИДшником, которая будет удалена, и новая с правильным
        // коротким ИД, опубликованная и живая. Поэтому сгруппируем по тайтлу в мультимапу.
        Map<String, List<CommonModel>> allModels = byTitleMulti(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(2);
        Assertions.assertThat(allModels.get("Meow")).hasSize(1);
        Assertions.assertThat(allModels.get("Moo")).hasSize(2);

        CommonModel meowPublished = allModels.get("Meow").get(0);
        CommonModel mooDeleted = allModels.get("Moo").stream().filter(CommonModel::isDeleted).findAny().get();
        CommonModel mooPublished = allModels.get("Moo").stream().filter(m -> !m.isDeleted()).findAny().get();

        Assertions.assertThat(meowPublished.isPublished()).isTrue();
        Assertions.assertThat(mooPublished.isPublished()).isTrue();
        Assertions.assertThat(meowPublished.getId()).isEqualTo(modelId1);
        Assertions.assertThat(mooPublished.getId()).isNotEqualTo(modelId2);
        Assertions.assertThat(mooPublished.getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);

        // Старая версия Моо модели с длинным ИД теперь удалена. Больше нам про неё ничего не интересно знать.
        Assertions.assertThat(mooDeleted.isDeleted()).isTrue();

        Assertions.assertThat(stats.getCreatedIds()).isEmpty();
        Assertions.assertThat(stats.getChangedIds())
            .containsOnlyKeys(modelId2)
            .containsValues(mooPublished.getId());
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEmpty();
    }

    @Test
    public void testPublishAlreadyPublishedV2() throws ModelStoreException {
        long modelId1 = 10L;
        long modelId2 = 11L;

        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(100500L, modelId1).toBuilder()
                .addParameterValues(title("Meow"))
                .setPublished(true)
                .build(),
            createUnpublishedCluster(100500L, modelId2).toBuilder()
                .addParameterValues(title("Moo"))
                .setPublished(true)
                .build()
        );
        modelStore.saveClusters(clusters);

        // Публикуем кластеры. Они и так опубликованы, ничего не должно произойти.
        List<PublishModelDiff> publishDiffs = Arrays.asList(
            PublishModelDiff.newBuilder()
                .setId(modelId1)
                .setPublished(true)
                .build(),
            PublishModelDiff.newBuilder()
                .setId(modelId2)
                .setPublished(true)
                .build()
        );

        ImportModelsStats stats = cardApiModelStorage.applyImportV2(100500L, System.currentTimeMillis(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), publishDiffs);

        // Вообще ничего не поменяется.
        Map<String, CommonModel> allModels = byTitle(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(2);

        CommonModel meowPublished = allModels.get("Meow");
        CommonModel mooPublished = allModels.get("Moo");

        Assertions.assertThat(meowPublished.isPublished()).isTrue();
        Assertions.assertThat(mooPublished.isPublished()).isTrue();
        Assertions.assertThat(meowPublished.getId()).isEqualTo(modelId1);
        Assertions.assertThat(mooPublished.getId()).isEqualTo(modelId2);

        Assertions.assertThat(stats.getCreatedIds()).isEmpty();
        Assertions.assertThat(stats.getChangedIds()).isEmpty();
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEmpty();
    }

    @Test
    public void testCreateUpdatePublishV2() throws ModelStoreException {
        long modelId1 = 12L;
        long modelId2 = ModelStoreInterface.GENERATED_ID_MIN_VALUE + 10000L;
        long modelId3 = ModelStoreInterface.GENERATED_ID_MIN_VALUE + 10001L;
        long vendorMeowAfter = 2L;
        long vendorWoofBefore = 3L;
        long vendorWoofAfter = 4L;
        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(100500L, modelId2).toBuilder()
                .addParameterValues(title("Woof"))
                .addParameterValues(vendor(vendorWoofBefore))
                .build(),
            createUnpublishedCluster(100500L, modelId3).toBuilder()
                .addParameterValues(title("Moo"))
                .build()
        );
        modelStore.saveClusters(clusters);

        List<CreateOrUpdateModelDiff> createDiffs = Collections.singletonList(
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId1)
                .setPublished(true)
                .setTitle(loc("Meow"))
                .addParameterValue(title("Meow"))
                .addParameterValue(vendor(vendorMeowAfter))
                .setVendorId(vendorMeowAfter)
                .build()
        );
        List<CreateOrUpdateModelDiff> updateDiffs = Collections.singletonList(
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId2)
                .setTitle(loc("Woof"))
                .addParameterValue(title("Woof"))
                .setVendorId(vendorWoofAfter)
                .addParameterValue(vendor(vendorWoofAfter))
                .setPublished(true)
                .build()
        );
        List<PublishModelDiff> publishDiffs = Collections.singletonList(
            PublishModelDiff.newBuilder()
                .setId(modelId3)
                .setPublished(true)
                .build()
        );
        ImportModelsStats stats = cardApiModelStorage.applyImportV2(100500L, System.currentTimeMillis(),
            createDiffs, updateDiffs, Collections.emptyList(), publishDiffs);

        Map<String, List<CommonModel>> allModels = byTitleMulti(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(3);
        Assertions.assertThat(allModels.get("Meow")).hasSize(1);
        Assertions.assertThat(allModels.get("Woof")).hasSize(2);
        Assertions.assertThat(allModels.get("Moo")).hasSize(2);

        CommonModel meowCreated = allModels.get("Meow").get(0);
        CommonModel woofDeleted = allModels.get("Woof").stream().filter(CommonModel::isDeleted).findAny().get();
        CommonModel woofPubdated = allModels.get("Woof").stream().filter(m -> !m.isDeleted()).findAny().get();
        CommonModel mooDeleted = allModels.get("Moo").stream().filter(CommonModel::isDeleted).findAny().get();
        CommonModel mooPublished = allModels.get("Moo").stream().filter(m -> !m.isDeleted()).findAny().get();

        Assertions.assertThat(meowCreated.isPublished()).isTrue();
        Assertions.assertThat(woofPubdated.isPublished()).isTrue();
        Assertions.assertThat(mooPublished.isPublished()).isTrue();
        Assertions.assertThat(meowCreated.getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);
        Assertions.assertThat(woofPubdated.getId()).isNotEqualTo(modelId2);
        Assertions.assertThat(woofPubdated.getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);
        Assertions.assertThat(mooPublished.getId()).isNotEqualTo(modelId3);
        Assertions.assertThat(mooPublished.getId()).isLessThan(ModelStoreInterface.GENERATED_ID_MIN_VALUE);

        Assertions.assertThat(woofDeleted.isDeleted()).isTrue();
        Assertions.assertThat(mooDeleted.isDeleted()).isTrue();

        Assertions.assertThat(stats.getCreatedIds()).containsExactlyInAnyOrder(meowCreated.getId());
        Assertions.assertThat(stats.getChangedIds()).isEqualTo(ImmutableMap.of(
            modelId2, woofPubdated.getId(),
            modelId3, mooPublished.getId()
        ));
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEqualTo(ImmutableMap.of(
            modelId1, meowCreated.getId()
        ));
    }

    @Test
    public void testUnpublishV2() throws ModelStoreException {
        long modelId1 = 10L;
        long modelId2 = 11L;

        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(100500L, modelId1).toBuilder()
                .addParameterValues(title("Meow"))
                .setPublished(true)
                .build(),
            createUnpublishedCluster(100500L, modelId2).toBuilder()
                .addParameterValues(title("Moo"))
                .setPublished(true)
                .build()
        );
        modelStore.saveClusters(clusters);

        List<PublishModelDiff> publishDiffs = Arrays.asList(
            PublishModelDiff.newBuilder()
                .setId(modelId1)
                .setPublished(false)
                .build(),
            PublishModelDiff.newBuilder()
                .setId(modelId2)
                .setPublished(false)
                .build()
        );

        ImportModelsStats stats = cardApiModelStorage.applyImportV2(100500L, System.currentTimeMillis(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), publishDiffs);

        Map<String, CommonModel> allModels = byTitle(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(2);

        CommonModel meowUnpublished = allModels.get("Meow");
        CommonModel mooUnpublished = allModels.get("Moo");

        Assertions.assertThat(meowUnpublished.isPublished()).isFalse();
        Assertions.assertThat(mooUnpublished.isPublished()).isFalse();
        Assertions.assertThat(meowUnpublished.getId()).isEqualTo(modelId1);
        Assertions.assertThat(mooUnpublished.getId()).isEqualTo(modelId2);

        Assertions.assertThat(stats.getCreatedIds()).isEmpty();
        Assertions.assertThat(stats.getChangedIds()).isEmpty();
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEmpty();
    }

    @Test
    public void testUnpubdateV2() throws ModelStoreException {
        long vendorMeowBefore = 123L;
        long vendorMeowAfter = 1234L;

        long vendorMooBefore = 567L;
        long vendorMooAfter = 5678L;

        long modelId1 = 10L;
        long modelId2 = 11L;

        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(100500L, modelId1).toBuilder()
                .addParameterValues(title("Meow"))
                .addParameterValues(vendor(vendorMeowBefore))
                .setPublished(true)
                .build(),
            createUnpublishedCluster(100500L, modelId2).toBuilder()
                .addParameterValues(title("Moo"))
                .addParameterValues(vendor(vendorMooBefore))
                .setPublished(true)
                .build()
        );
        modelStore.saveClusters(clusters);

        List<CreateOrUpdateModelDiff> updateDiffs = Arrays.asList(
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId1)
                .setTitle(loc("Meow"))
                .addParameterValue(title("Meow"))
                .setVendorId(vendorMeowAfter)
                .addParameterValue(vendor(vendorMeowAfter))
                .setPublished(false)
                .build(),
            CreateOrUpdateModelDiff.newBuilder()
                .setId(modelId2)
                .setTitle(loc("Moo"))
                .addParameterValue(title("Moo"))
                .setVendorId(vendorMooAfter)
                .addParameterValue(vendor(vendorMooAfter))
                .setPublished(false)
                .build()
        );

        ImportModelsStats stats = cardApiModelStorage.applyImportV2(100500L, System.currentTimeMillis(),
            Collections.emptyList(), updateDiffs, Collections.emptyList(), Collections.emptyList());

        Map<String, CommonModel> allModels = byTitle(modelStorageService.getAllModels());
        Assertions.assertThat(allModels).hasSize(2);

        CommonModel meowUnpublished = allModels.get("Meow");
        CommonModel mooUnpublished = allModels.get("Moo");

        Assertions.assertThat(meowUnpublished.isPublished()).isFalse();
        Assertions.assertThat(mooUnpublished.isPublished()).isFalse();
        Assertions.assertThat(meowUnpublished.getId()).isEqualTo(modelId1);
        Assertions.assertThat(mooUnpublished.getId()).isEqualTo(modelId2);

        Assertions.assertThat(stats.getCreatedIds()).isEmpty();
        Assertions.assertThat(stats.getChangedIds()).isEmpty();
        Assertions.assertThat(stats.getEmptyTitleIds()).isEmpty();
        Assertions.assertThat(stats.getFailedIds()).isEmpty();
        Assertions.assertThat(stats.getFakeToRealIds()).isEmpty();

        Assertions.assertThat(meowUnpublished.getVendorId()).isEqualTo(vendorMeowAfter);
        Assertions.assertThat(mooUnpublished.getVendorId()).isEqualTo(vendorMooAfter);
    }

    @Test
    public void testClusterPublicationChangesIndexing() throws ModelStoreInterface.ModelStoreException {
        List<ModelStorage.Model> clusters = Arrays.asList(
            createUnpublishedCluster(10, ModelStoreInterface.GENERATED_ID_MIN_VALUE)
        );
        modelStore.saveClusters(clusters);

        List<ModelCardApi.PublishModelDiff> publishModelDiffs = Arrays.asList(
            ModelCardApi.PublishModelDiff.newBuilder()
                .setPublished(true)
                .setId(ModelStoreInterface.GENERATED_ID_MIN_VALUE)
                .build()
        );
        cardApiModelStorage.applyImportV3(10, 10L,
            Collections.emptyList(),
            Collections.emptyList(),
            publishModelDiffs
        );

        // Check model storage content
        Optional<CommonModel> oldModel = modelStorageService.getAllModels().stream()
            .filter(model -> model.isDeleted())
            .findFirst();
        Optional<CommonModel> newModel = modelStorageService.getAllModels().stream()
            .filter(model -> !model.isDeleted())
            .findFirst();

        Assert.assertTrue(oldModel.isPresent());
        Assert.assertFalse(oldModel.get().isPublished());
        Assert.assertEquals((long) ModelStoreInterface.GENERATED_ID_MIN_VALUE, oldModel.get().getId());

        Assert.assertTrue(newModel.isPresent());
        Assert.assertTrue(newModel.get().isPublished());

        // Check index content
        Optional<ModelStorage.Model> indexeOldModel = updateModelIndexInterface.getIndexed().stream()
            .filter(model -> model.getDeleted())
            .findFirst();
        Optional<ModelStorage.Model> indexeNewModel = updateModelIndexInterface.getIndexed().stream()
            .filter(model -> !model.getDeleted())
            .findFirst();

        Assert.assertTrue(indexeOldModel.isPresent());
        Assert.assertFalse(indexeOldModel.get().getPublished());
        Assert.assertEquals((long) ModelStoreInterface.GENERATED_ID_MIN_VALUE, indexeOldModel.get().getId());

        Assert.assertTrue(indexeNewModel.isPresent());
        Assert.assertTrue(indexeNewModel.get().getPublished());
    }

    private LocalizedString loc(String str) {
        return LocalizedString.newBuilder()
            .setIsoCode(Language.RUSSIAN.getIsoCode())
            .setValue(str)
            .build();
    }

    private ParameterValue title(String title) {
        return ParameterValue.newBuilder()
            .setXslName(XslNames.NAME)
            .setParamId(1)
            .setValueType(ValueType.STRING)
            .addStrValue(loc(title))
            .build();
    }

    private ParameterValue vendor(long vendorId) {
        return ParameterValue.newBuilder()
            .setXslName(XslNames.VENDOR)
            .setParamId(2)
            .setValueType(ValueType.ENUM)
            .setOptionId((int) vendorId)
            .build();
    }

    private Map<String, CommonModel> byTitle(Collection<CommonModel> models) {
        return models.stream().collect(Collectors.toMap(CommonModel::getTitle, Function.identity()));
    }

    private Map<String, List<CommonModel>> byTitleMulti(Collection<CommonModel> models) {
        return models.stream().collect(Collectors.groupingBy(CommonModel::getTitle));
    }
}
