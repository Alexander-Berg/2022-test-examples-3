package ru.yandex.market.psku.postprocessor.service.deleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.export.MboParameters.ValueType;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.LocalizedString;
import ru.yandex.market.mbo.http.ModelStorage.Model;
import ru.yandex.market.mbo.http.ModelStorage.ModificationSource;
import ru.yandex.market.mbo.http.ModelStorage.ParameterValue;
import ru.yandex.market.mbo.http.ModelStorage.Picture;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterContentDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterMetaDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterMeta;
import ru.yandex.market.psku.postprocessor.service.staff.StaffUserService;

import static java.lang.Math.random;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper.AUTO_USER_ID;
import static ru.yandex.market.psku.postprocessor.common.db.dao.ClusterContentDao.REJECTED_MAPPING_ID;

public class DeduplicatedPskuDeleterServiceTest extends BaseDBTest {

    public static final List<Integer> COLOR_VENDORS = asList(
            15624431, 14898402, 14896353, 15106275, 15562475, 15562474, 14897380, 15839993, 14897897
    );
    public static final List<Integer> VENDORS = asList(
            14297955, 9383899, 9325837, 9285308, 11153407, 8337980, 8364096, 9294121, 8340925
    );
    public static final List<Integer> PANTS_SIZES = asList(
            27132310, 14516437, 14516433, 14516429, 14516425, 14516421, 27132290, 14516417, 27132350
    );
    private static final long CATEGORY_ID = 7811907L;
    private static final long SUPPLIER_ID = 10473485L;

    @Autowired
    private ClusterContentDao clusterContentDao;

    @Autowired
    private ClusterMetaDao clusterMetaDao;

    private ModelStorageHelper modelStorageHelper;
    private DeduplicatedPskuDeleterService deduplicatedPskuDeleterService;

    @Before
    public void setUp() throws Exception {
        ModelStorageServiceStub modelStorageServiceStub = new ModelStorageServiceStub();
        modelStorageServiceStub.setHost("http://mbo-card-api.tst.vs.market.yandex.net:33714/modelStorage/");
        modelStorageHelper = new ModelStorageHelper(modelStorageServiceStub, modelStorageServiceStub);
        deduplicatedPskuDeleterService = new DeduplicatedPskuDeleterService(
                modelStorageHelper, clusterContentDao, clusterMetaDao, Mockito.mock(StaffUserService.class),
                null,  0
        );
    }

    @Test
    public void testLoadPartitionedPskuDeleteInfos() {
        for (int clusterSize = 1; clusterSize < 150; clusterSize += 25) {
            ClusterMeta clusterMeta = createClusterMeta();
            for (int i = 0; i < clusterSize; ++i) {
                createClusterContent(clusterMeta.getId(), (long) clusterSize, (long) i);
            }
        }

        List<List<DeduplicationDeleteInfo>> lists = deduplicatedPskuDeleterService.loadPartitionedPkuClusterDeleteInfos();
        assertThat(lists).hasSize(6);
        List<Integer> sizes = lists.stream().map(List::size).sorted().collect(Collectors.toList());

        assertThat(sizes.get(0)).isEqualTo(1);
        assertThat(sizes.get(1)).isEqualTo(26);
        assertThat(sizes.get(2)).isEqualTo(51);
        assertThat(sizes.get(3)).isEqualTo(76);
        assertThat(sizes.get(4)).isEqualTo(101);
        assertThat(sizes.get(5)).isEqualTo(126);
    }

