package ru.yandex.market.mboc.common.services.msku.sync;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.db.jooq.generated.msku.enums.SkuQualityEnum;
import ru.yandex.market.mboc.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.mboc.common.msku.KnownMboParams;
import ru.yandex.market.mboc.common.repo.bindings.pojos.MskuParameters;
import ru.yandex.market.mboc.common.services.modelstorage.ModelConverter;
import ru.yandex.market.mboc.common.services.modelstorage.XslNames;
import ru.yandex.market.mboc.common.test.RandomTestUtils;
import ru.yandex.market.mboc.common.utils.LocalizedStringUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicnumber")
public class ImportMskuServiceFromYtTest extends BaseImportMskuServiceTest {
    private static final AtomicLong IDS_GENERATOR = new AtomicLong(0L);

    protected EnhancedRandom random;

    @Before
    public void initRandom() {
        random = RandomTestUtils.createNewRandom();
    }

    @Test
    public void noMsku() {
        mockStuff("20200101_0000");
        ytImportOperations.setMskuNodes(List.of());

        List<ImportResultImpl> importResults = importMskuService.syncWithYtForTest();

        assertImportResultByCount(importResults, Map.of());
        List<Msku> mskuFromDb = mskuRepository.findAll();
        assertThat(mskuFromDb).isEmpty();
    }