    @Test
    @Ignore
    public void test_DeleteSkuFromModelWith2Sku_SkuDeletedAndModelStays() {
        // Удаляем СКЮ, но у модели есть другие СКЮ --> Удалена СКЮ, модель не тронута
        ModelStorageHelper.SaveGroupResponse saveGroupResponseInitial = initPskusWithParentModel(2);
        ModelStorageHelper.SaveGroupResponse saveGroupResponseAccepting = initPskusWithParentModel(1);
        List<Long> skusIds = getSkusIds(saveGroupResponseInitial);
        Long acceptingSkuId = getSkusIds(saveGroupResponseAccepting).get(0);
        long parentModelId = getParentModelId(saveGroupResponseInitial);

        new DeduplicatedPskuDeleter(clusterContentDao, clusterMetaDao, modelStorageHelper,
                singletonList(new DeduplicationDeleteInfo(skusIds.get(0), acceptingSkuId, 0L, 0L, 0L))).run();

        assertThat(modelStorageHelper.findModels(skusIds)).hasSize(skusIds.size() - 1);
        assertThat(modelStorageHelper.findModel(parentModelId)).isPresent().get()
                .matches(model -> model.getRelationsCount() == skusIds.size() - 1);
    }


    @Test
    @Ignore
    public void test_DeleteSkuFromModelWithoutOtherSkus_ModelAndSkuDeleted() {
        // Удаляем СКЮ, которая у модели последняя --> Удалена и модель и СКЮ
        ModelStorageHelper.SaveGroupResponse saveGroupResponseInitial = initPskusWithParentModel(1);
        ModelStorageHelper.SaveGroupResponse saveGroupResponseAccepting = initPskusWithParentModel(1);
        List<Long> skusIds = getSkusIds(saveGroupResponseInitial);
        Long acceptingSkuId = getSkusIds(saveGroupResponseAccepting).get(0);
        long parentModelId = getParentModelId(saveGroupResponseInitial);

        new DeduplicatedPskuDeleter(clusterContentDao, clusterMetaDao, modelStorageHelper,
                singletonList(new DeduplicationDeleteInfo(skusIds.get(0), acceptingSkuId, 0L, 0L, 0L))).run();

        assertThat(modelStorageHelper.findModels(skusIds)).hasSize(skusIds.size() - 1);
        assertThat(modelStorageHelper.findModel(parentModelId)).isEmpty();
    }

    @Test
    @Ignore
    public void test_Delete2SkuFromModelWith3Sku_SkusAreDeletedAndModelStays() {
        // Удаляем у модели 2 СКЮ, но у неё есть ещё --> Удалены СКЮ, модель не тронута
        ModelStorageHelper.SaveGroupResponse saveGroupResponseInitial = initPskusWithParentModel(3);
        ModelStorageHelper.SaveGroupResponse saveGroupResponseAccepting = initPskusWithParentModel(2);
        List<Long> skusIds = getSkusIds(saveGroupResponseInitial);
        List<Long> acceptingSkuIds = getSkusIds(saveGroupResponseAccepting);
        long parentModelId = getParentModelId(saveGroupResponseInitial);

        new DeduplicatedPskuDeleter(clusterContentDao, clusterMetaDao, modelStorageHelper,
                asList(
                        new DeduplicationDeleteInfo(skusIds.get(0), acceptingSkuIds.get(0), 0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds.get(1), acceptingSkuIds.get(1), 0L,0L, 0L)
                )).run();

        assertThat(modelStorageHelper.findModels(skusIds)).hasSize(skusIds.size() - 2);
        assertThat(modelStorageHelper.findModel(parentModelId)).isPresent().get()
                .matches(model -> model.getRelationsCount() == skusIds.size() - 2);
    }