    @Test
    public void newMskuOnly() {
        mockStuff("20200101_0000");
        Msku msku1 = randomMsku();
        Msku msku2 = randomMsku();
        Msku msku3 = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku1, msku2, msku3));

        List<ImportResultImpl> importResults = importMskuService.syncWithYtForTest();

        assertImportResultByCount(importResults, Map.of(
            ImportResultImpl.LineResult.INSERTED, 3L
        ));
        List<Msku> actual = mskuRepository.findAll();
        assertMsku(actual, msku1, msku2, msku3);
    }

    @Test
    public void addNewMsku() {
        mockStuff("20200101_0000");
        Msku msku1 = randomMsku();
        Msku msku2 = randomMsku();
        Msku msku3 = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku1, msku2, msku3));
        importMskuService.syncWithYtForTest();

        mockStuff("20200101_0001");
        Msku msku4 = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku1, msku2, msku3, msku4));
        List<ImportResultImpl> importResults = importMskuService.syncWithYtForTest();

        assertImportResultByCount(importResults, Map.of(
            ImportResultImpl.LineResult.INSERTED, 1L,
            ImportResultImpl.LineResult.HASH_MATCH, 3L
        ));
        List<Msku> actual = mskuRepository.findAll();
        assertMsku(actual, msku1, msku2, msku3, msku4);
    }

    @Test
    public void markDeletedMsku() {
        mockStuff("20200101_0000");
        Msku msku1 = randomMsku();
        Msku msku2 = randomMsku();
        Msku msku3 = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku1, msku2, msku3));
        importMskuService.syncWithYtForTest();

        mockStuff("20200101_0001");
        Msku msku2deleted = msku2.setDeleted(true);
        Msku msku3deleted = msku3.setDeleted(true);
        ytImportOperations.setMskuNodes(convertMsku(msku1, msku2deleted, msku3deleted));
        List<ImportResultImpl> importResults = importMskuService.syncWithYtForTest();

        assertImportResultByCount(importResults, Map.of(
            ImportResultImpl.LineResult.HASH_MATCH, 1L,
            ImportResultImpl.LineResult.UPDATED, 2L
        ));
        List<Msku> actual = mskuRepository.findAll();
        assertMsku(actual, msku1, msku2deleted, msku3deleted);
    }

    @Test
    public void testFastMskuIsNotDeleted() {
        mockStuff("20200101_0000");
        Msku msku = randomMsku();
        msku.setSkuType(SkuTypeEnum.FAST_SKU);

        ytImportOperations.setMskuNodes(convertMsku(msku));
        importMskuService.syncWithYtForTest();

        mockStuff("20200101_0001");

        ytImportOperations.setMskuNodes(Collections.emptyList());
        importMskuService.syncWithYtForTest();

        //verifying that msku4 is not deleted
        Msku actualMsku4 = mskuRepository.findById(msku.getMarketSkuId()).get();
        assertThat(actualMsku4.getDeleted()).isFalse();
    }

    @Test
    public void deleteMsku() {
        mockStuff("20200101_0000");
        Msku msku1 = randomMsku();
        Msku msku2 = randomMsku();
        Msku msku3 = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku1, msku2, msku3));
        importMskuService.syncWithYtForTest();

        mockStuff("20200101_0001");
        Msku msku2deleted = msku2.setDeleted(true);
        Msku msku3deleted = msku3.setDeleted(true);
        ytImportOperations.setMskuNodes(convertMsku(msku1));
        List<ImportResultImpl> importResults = importMskuService.syncWithYtForTest();

        assertImportResultByCount(importResults, Map.of(
            ImportResultImpl.LineResult.HASH_MATCH, 1L,
            ImportResultImpl.LineResult.UPDATED, 2L
        ));
        List<Msku> actual = mskuRepository.findAll();
        assertMsku(actual, msku1, msku2deleted, msku3deleted);
    }

    @Test
    public void updateMskuPictureUrl() {
        mockStuff("20200101_0000");
        Msku msku1 = randomMsku();
        Msku msku2 = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku1, msku2));
        importMskuService.syncWithYtForTest();

        mockStuff("20200101_0001");
        Msku msku11 = msku1.setPictureUrl(msku1.getPictureUrl() + "updated");
        ytImportOperations.setMskuNodes(convertMsku(msku11, msku2));

        List<ImportResultImpl> importResults = importMskuService.syncWithYtForTest();

        assertImportResultByCount(importResults, Map.of(
            ImportResultImpl.LineResult.HASH_MATCH, 1L,
            ImportResultImpl.LineResult.UPDATED, 1L
        ));
        List<Msku> actual = mskuRepository.findAll();
        assertMsku(actual, msku11, msku2);
    }

    @Test
    public void updateMskuCategory() {
        mockStuff("20200101_0000");
        Msku msku1 = randomMsku();
        Msku msku2 = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku1, msku2));
        importMskuService.syncWithYtForTest();

        mockStuff("20200101_0001");
        Msku msku11 = msku1.setCategoryId(msku1.getCategoryId() + 1);
        ytImportOperations.setMskuNodes(convertMsku(msku11, msku2));

        List<ImportResultImpl> importResults = importMskuService.syncWithYtForTest();

        assertImportResultByCount(importResults, Map.of(
            ImportResultImpl.LineResult.HASH_MATCH, 1L,
            ImportResultImpl.LineResult.UPDATED, 1L
        ));
        List<Msku> actual = mskuRepository.findAll();
        assertMsku(actual, msku11, msku2);
    }

    @Test
    public void updateMskuCargoTypeLmsIds() {
        mockStuff("20200101_0000");
        Msku msku = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku));
        importMskuService.syncWithYtForTest();

        mockStuff("20200101_0001");
        ModelStorage.Model.Builder modelBuilder = convertToProto(msku)
            .toBuilder();
        cargoTypeCachingService.getMboParameterIds().forEach(paramId -> {
            ModelStorage.ParameterValue value = ModelStorage.ParameterValue.newBuilder()
                .setUserId(1)
                .setParamId(paramId)
                .setBoolValue(true)
                .setXslName("1")
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setTypeId(MboParameters.ValueType.BOOLEAN_VALUE)
                .build();
            modelBuilder.addParameterValues(value);
        });
        ytImportOperations.setMskuNodes(convertProto(modelBuilder.build()));
        List<ImportResultImpl> importResults = importMskuService.syncWithYtForTest();

        assertImportResultByCount(importResults, Map.of(
            ImportResultImpl.LineResult.UPDATED, 1L
        ));
    }

    @Test
    public void updateMskuExpirDate() {
        mockStuff("20200101_0000");
        Msku msku = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku));
        importMskuService.syncWithYtForTest();

        mockStuff("20200101_0001");
        ModelStorage.Model.Builder modelBuilder = convertToProto(msku)
            .toBuilder();

        long paramId = 646313254L;
        ModelStorage.ParameterValue.Builder expirDate = ModelStorage.ParameterValue.newBuilder()
            .setUserId(1)
            .setParamId(paramId) // not same with cargotype
            .setBoolValue(true)
            .setXslName(KnownMboParams.EXPIR_DATE.mboXslName())
            .setValueType(MboParameters.ValueType.BOOLEAN)
            .setTypeId(MboParameters.ValueType.BOOLEAN_VALUE);

        modelBuilder.addParameterValues(expirDate);
        ytImportOperations.setMskuNodes(convertProto(modelBuilder.build()));
        List<ImportResultImpl> importResults = importMskuService.syncWithYtForTest();

        assertImportResultByCount(importResults, Map.of(
            ImportResultImpl.LineResult.UPDATED, 1L
        ));
    }

    @Test
    public void importSameStuff() {
        mockStuff("20200101_0000");
        Msku msku1 = randomMsku();
        Msku msku2 = randomMsku();
        Msku msku3 = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku1, msku2, msku3));
        importMskuService.syncWithYtForTest();

        mockStuff("20200101_0000");
        Msku msku4 = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku1, msku2, msku3, msku4));
        List<ImportResultImpl> importResults = importMskuService.syncWithYtForTest();

        assertImportResultByCount(importResults, Map.of());
        List<Msku> actual = mskuRepository.findAll();
        assertMsku(actual, msku1, msku2, msku3);
    }

    @Test
    public void importOlderStuff() {
        mockStuff("20200101_0000");
        Msku msku1 = randomMsku();
        Msku msku2 = randomMsku();
        Msku msku3 = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku1, msku2, msku3));
        importMskuService.syncWithYtForTest();

        mockStuff("20190101_0000");
        Msku msku4 = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku1, msku2, msku3, msku4));
        List<ImportResultImpl> importResults = importMskuService.syncWithYtForTest();

        assertImportResultByCount(importResults, Map.of());
        List<Msku> actual = mskuRepository.findAll();
        assertMsku(actual, msku1, msku2, msku3);
    }

    @Test
    public void ignoreAlreadyImported() {
        mockStuff("20200101_0000");
        Msku msku1 = randomMsku();
        Msku msku2 = randomMsku();
        Msku msku3 = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku1, msku2, msku3));
        importMskuService.syncWithYtForTest();

        mockStuff("20200101_0001");
        msku3.setPictureUrl(msku3.getPictureUrl() + "updated");
        Msku msku3updated = mskuRepository.getById(msku3.getMarketSkuId());
        msku3updated.setYtImportTs(Instant.now());
        mskuRepository.save(msku3updated);
        Msku msku4 = randomMsku();
        ytImportOperations.setMskuNodes(convertMsku(msku1, msku2, msku3, msku4));
        List<ImportResultImpl> importResults = importMskuService.syncWithYtForTest();

        assertImportResultByCount(importResults, Map.of(
            ImportResultImpl.LineResult.INSERTED, 1L,
            ImportResultImpl.LineResult.HASH_MATCH, 2L,
            ImportResultImpl.LineResult.ALREADY_IMPORTED, 1L
        ));
        List<Msku> actual = mskuRepository.findAll();
        assertMsku(actual, msku1, msku2, msku3updated, msku4);
    }

    @Test
    public void differentOrderOfParameterValuesSameHash() {
        mockStuff("20200101_0000");
        Msku msku = randomMsku();
        ModelStorage.ParameterValue value1 = ModelStorage.ParameterValue.newBuilder()
            .setUserId(1)
            .setParamId(1)
            .setOptionId(1)
            .setXslName("1")
            .setValueType(MboParameters.ValueType.ENUM)
            .setTypeId(MboParameters.ValueType.ENUM_VALUE)
            .build();
        ModelStorage.ParameterValue value2 = ModelStorage.ParameterValue.newBuilder()
            .setUserId(2)
            .setParamId(2)
            .setOptionId(2)
            .setXslName("2")
            .setValueType(MboParameters.ValueType.ENUM)
            .setTypeId(MboParameters.ValueType.ENUM_VALUE)
            .build();

        ModelStorage.Model model1 = convertToProto(msku)
            .toBuilder()
            .addParameterValues(value1)
            .addParameterValues(value2)
            .build();
        ModelStorage.Model model2 = convertToProto(msku)
            .toBuilder()
            .addParameterValues(value2)
            .addParameterValues(value1)
            .build();

        ytImportOperations.setMskuNodes(convertProto(model1));
        List<ImportResultImpl> importResults = importMskuService.syncWithYtForTest();
        assertImportResultByCount(importResults, Map.of(
            ImportResultImpl.LineResult.INSERTED, 1L
        ));

        mockStuff("20200101_0001");
        ytImportOperations.setMskuNodes(convertProto(model2));
        importResults = importMskuService.syncWithYtForTest();
        assertImportResultByCount(importResults, Map.of(
            ImportResultImpl.LineResult.HASH_MATCH, 1L
        ));
    }

    private void mockStuff(String currentYtId) {
        ytImportOperations.setCurrentYtId(currentYtId);
    }

    private YTreeMapNode convertYt(ModelStorage.Model protoModel) {
        return YTree.mapBuilder()
            .key("model_id").value(protoModel.getId())
            .key("data").value(protoModel.toByteArray())
            .buildMap();
    }

    private YTreeMapNode convertYt(Msku msku) {
        ModelStorage.Model protoModel = convertToProto(msku);
        return convertYt(protoModel);
    }

    private List<YTreeMapNode> convertMsku(List<Msku> mskus) {
        return mskus.stream().map(this::convertYt).collect(Collectors.toList());
    }

    private List<YTreeMapNode> convertMsku(Msku... mskus) {
        return this.convertMsku(Arrays.asList(mskus));
    }

    private List<YTreeMapNode> convertProto(List<ModelStorage.Model> models) {
        return models.stream().map(this::convertYt).collect(Collectors.toList());
    }

    private List<YTreeMapNode> convertProto(ModelStorage.Model... models) {
        return convertProto(Arrays.asList(models));
    }

    private Msku randomMsku() {
        return random.nextObject(
            Msku.class,
            "marketSkuId", "deleted", "mskuParameterValues", "cargoTypeLmsIds", "parameterValuesProto",
            "ytDataHash", "ytImportTimeout", "ytImportTs"
        )
            .setMarketSkuId(IDS_GENERATOR.incrementAndGet())
            .setSkuType(SkuTypeEnum.SKU)
            .setSkuQuality(SkuQualityEnum.OPERATOR)
            .setCategoryQuality(null)
            .setDeleted(false)
            .setMskuParameterValues(new MskuParameters().setCargoParameters(Map.of()))
            .setCargoTypeLmsIds()
            .setAllowPartnerContent(null);
    }

    private void assertImportResultByCount(List<ImportResultImpl> importResults,
                                           Map<ImportResultImpl.LineResult, Long> expected) {
        var countResults = importResults.stream().collect(Collectors.groupingBy(
            ImportResultImpl::getLineResult,
            Collectors.counting()
        ));
        assertThat(countResults).isEqualTo(expected);
    }

    private static ModelStorage.Model convertToProto(Msku msku) {
        ModelStorage.Model.Builder resultBuilder = ModelStorage.Model.newBuilder();
        resultBuilder.setId(msku.getMarketSkuId());
        resultBuilder.addTitles(LocalizedStringUtils.reverseConvert(msku.getTitle()));
        resultBuilder.setPublishedOnMarket(msku.getPublishedOnMarket());
        resultBuilder.setPublishedOnBlueMarket(msku.getPublishedOnBlueMarket());
        resultBuilder.setCategoryId(msku.getCategoryId());
        resultBuilder.setVendorId(msku.getVendorId());
        resultBuilder.setCurrentType(msku.getSkuType().getLiteral());
        resultBuilder.setDeleted(msku.getDeleted());
        resultBuilder.setCreatedDate(msku.getCreationTs().toEpochMilli());
        resultBuilder.setModifiedTs(msku.getModificationTs().toEpochMilli());
        if (msku.getIsSku()) {
            resultBuilder.addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(ModelConverter.IS_SKU_PARAM_ID)
                .setXslName(XslNames.IS_SKU)
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setBoolValue(true)
            );
        }
        resultBuilder.addRelations(ModelStorage.Relation.newBuilder()
            .setId(msku.getParentModelId())
            .setType(
                msku.getSkuType() == SkuTypeEnum.EXPERIMENTAL_SKU
                    ? ModelStorage.RelationType.EXPERIMENTAL_BASE_MODEL
                    : ModelStorage.RelationType.SKU_PARENT_MODEL
            )
            .setCategoryId(msku.getCategoryId())
        );
        if (msku.getVendorCodes() != null) {
            resultBuilder.addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(ModelConverter.VENDOR_CODE_PARAM_ID)
                .setXslName(XslNames.VENDOR_CODE)
                .setValueType(MboParameters.ValueType.STRING)
                .addAllStrValue(LocalizedStringUtils.reverseConvert(Arrays.asList(msku.getVendorCodes())))
            );
        }
        if (msku.getBarCodes() != null) {
            resultBuilder.addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(ModelConverter.BAR_CODE_PARAM_ID)
                .setXslName(XslNames.BAR_CODE)
                .setValueType(MboParameters.ValueType.STRING)
                .addAllStrValue(LocalizedStringUtils.reverseConvert(Arrays.asList(msku.getBarCodes())))
            );
        }
        if (msku.getSupplierId() != null) {
            resultBuilder.setSupplierId(msku.getSupplierId());
        }
        if (msku.getPictureUrl() != null) {
            resultBuilder.addPictures(
                ModelStorage.Picture.newBuilder()
                    .setUrl(msku.getPictureUrl()));
        }
        return resultBuilder.build();
    }
}