    @Test
    @Ignore
    public void test_Delete3SkusFromModelWith3Skus_ModelAndSkusAreDeleted() {
        // Удаляем у модели 3 СКЮ, и у неё других нет --> Удалена и модель и все СКЮ
        ModelStorageHelper.SaveGroupResponse saveGroupResponseInitial = initPskusWithParentModel(3);
        ModelStorageHelper.SaveGroupResponse saveGroupResponseAccepting = initPskusWithParentModel(3);
        List<Long> skusIds = getSkusIds(saveGroupResponseInitial);
        List<Long> acceptingSkuIds = getSkusIds(saveGroupResponseAccepting);
        long parentModelId = getParentModelId(saveGroupResponseInitial);

        new DeduplicatedPskuDeleter(clusterContentDao, clusterMetaDao, modelStorageHelper,
                asList(
                        new DeduplicationDeleteInfo(skusIds.get(0), acceptingSkuIds.get(0), 0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds.get(1), acceptingSkuIds.get(1), 0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds.get(2), acceptingSkuIds.get(2), 0L, 0L, 0L)
                )).run();


        assertThat(modelStorageHelper.findModels(skusIds)).hasSize(skusIds.size() - 3);
        assertThat(modelStorageHelper.findModel(parentModelId)).isEmpty();
    }

    @Test
    @Ignore
    public void test_deletingMoreThanOneGroup() {
        // Удаляем у модели 3 СКЮ, и у неё других нет --> Удалена и модель и все СКЮ
        ModelStorageHelper.SaveGroupResponse saveGroupResponseInitial = initPskusWithParentModel(3);
        ModelStorageHelper.SaveGroupResponse saveGroupResponseAccepting = initPskusWithParentModel(3);
        List<Long> skusIds = getSkusIds(saveGroupResponseInitial);
        List<Long> acceptingSkuIds = getSkusIds(saveGroupResponseAccepting);
        long parentModelId = getParentModelId(saveGroupResponseInitial);

        ModelStorageHelper.SaveGroupResponse saveGroupResponseInitial1 = initPskusWithParentModel(3);
        ModelStorageHelper.SaveGroupResponse saveGroupResponseAccepting1 = initPskusWithParentModel(3);
        List<Long> skusIds1 = getSkusIds(saveGroupResponseInitial1);
        List<Long> acceptingSkuIds1 = getSkusIds(saveGroupResponseAccepting1);
        long parentModelId1 = getParentModelId(saveGroupResponseInitial1);

        new DeduplicatedPskuDeleter(clusterContentDao, clusterMetaDao, modelStorageHelper,
                asList(
                        new DeduplicationDeleteInfo(skusIds.get(0), acceptingSkuIds.get(0), 0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds.get(1), acceptingSkuIds.get(1), 0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds.get(2), acceptingSkuIds.get(2), 0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds1.get(0), acceptingSkuIds1.get(0), 0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds1.get(1), acceptingSkuIds1.get(1), 0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds1.get(2), acceptingSkuIds1.get(2), 0L, 0L, 0L)
                )).run();

        assertThat(modelStorageHelper.findModels(skusIds)).hasSize(skusIds.size() - 3);
        assertThat(modelStorageHelper.findModel(parentModelId)).isEmpty();
        assertThat(modelStorageHelper.findModels(skusIds1)).hasSize(skusIds.size() - 3);
        assertThat(modelStorageHelper.findModel(parentModelId1)).isEmpty();
    }


    @Test
    @Ignore
    public void testSpeedWithParallel() {
        ModelStorageHelper.SaveGroupResponse saveGroupResponseInitial = initPskusWithParentModel(3);
        ModelStorageHelper.SaveGroupResponse saveGroupResponseAccepting = initPskusWithParentModel(3);
        List<Long> skusIds = getSkusIds(saveGroupResponseInitial);
        List<Long> acceptingSkuIds = getSkusIds(saveGroupResponseAccepting);

        ModelStorageHelper.SaveGroupResponse saveGroupResponseInitial1 = initPskusWithParentModel(3);
        ModelStorageHelper.SaveGroupResponse saveGroupResponseAccepting1 = initPskusWithParentModel(3);
        List<Long> skusIds1 = getSkusIds(saveGroupResponseInitial1);
        List<Long> acceptingSkuIds1 = getSkusIds(saveGroupResponseAccepting1);

        long startNoParallel = System.currentTimeMillis();


        new DeduplicatedPskuDeleter(clusterContentDao, clusterMetaDao, modelStorageHelper,
                asList(
                        new DeduplicationDeleteInfo(skusIds.get(0), acceptingSkuIds.get(0),0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds.get(1), acceptingSkuIds.get(1),0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds.get(2), acceptingSkuIds.get(2),0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds1.get(0), acceptingSkuIds1.get(0),0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds1.get(1), acceptingSkuIds1.get(1),0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds1.get(2), acceptingSkuIds1.get(2),0L, 0L, 0L)
                )).run();

        long finishNoParallel = System.currentTimeMillis();

        saveGroupResponseInitial = initPskusWithParentModel(3);
        saveGroupResponseAccepting = initPskusWithParentModel(3);
        skusIds = getSkusIds(saveGroupResponseInitial);
        acceptingSkuIds = getSkusIds(saveGroupResponseAccepting);

        saveGroupResponseInitial1 = initPskusWithParentModel(3);
        saveGroupResponseAccepting1 = initPskusWithParentModel(3);
        skusIds1 = getSkusIds(saveGroupResponseInitial1);
        acceptingSkuIds1 = getSkusIds(saveGroupResponseAccepting1);

        long startParallel = System.currentTimeMillis();
        Lists.partition(
                asList(
                        new DeduplicationDeleteInfo(skusIds.get(0), acceptingSkuIds.get(0),0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds.get(1), acceptingSkuIds.get(1),0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds.get(2), acceptingSkuIds.get(2),0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds1.get(0), acceptingSkuIds1.get(0),0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds1.get(1), acceptingSkuIds1.get(1),0L, 0L, 0L),
                        new DeduplicationDeleteInfo(skusIds1.get(2), acceptingSkuIds1.get(2),0L, 0L, 0L)
                ), 2
        ).stream().parallel().forEach(pskusInBatch -> {
            try {
                new DeduplicatedPskuDeleter(clusterContentDao, clusterMetaDao, modelStorageHelper, pskusInBatch).run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        long finishParallel = System.currentTimeMillis();

        System.out.println("Not parallel: " + (finishNoParallel - startNoParallel) + "\n");
        System.out.println("Parallel: " + (finishParallel - startParallel) + "\n");
    }

    private ModelStorageHelper.SaveGroupResponse initPskusWithParentModel(int pskusCount) {
        List<Model> skusWithParentModel = createSkusWithParentModel(pskusCount);
        return saveModels(skusWithParentModel);
    }

    private long getParentModelId(ModelStorageHelper.SaveGroupResponse saveGroupResponse) {
        return saveGroupResponse.getResponse().getResponse(0)
                .getRequestedModelsStatusesList().stream()
                .filter(os -> os.getModel().getCurrentType().equals("GURU"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Skus could not be saved without a model"))
                .getModelId();
    }

    @NotNull
    private List<Long> getSkusIds(ModelStorageHelper.SaveGroupResponse saveGroupResponse) {
        return saveGroupResponse.getResponse().getResponse(0)
                .getRequestedModelsStatusesList().stream()
                .filter(os -> os.getModel().getCurrentType().equals("SKU"))
                .map(ModelStorage.OperationStatus::getModelId)
                .collect(Collectors.toList());
    }

    private List<Model> createSkusWithParentModel(int skusCount) {
        assert skusCount >= 1;
        assert skusCount <= 9;

        Model.Builder parentModel = Model.newBuilder()
                .setId(-1)
                .setPublished(true)
                .setCategoryId(CATEGORY_ID)
                .setSourceType("PARTNER")
                .setCurrentType("GURU")
                .setSupplierId(SUPPLIER_ID)
                .addPictures(getSkuPicture())
                .addAllParameterValues(getModelParameterValues());

        List<Model> skus = new ArrayList<>();
        for (int i = 2; i < skusCount + 2; i++) {
            int skuId = -i;
            parentModel.addRelations(ModelStorage.Relation.newBuilder()
                    .setId(skuId)
                    .setType(ModelStorage.RelationType.SKU_MODEL)
                    .setCategoryId(CATEGORY_ID)
                    .build());

            skus.add(
                    Model.newBuilder()
                            .setId(skuId)
                            .setCategoryId(CATEGORY_ID)
                            .setSourceType("PARTNER_SKU")
                            .setCurrentType("SKU")
                            .setSupplierId(SUPPLIER_ID)
                            .setPublished(true)
                            .addRelations(
                                    ModelStorage.Relation.newBuilder()
                                            .setId(-1)
                                            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                                            .setCategoryId(CATEGORY_ID)
                                            .build()
                            )
                            .addPictures(getSkuPicture())
                            .addAllParameterValues(getSkuParamValues(i))
                            .build()
            );
        }

        skus.add(parentModel.build());

        return skus;
    }

    private List<Model> createSkus(Collection<Long> skuIds) {

        List<Model> skus = new ArrayList<>();
        for (long id : skuIds) {
            skus.add(
                    Model.newBuilder()
                            .setId(id)
                            .setCategoryId(CATEGORY_ID)
                            .setSourceType("PARTNER_SKU")
                            .setCurrentType("SKU")
                            .setSupplierId(SUPPLIER_ID)
                            .addAllRelations(
                                    id == 0 ? Arrays.asList(
                                            ModelStorage.Relation.newBuilder()
                                                    .setId(1)
                                                    .setType(ModelStorage.RelationType.SKU_MODEL)
                                                    .build(),
                                            ModelStorage.Relation.newBuilder()
                                                    .setId(2)
                                                    .setType(ModelStorage.RelationType.SKU_MODEL)
                                                    .build()
                                    ) : Collections.singletonList(
                                            ModelStorage.Relation.newBuilder()
                                                    .setId(0)
                                                    .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                                                    .build()
                                    )
                            )
                            .setPublished(true)
                            .addPictures(getSkuPicture())
                            .addAllParameterValues(getSkuParamValues((int) id % 9))
                            .build()
            );

        }

        return skus;
    }

    @NotNull
    private List<ParameterValue> getSkuParamValues(int i) {
        return asList(
                ParameterValue.newBuilder()
                        .setTypeId(2)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(24815850)
                        .setXslName("height_woman_MAX")
                        .setValueType(ValueType.NUMERIC)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .setNumericValue(format("%d%d", 17, i))
                        .build(),
                ParameterValue.newBuilder()
                        .setTypeId(1)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(14871214)
                        .setXslName("color_vendor")
                        .setOptionId(COLOR_VENDORS.get(i))
                        .setValueType(ValueType.ENUM)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .build(),
                ParameterValue.newBuilder()
                        .setTypeId(1)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(7893318)
                        .setXslName("vendor")
                        .setOptionId(VENDORS.get(i))
                        .setValueType(ValueType.ENUM)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .build(),
                ParameterValue.newBuilder()
                        .setTypeId(2)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(24815151)
                        .setXslName("pants_size_MIN")
                        .setValueType(ValueType.NUMERIC)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .setNumericValue(format("9%d", i))
                        .build(),
                ParameterValue.newBuilder()
                        .setTypeId(4)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(15341921)
                        .setXslName("description")
                        .addStrValue(asLocalized(format("Джинсы что-то там - %d", i)))
                        .setValueType(ValueType.STRING)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .build(),
                ParameterValue.newBuilder()
                        .setTypeId(4)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(14202862)
                        .setXslName("BarCode")
                        .addStrValue(asLocalized(format("765478654389%d", i)))
                        .setValueType(ValueType.STRING)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .build(),
                ParameterValue.newBuilder()
                        .setTypeId(1)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(14474267)
                        .setXslName("pants_size")
                        .setOptionId(PANTS_SIZES.get(i))
                        .setValueType(ValueType.ENUM)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .build(),
                ParameterValue.newBuilder()
                        .setTypeId(4)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(7351771)
                        .setXslName("name")
                        .addStrValue(asLocalized(format("Джинсы опять - %d", i)))
                        .setValueType(ValueType.STRING)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .build(),
                ParameterValue.newBuilder()
                        .setTypeId(0)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(17578891)
                        .setXslName("use_name_as_title")
                        .setOptionId(17578892)
                        .setBoolValue(true)
                        .setValueType(ValueType.BOOLEAN)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .build()
        );
    }

    @NotNull
    private List<ParameterValue> getModelParameterValues() {
        return asList(
                ParameterValue.newBuilder()
                        .setTypeId(1)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(7893318)
                        .setXslName("vendor")
                        .setOptionId(4654044)
                        .setValueType(ValueType.ENUM)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .build(),
                ParameterValue.newBuilder()
                        .setTypeId(4)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(7351771)
                        .setXslName("name")
                        .addStrValue(asLocalized("Джинсы"))
                        .setValueType(ValueType.STRING)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .build(),
                ParameterValue.newBuilder()
                        .setTypeId(0)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(17578891)
                        .setXslName("use_name_as_title")
                        .setOptionId(17578892)
                        .setBoolValue(true)
                        .setValueType(ValueType.BOOLEAN)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .build(),
                ParameterValue.newBuilder()
                        .setTypeId(1)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(17693310)
                        .setXslName("model_quality")
                        .setOptionId(17693319)
                        .setValueType(ValueType.ENUM)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .build(),
                ParameterValue.newBuilder()
                        .setTypeId(4)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(18003611)
                        .setXslName("psku_group_id")
                        .addStrValue(asLocalized("11112222"))
                        .setValueType(ValueType.STRING)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .build(),
                ParameterValue.newBuilder()
                        .setTypeId(1)
                        .setUserId(AUTO_USER_ID)
                        .setParamId(18080891)
                        .setXslName("psku_source_create")
                        .setOptionId(18080893)
                        .setValueType(ValueType.ENUM)
                        .setValueSource(ModificationSource.VENDOR_OFFICE)
                        .build()
        );
    }

    @NotNull
    private LocalizedString asLocalized(String str) {
        return LocalizedString.newBuilder().setIsoCode("ru").setValue(str).build();
    }

    @NotNull
    private Picture getSkuPicture() {
        return Picture.newBuilder()
                .setUrl("//avatars.mds.yandex.net/get-mpic/5032583/img_id3153818321975534599.jpeg/orig")
                .setWidth(926)
                .setHeight(876)
                .setOrigMd5("165bd54f4d877094ac7d30edacba9c6c")
                .setUrlOrig("//avatars.mds.yandex.net/get-mpic/4012096/img_id3486335894518867972.jpeg/orig")
                .setColorness(0.0)
                .setUrlSource("http://avatars.mds.yandex" +
                        ".net/get-marketpictesting/1492023/picd4787abd5d3e3b0e309b8344fd9f0ecc/orig")
                .setValueSource(ModificationSource.AUTO)
                .setColornessAvg(0.0)
                .setIsWhiteBackground(true)
                .build();
    }

    private ModelStorageHelper.SaveGroupResponse saveModels(List<Model> models) {
        return modelStorageHelper.executeSaveModelRequest(
                ModelCardApi.SaveModelsGroupRequest.newBuilder()
                        .addModelsRequest(
                                createSaveModelsRequestBuilder()
                                        .addAllModels(models)
                        )
                        .build()
        );
    }


    private ModelStorage.SaveModelsRequest.Builder createSaveModelsRequestBuilder() {
        return ModelStorage.SaveModelsRequest.newBuilder()
                .setModificationSource(ModificationSource.VENDOR_OFFICE)
                .setUserId(AUTO_USER_ID)
                .setSource(MboAudit.Source.PARTNER_PSKU2)
                .setForceProcess(true)
                .setForcePicker(true)
                .setReplacePictures(true)
                .setApplyModelRules(true)
                .setEraseBrokenParams(true)
                .setDeduplicatePictures(false)
                .setEraseParamsWithInvalidOptionIds(true)
                .setSkipPsku20MandatoryParamsValidation(true)
                .setMergeParameters(false)
                .setForceAll(true)
                .setEraseInvalidRuleParams(true)
                .setSkipFirstPictureValidation(true);
    }

    @Test
    public void testGetOnlyNeededElementToDelete() {
        Long clusterMetaId = 1L;
        List<ClusterContent> clusterContentList = new ArrayList<>();
        // Царь карточка
        ClusterContent clusterContentTzar = new ClusterContent();
        clusterContentTzar.setClusterMetaId(clusterMetaId);
        clusterContentTzar.setStatus(ClusterContentStatus.NEW);
        clusterContentTzar.setType(ClusterContentType.PSKU);
        clusterContentList.add(clusterContentTzar);
        // DSBS офер
        ClusterContent clusterContentDSBS = new ClusterContent();
        clusterContentDSBS.setClusterMetaId(clusterMetaId);
        clusterContentDSBS.setStatus(ClusterContentStatus.REMAPPED);
        clusterContentDSBS.setType(ClusterContentType.DSBS);
        clusterContentList.add(clusterContentDSBS);
        // Реджекнутый ПСКЮ
        ClusterContent clusterContentPSKUReject = new ClusterContent();
        clusterContentPSKUReject.setClusterMetaId(clusterMetaId);
        clusterContentPSKUReject.setStatus(ClusterContentStatus.NEW);
        clusterContentPSKUReject.setType(ClusterContentType.PSKU);
        clusterContentPSKUReject.setTargetSkuId(REJECTED_MAPPING_ID);
        clusterContentList.add(clusterContentPSKUReject);
        // Принятый ПСКЮ
        ClusterContent clusterContentPSKUAccept = new ClusterContent();
        clusterContentPSKUAccept.setClusterMetaId(clusterMetaId);
        clusterContentPSKUAccept.setStatus(ClusterContentStatus.REMAPPED);
        clusterContentPSKUAccept.setType(ClusterContentType.PSKU);
        clusterContentPSKUAccept.setTargetSkuId(1L);
        clusterContentList.add(clusterContentPSKUAccept);

        List<ClusterContent> clusterContentToDelete =
            DeduplicatedPskuDeleterService.getClusterContentToDelete(clusterContentList);
        assertThat(clusterContentToDelete).hasSize(1);
        assertThat(clusterContentToDelete).contains(clusterContentPSKUAccept);
        assertThat(clusterContentToDelete).doesNotContain(clusterContentTzar);
        assertThat(clusterContentToDelete).doesNotContain(clusterContentDSBS);
        assertThat(clusterContentToDelete).doesNotContain(clusterContentPSKUReject);
    }

    private ClusterMeta createClusterMeta() {
        ClusterMeta clusterMeta = new ClusterMeta();
        clusterMeta.setType(ClusterType.PSKU_EXISTS);
        clusterMeta.setStatus(ClusterStatus.REMAPPING_FINISHED);
        clusterMetaDao.insert(clusterMeta);
        return clusterMeta;
    }

    private ClusterContent createClusterContent(Long clusterMetaId, Long skuId, Long targetSkuId) {
        ClusterContent clusterContent = new ClusterContent();
        clusterContent.setClusterMetaId(clusterMetaId);
        clusterContent.setType(ClusterContentType.PSKU);
        clusterContent.setOfferId(format("%d-offer-%f", clusterMetaId, random()));
        clusterContent.setStatus(ClusterContentStatus.REMAPPED);
        clusterContent.setSkuId(skuId);
        clusterContent.setTargetSkuId(targetSkuId);
        clusterContentDao.insert(clusterContent);
        return clusterContent;
    }


}
