package ru.yandex.market.psku.postprocessor.service.migration.convertor;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampUnitedOffer;
import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.P1toP2Converter;
import ru.yandex.market.ir.autogeneration.common.helpers.ParameterUtil;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration.common.util.ModelBuilder;
import ru.yandex.market.ir.autogeneration.common.util.ModelProtoUtils;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ConvertedPskuGroupDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.Tables;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuConvertStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ConvertedPskuGroup;
import ru.yandex.market.robot.db.ParameterValueComposer;

// db cleaning is handled manually in this test, because of multithreading
@Ignore
@Transactional(propagation = Propagation.NEVER)
public class Psku10toPsku20ConverterServiceTest extends BaseDBTest {

    private static final long HID = 91491;
    private static final long PSKU_ID11 = 101L;

    private static final long PSKU_ID21 = 201L;
    private static final long PSKU_ID22 = 202L;
    private static final long PSKU_ID23 = 203L;

    private static final long PMODEL_ID1 = 10L;
    private static final long PMODEL_ID2 = 20L;

    private static final int BIZ_ID_1 = 1;
    private static final int BIZ_ID_2 = 2;

    private static final String OFFER_ID_1 = "123";
    private static final String OFFER_ID_2 = "456";
    private static final String OFFER_ID_3 = "789";

    private static final long NON_EXISTING_PARAM_ID = 666L;
    private static final long PARAM_WITH_WRONG_OPTION = 667L;

    private static final long USE_NAME_AS_TITLE_PARAM_ID = 17578891L;
    private static final long USE_NAME_AS_TITLE_TRUE_OPT = 1L;
    private static final long VENDOR_OPT = 7L;
    private static final String VENDOR_OPT_VAL = "Super";
    private static final long MODEL_QUALITY_PARAM_ID = 17693310L;

    private static final long OLDEST_DATE_PARAM_ID = 7351723L;
    private static final long NON_SKU_PARAM_ID = 22222L;
    private static final String NON_SKU_PARAM_NAME = "this_is_not_sku_param";
    private static final long SKU_PARAM_ID = 33333L;
    private static final String SKU_PARAM_NAME = "this_is_sku_param";
    private static final long SKU_PARAM_DOUBLED_ID = 44444L;
    private static final String SKU_PARAM_DOUBLED_NAME = "doubled_sku_param";

    private static final long CONVERTED_PARAM_ID = 22368894L;
    private static final long CONVERTED_PARAM_TRUE_OPT = 33L;

    private static final long MULTIVALUE_PARAM_ID = 12369326L;
    private static final long MULTIVALUE_PARAM_OPT_1 = 1L;
    private static final long MULTIVALUE_PARAM_OPT_2 = 2L;
    private static final String MULTIVALUE_PARAM_NAME = "color_arm";

    private static final long GOOD_CONTENT_GROUP_ID_PARAM_ID = 100001L;
    private static final String GOOD_CONTENT_PARSABLE_GROUP_ID = "12345";
    private static final String GOOD_CONTENT_UNPARSABLE_GROUP_ID = "t-34";

    //Pictures
    private static final ModelStorage.Picture PIC1 = ModelStorage.Picture.newBuilder().setOrigMd5("pic1").setModificationDate(1L).build();
    private static final ModelStorage.Picture PIC2 = ModelStorage.Picture.newBuilder().setOrigMd5("pic2").setModificationDate(1L).build();
    private static final ModelStorage.Picture PIC3 = ModelStorage.Picture.newBuilder().setOrigMd5("pic1").setModificationDate(2L).build();
    private static final ModelStorage.Picture PIC4 = ModelStorage.Picture.newBuilder().setOrigMd5("pic4").setModificationDate(1L).build();
    private static final ModelStorage.Picture PIC5 = ModelStorage.Picture.newBuilder().setOrigMd5("pic4").setModificationDate(0L).build();
    private static final ModelStorage.Picture PIC6 = ModelStorage.Picture.newBuilder().setOrigMd5("pic6").setModificationDate(1L).build();

    @Autowired
    ConvertedPskuGroupDao convertedPskuGroupDao;

    private Psku10toPsku20ConverterService service;
    private ModelStorageHelper modelStorageHelper;
    private ModelStorageServiceMock modelStorageServiceMock;
    private CategoryDataKnowledgeMock categoryDataKnowledgeMock;

    @Before
    public void setUp() {
        modelStorageServiceMock = Mockito.spy(new ModelStorageServiceMock());
        modelStorageHelper = Mockito.spy(new ModelStorageHelper(modelStorageServiceMock, modelStorageServiceMock));
        categoryDataKnowledgeMock = new CategoryDataKnowledgeMock();
        categoryDataKnowledgeMock.addCategoryData(HID, prepareCategoryData());
        modelStorageServiceMock.putModels(
            ModelBuilder.newBuilder(PSKU_ID11, HID)
                .currentType(ModelStorage.ModelType.PARTNER_SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .withSkuParentRelation(HID, PMODEL_ID1)
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.NAME_ID)
                    .setXslName(ParameterValueComposer.NAME)
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue(String.valueOf(PSKU_ID11))
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.VENDOR_ID)
                    .setXslName("vendor")
                    .setOptionId((int) VENDOR_OPT)
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue(VENDOR_OPT_VAL)
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.BARCODE_ID)
                    .setXslName("BarCode")
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("87280500") //valid
                        .build())
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("93737412187479") //valid, but duplicate with PSKU_ID21
                        .build())
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("797266714467") //valid
                        .build())
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("123") //invalid
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.BARCODE_ID)
                    .setXslName("BarCode")
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("34") //invalid
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(NON_EXISTING_PARAM_ID)
                    .setXslName("ThisNotExists")
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("34")
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(PARAM_WITH_WRONG_OPTION)
                    .setXslName("ParamWithWrongOption")
                    .setOptionId(123) // does not exists
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("wrong")
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(NON_SKU_PARAM_ID)
                    .setXslName(NON_SKU_PARAM_NAME)
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("17566")
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(SKU_PARAM_DOUBLED_ID)
                    .setXslName(SKU_PARAM_DOUBLED_NAME)
                    .setModificationDate(1L)
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("old_doubled")
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(MULTIVALUE_PARAM_ID)
                    .setXslName(MULTIVALUE_PARAM_NAME)
                    .setModificationDate(1L)
                    .setOptionId((int) MULTIVALUE_PARAM_OPT_1)
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(MULTIVALUE_PARAM_ID)
                    .setXslName(MULTIVALUE_PARAM_NAME)
                    .setModificationDate(1L)
                    .setOptionId((int) MULTIVALUE_PARAM_OPT_2)
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(MULTIVALUE_PARAM_ID)
                    .setXslName(MULTIVALUE_PARAM_NAME)
                    .setModificationDate(2L)
                    .setOptionId((int) MULTIVALUE_PARAM_OPT_2)
                    .build())
                .picture(PIC1).picture(PIC2).picture(PIC3).picture(PIC4).picture(PIC5).picture(PIC6)
                .build(),
            ModelBuilder.newBuilder(PMODEL_ID1, HID)
                .currentType(ModelStorage.ModelType.PARTNER)
                .source(ModelStorage.ModelType.PARTNER.name())
                .withSkuRelations(HID, PSKU_ID11)
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.NAME_ID)
                    .setXslName(ParameterValueComposer.NAME)
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue(String.valueOf(PMODEL_ID1))
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.VENDOR_ID)
                    .setXslName("vendor")
                    .setOptionId((int) VENDOR_OPT)
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue(VENDOR_OPT_VAL)
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(NON_EXISTING_PARAM_ID)
                    .setXslName("ThisNotExists")
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("34")
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(PARAM_WITH_WRONG_OPTION)
                    .setXslName("ParamWithWrongOption")
                    .setOptionId(123) // does not exists
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("wrong")
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(SKU_PARAM_ID)
                    .setXslName(SKU_PARAM_NAME)
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("99087")
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(SKU_PARAM_DOUBLED_ID)
                    .setXslName(SKU_PARAM_DOUBLED_NAME)
                    .setModificationDate(2L) // fresher than SKU_PARAM_DOUBLED_ID in PSKU_ID11
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("new_doubled")
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(MULTIVALUE_PARAM_ID)
                    .setXslName(MULTIVALUE_PARAM_NAME)
                    .setModificationDate(1L)
                    .setOptionId((int) MULTIVALUE_PARAM_OPT_1)
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(MULTIVALUE_PARAM_ID)
                    .setXslName(MULTIVALUE_PARAM_NAME)
                    .setModificationDate(1L)
                    .setOptionId((int) MULTIVALUE_PARAM_OPT_2)
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(MULTIVALUE_PARAM_ID)
                    .setXslName(MULTIVALUE_PARAM_NAME)
                    .setModificationDate(2L)
                    .setOptionId((int) MULTIVALUE_PARAM_OPT_2)
                    .build())
                .build(),
            // second group
            ModelBuilder.newBuilder(PSKU_ID21, HID)
                .currentType(ModelStorage.ModelType.PARTNER_SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .withSkuParentRelation(HID, PMODEL_ID2)
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.NAME_ID)
                    .setXslName(ParameterValueComposer.NAME)
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue(String.valueOf(PSKU_ID21))
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.VENDOR_ID)
                    .setXslName("vendor")
                    .setOptionId((int) VENDOR_OPT)
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue(VENDOR_OPT_VAL)
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.BARCODE_ID)
                    .setXslName("BarCode")
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("8147933775581")
                        .build())
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("93737412187479") // duplicated with PSKU_ID11
                        .build())
                    .build())
                .build(),
            ModelBuilder.newBuilder(PSKU_ID22, HID)
                .currentType(ModelStorage.ModelType.PARTNER_SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .withSkuParentRelation(HID, PMODEL_ID2)
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.NAME_ID)
                    .setXslName(ParameterValueComposer.NAME)
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue(String.valueOf(PSKU_ID22))
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.VENDOR_ID)
                    .setXslName("vendor")
                    .setOptionId((int) VENDOR_OPT)
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue(VENDOR_OPT_VAL)
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.BARCODE_ID)
                    .setXslName("BarCode")
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("1234567")
                        .build())
                    .build())
                .build(),
            ModelBuilder.newBuilder(PSKU_ID23, HID)
                .currentType(ModelStorage.ModelType.PARTNER_SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .withSkuParentRelation(HID, PMODEL_ID2)
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.NAME_ID)
                    .setXslName(ParameterValueComposer.NAME)
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue(String.valueOf(PSKU_ID23))
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.VENDOR_ID)
                    .setXslName("vendor")
                    .setOptionId((int) VENDOR_OPT)
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue(VENDOR_OPT_VAL)
                        .build())
                    .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(ParameterValueComposer.BARCODE_ID)
                    .setXslName("BarCode")
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue("1234567")
                        .build())
                    .build())
                .build(),
            ModelBuilder.newBuilder(PMODEL_ID2, HID)
                .currentType(ModelStorage.ModelType.PARTNER)
                .source(ModelStorage.ModelType.PARTNER.name())
                .withSkuRelations(HID, PSKU_ID21)
                .withSkuRelations(HID, PSKU_ID22)
                .withSkuRelations(HID, PSKU_ID23)
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                    .setParamId(GOOD_CONTENT_GROUP_ID_PARAM_ID)
                    .setXslName(ParameterUtil.GOOD_CONTENT_GROUP_ID_PARAM_NAME)
                    .addStrValue(ModelStorage.LocalizedString.newBuilder()
                        .setValue(GOOD_CONTENT_PARSABLE_GROUP_ID)
                        .build())
                    .build())
                .build()
        );
        service = new Psku10toPsku20ConverterService(modelStorageHelper,
            categoryDataKnowledgeMock, convertedPskuGroupDao, new P1toP2Converter(modelStorageHelper));
        // clear table
        convertedPskuGroupDao.dsl().deleteFrom(Tables.CONVERTED_PSKU_GROUP).execute();
    }

    @Test
    public void testPModelWithOnePSkuConverted() {
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID11)
        );
        Map<Long, Integer> convertMap = service.convert(offers, false);
        Assertions.assertThat(convertMap.keySet()).containsExactly(PSKU_ID11);
        Assertions.assertThat(convertMap.values()).containsExactly(Math.toIntExact(PMODEL_ID1));

        checkPSkuConvertedProperly(PSKU_ID11, PMODEL_ID1);
        checkGroupInfoStored(PSKU_ID11, PMODEL_ID1, BIZ_ID_1, OFFER_ID_1, PskuConvertStatus.OK, false);
    }

    @Test
    public void testWhenDuplicatePicturesMostRecentAreUsed() {
        List<Long> pSkusIds = ImmutableList.of(PSKU_ID11);
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID11)
        );
        Map<Long, Integer> map = service.convert(offers, false);
        Assertions.assertThat(map.keySet()).containsExactlyInAnyOrder(PSKU_ID11);
        int groupId = Math.toIntExact(PMODEL_ID1);
        Assertions.assertThat(map.values()).containsExactly(groupId);
        Map<Long, ModelStorage.Model> convertedPSkus = modelStorageServiceMock.getModelsMap().values().stream()
            .filter(model -> pSkusIds.contains(model.getId()))
            .collect(Collectors.toMap(ModelStorage.Model::getId, Function.identity()));
        Assertions.assertThat(convertedPSkus.get(PSKU_ID11).getPicturesList()).containsExactly(PIC2, PIC3, PIC4, PIC6);
    }

    @Test
    public void testPModelWithOnePSkuConvertedFail() {
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID11)
        );
        Mockito.doThrow(new RuntimeException("some exception"))
            .when(modelStorageHelper)
            .executeSaveModelRequest(Mockito.any());
        Map<Long, Integer> convertMap = service.convert(offers, false);
        Assertions.assertThat(convertMap.keySet()).isEmpty();

        checkGroupInfoStored(PSKU_ID11, PMODEL_ID1, BIZ_ID_1, OFFER_ID_1, PskuConvertStatus.FAILED, false, "EX:some " +
            "exception");
    }

    @Test
    public void testPModelWithOnePSkuConvertedWithValidationError() {
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID11)
        );

        ModelCardApi.SaveModelsGroupResponse response = ModelCardApi.SaveModelsGroupResponse.newBuilder()
            .addResponse(
                ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                    .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                    .addAllRequestedModelsStatuses(ImmutableList.of(
                        newOperationStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR,
                            PSKU_ID11, ModelStorage.ModelType.PARTNER_SKU, ModelStorage.ModelType.PARTNER_SKU),
                        newOperationStatus(ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP,
                            PMODEL_ID1, ModelStorage.ModelType.PARTNER, ModelStorage.ModelType.PARTNER)
                    ))
                    .addValidationError(ModelStorage.ValidationError.newBuilder()
                        .setModelId(PSKU_ID11)
                        .setType(ModelStorage.ValidationErrorType.DUPLICATE_BARCODE)
                        .build())
                    .build()
            ).build();

        Mockito.doReturn(response)
            .when(modelStorageServiceMock)
            .saveModelsGroup(Mockito.any());
        Map<Long, Integer> convertMap = service.convert(offers, false);
        Assertions.assertThat(convertMap.keySet()).isEmpty();

        checkGroupInfoStored(PSKU_ID11, PMODEL_ID1, BIZ_ID_1, OFFER_ID_1, PskuConvertStatus.FAILED, false,
            "VE:DUPLICATE_BARCODE");
    }

    @Test
    public void testPModelWithOnePSkuConvertedWithParentValidationError() {
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID11)
        );

        ModelCardApi.SaveModelsGroupResponse response = ModelCardApi.SaveModelsGroupResponse.newBuilder()
            .addResponse(
                ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                    .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                    .addAllRequestedModelsStatuses(ImmutableList.of(
                        ModelStorage.OperationStatus.newBuilder()
                            .setType(ModelStorage.OperationType.CREATE)
                            .setStatus(ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP)
                            .setModelId(PSKU_ID11)
                            .setModel(ModelStorage.Model.newBuilder()
                                .setId(PSKU_ID11)
                                .setCurrentType(ModelStorage.ModelType.PARTNER_SKU.name())
                                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                                .addRelations(ModelStorage.Relation.newBuilder()
                                    .setId(PMODEL_ID1)
                                    .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                                    .build())
                                .build())
                            .build(),
                        newOperationStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR,
                            PMODEL_ID1, ModelStorage.ModelType.PARTNER, ModelStorage.ModelType.PARTNER)
                    ))
                    .addValidationError(ModelStorage.ValidationError.newBuilder()
                        .setModelId(PMODEL_ID1)
                        .setType(ModelStorage.ValidationErrorType.MISSING_VENDOR)
                        .build())
                    .build()
            ).build();

        Mockito.doReturn(response)
            .when(modelStorageServiceMock)
            .saveModelsGroup(Mockito.any());
        Map<Long, Integer> convertMap = service.convert(offers, false);
        Assertions.assertThat(convertMap.keySet()).isEmpty();

        checkGroupInfoStored(PSKU_ID11, PMODEL_ID1, BIZ_ID_1, OFFER_ID_1, PskuConvertStatus.FAILED, false,
            "PVE:MISSING_VENDOR");
    }

    @Test
    public void testPModelWithOnePSkuConvertedUnknownStatus() {
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID11)
        );
        ModelCardApi.SaveModelsGroupResponse response = ModelCardApi.SaveModelsGroupResponse.newBuilder()
            .addResponse(
                ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                    .setStatus(ModelStorage.OperationStatusType.MODEL_MODIFIED)
                    .addAllRequestedModelsStatuses(ImmutableList.of(
                        newOperationStatus(ModelStorage.OperationStatusType.MODEL_MODIFIED,
                            PSKU_ID11, ModelStorage.ModelType.PARTNER_SKU, ModelStorage.ModelType.PARTNER_SKU),
                        newOperationStatus(ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP,
                            PMODEL_ID1, ModelStorage.ModelType.PARTNER, ModelStorage.ModelType.PARTNER)
                    ))
                    .build()
            ).build();
        Mockito.doReturn(response)
            .when(modelStorageServiceMock)
            .saveModelsGroup(Mockito.any());
        Map<Long, Integer> convertMap = service.convert(offers, false);
        Assertions.assertThat(convertMap.keySet()).isEmpty();

        checkGroupInfoStored(PSKU_ID11, PMODEL_ID1, BIZ_ID_1, OFFER_ID_1, PskuConvertStatus.FAILED, false,
            "UE:MODEL_MODIFIED");
    }

    @Test
    public void testPModelWithOnePSkuConvertedUnknownStatusParent() {
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID11)
        );
        ModelCardApi.SaveModelsGroupResponse response = ModelCardApi.SaveModelsGroupResponse.newBuilder()
            .addResponse(
                ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                    .setStatus(ModelStorage.OperationStatusType.MODEL_MODIFIED)
                    .addAllRequestedModelsStatuses(ImmutableList.of(
                        ModelStorage.OperationStatus.newBuilder()
                            .setType(ModelStorage.OperationType.CREATE)
                            .setStatus(ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP)
                            .setModelId(PSKU_ID11)
                            .setModel(ModelStorage.Model.newBuilder()
                                .setId(PSKU_ID11)
                                .setCurrentType(ModelStorage.ModelType.PARTNER_SKU.name())
                                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                                .addRelations(ModelStorage.Relation.newBuilder()
                                    .setId(PMODEL_ID1)
                                    .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                                    .build())
                                .build())
                            .build(),
                        newOperationStatus(ModelStorage.OperationStatusType.MODEL_MODIFIED,
                            PMODEL_ID1, ModelStorage.ModelType.PARTNER, ModelStorage.ModelType.PARTNER)
                    ))
                    .build()
            ).build();
        Mockito.doReturn(response)
            .when(modelStorageServiceMock)
            .saveModelsGroup(Mockito.any());
        Map<Long, Integer> convertMap = service.convert(offers, false);
        Assertions.assertThat(convertMap.keySet()).isEmpty();
        checkGroupInfoStored(PSKU_ID11, PMODEL_ID1, BIZ_ID_1, OFFER_ID_1, PskuConvertStatus.FAILED, false,
            "PE:MODEL_MODIFIED");
    }

    @Test
    public void testBarcodesHandledSingleInBatch() {
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID11)
        );
        service.convert(offers, false);
        checkHasBarcodes(PSKU_ID11, "87280500", "797266714467");
        checkHasFormerBarcodes(PSKU_ID11, "93737412187479");
    }

    @Test
    public void testBarcodesHandledMultipleInBatch() {
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID11),
            newUnitedOffer(BIZ_ID_1, OFFER_ID_2, PSKU_ID21)
        );
        service.convert(offers, false);
        checkHasBarcodes(PSKU_ID11, "87280500", "797266714467");
        checkHasBarcodes(PSKU_ID21, "8147933775581");
        // common duplicated barcode should be placed to former
        checkHasFormerBarcodes(PSKU_ID11, "93737412187479");
        checkHasFormerBarcodes(PSKU_ID21, "93737412187479");
    }

    @Test
    public void testVendorLeft() {
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID11)
        );
        service.convert(offers, false);
        checkHasVendor(PSKU_ID11, VENDOR_OPT_VAL);
        checkHasVendor(PMODEL_ID1, VENDOR_OPT_VAL);
    }

    @Test
    public void testParameterSeparationForModelAndSku() {
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID11)
        );
        service.convert(offers, false);
        // psku
        checkHasParam(PSKU_ID11, SKU_PARAM_ID);
        checkDoesNotHaveParam(PSKU_ID11, NON_SKU_PARAM_ID);
        checkHasParam(PSKU_ID11, SKU_PARAM_DOUBLED_ID);
        checkModelHasStrValues(PSKU_ID11, SKU_PARAM_DOUBLED_ID, SKU_PARAM_DOUBLED_NAME, "new_doubled");
        // pmodel
        checkHasParam(PMODEL_ID1, NON_SKU_PARAM_ID);
        checkDoesNotHaveParam(PMODEL_ID1, SKU_PARAM_ID);
    }

    @Test
    public void testParameterValidationFiltersUnexistingParams() {
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID11)
        );
        service.convert(offers, false);
        checkDoesNotHaveParam(PSKU_ID11, NON_EXISTING_PARAM_ID);
        checkDoesNotHaveParam(PMODEL_ID1, NON_EXISTING_PARAM_ID);
    }

    @Test
    public void testParameterValidationFiltersParamsWithWrongOption() {
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID11)
        );
        service.convert(offers, false);
        checkDoesNotHaveParam(PSKU_ID11, PARAM_WITH_WRONG_OPTION);
        checkDoesNotHaveParam(PMODEL_ID1, PARAM_WITH_WRONG_OPTION);
    }

    @Test
    public void testPModelWithMultiplePSkusInOneBatch() {
        List<Long> pSkusIds = ImmutableList.of(PSKU_ID21, PSKU_ID22, PSKU_ID23);
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID21),
            newUnitedOffer(BIZ_ID_2, OFFER_ID_2, PSKU_ID22),
            newUnitedOffer(BIZ_ID_2, OFFER_ID_3, PSKU_ID23)
        );
        Map<Long, Integer> map = service.convert(offers, false);
        Assertions.assertThat(map.keySet()).containsExactlyInAnyOrder(PSKU_ID21, PSKU_ID22, PSKU_ID23);
        int groupId = Math.toIntExact(PMODEL_ID2);
        Assertions.assertThat(map.values()).containsExactly(groupId, groupId, groupId);
        Map<Long, ModelStorage.Model> convertedPSkus = modelStorageServiceMock.getModelsMap().values().stream()
            .filter(model -> pSkusIds.contains(model.getId()))
            .collect(Collectors.toMap(ModelStorage.Model::getId, Function.identity()));
        checkPSkusGroupConvertedProperly(pSkusIds, convertedPSkus, PMODEL_ID2,
            Integer.parseInt(GOOD_CONTENT_PARSABLE_GROUP_ID));
        checkGroupInfoStored(PSKU_ID21, PMODEL_ID2, BIZ_ID_1, OFFER_ID_1, PskuConvertStatus.OK, false);
        checkGroupInfoStored(PSKU_ID22, PMODEL_ID2, BIZ_ID_2, OFFER_ID_2, PskuConvertStatus.OK, false);
        checkGroupInfoStored(PSKU_ID23, PMODEL_ID2, BIZ_ID_2, OFFER_ID_3, PskuConvertStatus.OK, false);
    }

    @Test
    public void testPModelWithMultiplePSkusNotInOneBatch() {
        List<Long> pSkusIds = ImmutableList.of(PSKU_ID21, PSKU_ID22, PSKU_ID23);
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID21),
            newUnitedOffer(BIZ_ID_2, OFFER_ID_2, PSKU_ID22)
            // doesn't have PSKU_ID23 in this call
        );
        Map<Long, Integer> convertMap = service.convert(offers, false);
        Assertions.assertThat(convertMap.keySet()).containsExactlyInAnyOrder(PSKU_ID21, PSKU_ID22, PSKU_ID23);
        int groupId = Math.toIntExact(PMODEL_ID2);
        Assertions.assertThat(convertMap.values()).containsExactly(groupId, groupId, groupId);
        Map<Long, ModelStorage.Model> convertedPSkus = modelStorageServiceMock.getModelsMap().values().stream()
            .filter(model -> pSkusIds.contains(model.getId()))
            .collect(Collectors.toMap(ModelStorage.Model::getId, Function.identity()));
        checkPSkusGroupConvertedProperly(pSkusIds, convertedPSkus, PMODEL_ID2,
            Integer.parseInt(GOOD_CONTENT_PARSABLE_GROUP_ID));
        checkGroupInfoStored(PSKU_ID21, PMODEL_ID2, BIZ_ID_1, OFFER_ID_1, PskuConvertStatus.OK, false);
        checkGroupInfoStored(PSKU_ID22, PMODEL_ID2, BIZ_ID_2, OFFER_ID_2, PskuConvertStatus.OK, false);
        // the PSKU_ID23 should be stored partially
        checkGroupInfoStored(PSKU_ID23, PMODEL_ID2, 0, null, PskuConvertStatus.OK, true);

        // now PSKU_ID23 arrives to new call
        List<DataCampUnitedOffer.UnitedOffer> offers2 = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_3, PSKU_ID23)
        );
        // convert map should be the same
        Map<Long, Integer> convertMap2 = service.convert(offers2, false);
        Mockito.verify(modelStorageHelper, Mockito.times(1))
            .executeSaveModelRequest(Mockito.any()); // model storage save called only 1 time (in previous batch)
        Assertions.assertThat(convertMap2.keySet()).containsExactlyInAnyOrder(PSKU_ID23); // PSKU_ID21, PSKU_ID22
        // already converted
        // and converted models should be the same OK
        Map<Long, ModelStorage.Model> convertedPSkus2 = modelStorageServiceMock.getModelsMap().values().stream()
            .filter(model -> pSkusIds.contains(model.getId()))
            .collect(Collectors.toMap(ModelStorage.Model::getId, Function.identity()));
        long pModelId = ModelProtoUtils.getParentModelIdOrFail(convertedPSkus2.get(PSKU_ID23));
        Assertions.assertThat(convertMap2.values()).containsExactly(Math.toIntExact(pModelId));
        checkPSkusGroupConvertedProperly(pSkusIds, convertedPSkus2, PMODEL_ID2,
            Integer.parseInt(GOOD_CONTENT_PARSABLE_GROUP_ID));
        // all pskus should be fully stored
        checkGroupInfoStored(PSKU_ID21, PMODEL_ID2, BIZ_ID_1, OFFER_ID_1, PskuConvertStatus.OK, false);
        checkGroupInfoStored(PSKU_ID22, PMODEL_ID2, BIZ_ID_2, OFFER_ID_2, PskuConvertStatus.OK, false);
        checkGroupInfoStored(PSKU_ID23, PMODEL_ID2, BIZ_ID_1, OFFER_ID_3, PskuConvertStatus.OK, false);
    }

    @Test
    public void testPModelWithMultiplePSkusWhenCantParseGroupId() {
        List<Long> pSkusIds = ImmutableList.of(PSKU_ID21, PSKU_ID22, PSKU_ID23);
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID21),
            newUnitedOffer(BIZ_ID_2, OFFER_ID_2, PSKU_ID22),
            newUnitedOffer(BIZ_ID_2, OFFER_ID_3, PSKU_ID23)
        );
        ModelStorage.Model pModel = modelStorageHelper.findModel(PMODEL_ID2).orElseThrow(
            () -> new RuntimeException("pmodel is not found in storage by id " + PMODEL_ID2)
        );
        List<ModelStorage.ParameterValue> paramsWithoutGroupId = pModel.getParameterValuesList().stream()
            .filter(pv -> !pv.getXslName().equals(ParameterUtil.GOOD_CONTENT_GROUP_ID_PARAM_NAME))
            .collect(Collectors.toList());
        paramsWithoutGroupId.add(ModelStorage.ParameterValue.newBuilder()
            .setParamId(GOOD_CONTENT_GROUP_ID_PARAM_ID)
            .setXslName(ParameterUtil.GOOD_CONTENT_GROUP_ID_PARAM_NAME)
            .addStrValue(ModelStorage.LocalizedString.newBuilder()
                .setValue(GOOD_CONTENT_UNPARSABLE_GROUP_ID)
                .build())
            .build());
        ModelStorage.Model newPModel =
            pModel.toBuilder().clearParameterValues().addAllParameterValues(paramsWithoutGroupId).build();
        modelStorageServiceMock.saveModels(ModelStorage.SaveModelsRequest.newBuilder()
            .addModels(newPModel)
            .build());

        Map<Long, Integer> map = service.convert(offers, false);
        Assertions.assertThat(map.keySet()).containsExactlyInAnyOrder(PSKU_ID21, PSKU_ID22, PSKU_ID23);
        int groupId = Math.toIntExact(PMODEL_ID2);
        Assertions.assertThat(map.values()).containsExactly(groupId, groupId, groupId);
        Map<Long, ModelStorage.Model> convertedPSkus = modelStorageServiceMock.getModelsMap().values().stream()
            .filter(model -> pSkusIds.contains(model.getId()))
            .collect(Collectors.toMap(ModelStorage.Model::getId, Function.identity()));
        checkPSkusGroupConvertedProperly(pSkusIds, convertedPSkus, PMODEL_ID2, (int) PMODEL_ID2); // model id as group id
        checkGroupInfoStored(PSKU_ID21, PMODEL_ID2, BIZ_ID_1, OFFER_ID_1, PskuConvertStatus.OK, false);
        checkGroupInfoStored(PSKU_ID22, PMODEL_ID2, BIZ_ID_2, OFFER_ID_2, PskuConvertStatus.OK, false);
        checkGroupInfoStored(PSKU_ID23, PMODEL_ID2, BIZ_ID_2, OFFER_ID_3, PskuConvertStatus.OK, false);
    }

    @Test
    public void testMultiValuesDeduplicated() {
        List<DataCampUnitedOffer.UnitedOffer> offers = ImmutableList.of(
            newUnitedOffer(BIZ_ID_1, OFFER_ID_1, PSKU_ID11)
        );
        service.convert(offers, false);
        checkMultiValuesDeduplicated(PSKU_ID11);
        checkMultiValuesDeduplicated(PMODEL_ID1);

        ModelStorage.Model model = modelStorageHelper.findModel(PSKU_ID11).orElseThrow(() ->
            new RuntimeException("Model not found"));
        MboParameters.Parameter categoryParam = categoryDataKnowledgeMock.getCategoryData(HID)
            .getParamById(MULTIVALUE_PARAM_ID);
        Map<Object, List<ModelStorage.ParameterValue>> optToParam = model.getParameterValuesList()
            .stream()
            .filter(pv -> pv.getParamId() == MULTIVALUE_PARAM_ID)
            .collect(Collectors.groupingBy(p -> ParameterUtil.extractValueFromParam(categoryParam, p)));

        List<ModelStorage.ParameterValue> multiPv2 = optToParam.get((int) MULTIVALUE_PARAM_OPT_2);
        Assertions.assertThat(multiPv2.get(0).getModificationDate()).isEqualTo(2L);
    }

    private void checkHasParam(long modelId, long paramId) {
        hasParam(modelId, paramId, true);
    }

    private void checkDoesNotHaveParam(long modelId, long paramId) {
        hasParam(modelId, paramId, false);
    }

    private void hasParam(long modelId, long paramId, boolean has) {
        ModelStorage.Model model = modelStorageHelper.findModel(modelId).orElseThrow(() ->
            new RuntimeException("Model not found"));
        List<ModelStorage.ParameterValue> params = model.getParameterValuesList()
            .stream()
            .filter(pv -> pv.getParamId() == paramId)
            .collect(Collectors.toList());
        if (has) {
            Assertions.assertThat(params).as("Check model has target param").isNotEmpty();
            Assertions.assertThat(params.size()).as("Check there is no param duplicates").isEqualTo(1);
        } else {
            Assertions.assertThat(params).isEmpty();
        }
    }

    private void checkMultiValuesDeduplicated(long modelId) {
        Set<Long> multiIds = categoryDataKnowledgeMock.getCategoryData(HID).getParameterList()
            .stream()
            .filter(MboParameters.Parameter::getMultivalue)
            .map(MboParameters.Parameter::getId)
            .collect(Collectors.toSet());
        ModelStorage.Model model = modelStorageHelper.findModel(modelId).orElseThrow(() ->
            new RuntimeException("Model not found"));
        List<ModelStorage.ParameterValue> multiParams = model.getParameterValuesList()
            .stream()
            .filter(pv -> multiIds.contains(pv.getParamId()))
            .collect(Collectors.toList());

        Map<Long, List<ModelStorage.ParameterValue>> paramIdToParams = multiParams.stream()
            .collect(Collectors.groupingBy(
                ModelStorage.ParameterValue::getParamId
            ));

        paramIdToParams.forEach((paramId, params) -> {
            MboParameters.Parameter categoryParam = categoryDataKnowledgeMock.getCategoryData(HID)
                .getParamById(paramId);
            Set<Object> seenValues = new HashSet<>();
            for (ModelStorage.ParameterValue pv : params) {
                Object v = ParameterUtil.extractValueFromParam(categoryParam, pv);
                Assertions.assertThat(seenValues)
                    .as("Check duplicates for multivalue " + paramId + " options")
                    .doesNotContain(v);
                seenValues.add(v);
            }
        });
    }

    private void checkHasVendor(long modelId, String name) {
        ModelStorage.Model pSku = modelStorageHelper.findModel(modelId).orElseThrow(() ->
            new RuntimeException("Model not found"));
        List<ModelStorage.ParameterValue> vendorParams = pSku.getParameterValuesList()
            .stream()
            .filter(pv -> pv.getParamId() == ParameterValueComposer.VENDOR_ID)
            .collect(Collectors.toList());
        Assertions.assertThat(vendorParams).containsExactly(ModelStorage.ParameterValue.newBuilder()
            .setParamId(ParameterValueComposer.VENDOR_ID)
            .setXslName("vendor")
            .setOptionId((int) VENDOR_OPT)
            .addStrValue(ModelStorage.LocalizedString.newBuilder()
                .setValue(name)
                .build())
            .build());
    }

    private void checkHasBarcodes(long modelId, String... barcodes) {
        checkModelHasStrValues(modelId, ParameterValueComposer.BARCODE_ID,
            ParameterValueComposer.BARCODE, barcodes);
    }

    private void checkHasFormerBarcodes(long modelId, String... formerBarcodes) {
        checkModelHasStrValues(modelId, ParameterUtil.FORMER_BAR_CODE_PARAM_ID,
            ParameterUtil.FORMER_BAR_CODE_PARAM_NAME, formerBarcodes);
    }

    private void checkModelHasStrValues(long modelId, long paramId, String xlsName, String... values) {
        ModelStorage.Model model = modelStorageHelper.findModel(modelId).orElseThrow(() ->
            new RuntimeException("Model not found"));
        List<ModelStorage.ParameterValue> barcodeParams = model.getParameterValuesList()
            .stream()
            .filter(pv -> pv.getParamId() == paramId)
            .filter(pv -> pv.getXslName().equals(xlsName))
            .collect(Collectors.toList());
        Assertions.assertThat(barcodeParams)
            .flatExtracting(p -> p.getStrValueList().stream().
                map(ModelStorage.LocalizedString::getValue).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(values);
    }

    private void checkPSkusGroupConvertedProperly(List<Long> pSkusIds,
                                                  Map<Long, ModelStorage.Model> convertedPSkus,
                                                  long oldPModel,
                                                  Integer expectedGroupId) {
        Assertions.assertThat(convertedPSkus.size())
            .as("All pSkus should be present in storage")
            .isEqualTo(pSkusIds.size());
        // check first psku is left with old p-model
        long minIdPSku = pSkusIds.stream().min(Comparator.comparingLong(i -> i)).orElseThrow(
            () -> new RuntimeException("PSkus min id is not found"));
        long minParentId = ModelProtoUtils.getParentModelId(convertedPSkus.get(minIdPSku)).orElseThrow(
            () -> new RuntimeException("Parent id for min psku is not set " + minIdPSku)
        );
        Assertions.assertThat(minParentId)
            .as("PSku " + minIdPSku + "with min id should have as parent old model with id " + oldPModel)
            .isEqualTo(oldPModel);
        checkPSkuConvertedProperly(minIdPSku, minParentId);
        checkGroupIdSet(oldPModel, expectedGroupId);

        // check other pskus
        convertedPSkus.remove(minIdPSku);
        convertedPSkus.forEach((pSkuId, pSku) -> {
            Optional<Long> maybePModelId = ModelProtoUtils.getParentModelId(pSku);
            Assertions.assertThat(maybePModelId)
                .as("Check parent id is present for " + pSkuId)
                .isPresent();
            long pModelId = maybePModelId.get();
            checkPSkuConvertedProperly(pSkuId, pModelId);
            checkGroupIdSet(pModelId, expectedGroupId);
        });
    }

    private void checkGroupIdSet(long pModelId, Integer expectedGroupId) {
        ModelStorage.Model model = modelStorageHelper.findModel(pModelId).orElseThrow(
            () -> new RuntimeException("pmodel is not found in storage by id " + pModelId)
        );
        Integer groupId = ModelProtoUtils.getGroupId(model);
        Assertions.assertThat(groupId).isEqualTo(expectedGroupId);
    }

    private void checkGroupInfoStored(long pSkuId,
                                      long oldPModelId,
                                      int businessId,
                                      String offerId,
                                      PskuConvertStatus status,
                                      boolean partial) {
        checkGroupInfoStored(pSkuId, oldPModelId, businessId, offerId, status, partial, null);
    }


    private void checkGroupInfoStored(long pSkuId,
                                      long oldPModelId,
                                      int businessId,
                                      String offerId,
                                      PskuConvertStatus status,
                                      boolean partial,
                                      String failMessage) {
        List<ConvertedPskuGroup> convertedPskuGroups = convertedPskuGroupDao.fetchByPskuId(pSkuId);
        Assertions.assertThat(convertedPskuGroups.size())
            .as("Check group info stored properly for psku " + pSkuId)
            .isEqualTo(1);
        ConvertedPskuGroup convertedPskuGroup = convertedPskuGroups.get(0);
        SoftAssertions.assertSoftly(softly -> {
            if (partial) {
                Assertions.assertThat(convertedPskuGroup.getBusinessId())
                    .as("Business id").isEqualTo(null);
                Assertions.assertThat(convertedPskuGroup.getOfferId())
                    .as("OfferId").isEqualTo(null);
            } else {
                Assertions.assertThat(convertedPskuGroup.getBusinessId())
                    .as("Business id").isEqualTo(businessId);
                Assertions.assertThat(convertedPskuGroup.getOfferId())
                    .as("OfferId").isEqualTo(offerId);
            }
            Assertions.assertThat(convertedPskuGroup.getGroupId())
                .as("PModel id should be stored as group id")
                .isEqualTo(Math.toIntExact(oldPModelId));
            Assertions.assertThat(convertedPskuGroup.getConvertStatus())
                .as("Convert status")
                .isEqualTo(status);
            Assertions.assertThat(convertedPskuGroup.getFailedReason())
                .as("Failed reason message")
                .isEqualTo(failMessage);
        });
    }

    private void checkPSkuConvertedProperly(long pSkuId, long pModelId) {
        ModelStorage.Model pSku = modelStorageHelper.findModel(pSkuId).orElseThrow(
            () -> new RuntimeException("PSku is not found in storage by id " + pSkuId)
        );
        Assertions.assertThat(pSku.getCurrentType()).isEqualTo(ModelStorage.ModelType.SKU.name());
        Assertions.assertThat(pSku.getSourceType()).isEqualTo(ModelStorage.ModelType.PARTNER_SKU.name());

        // check relation to parent
        Optional<Long> maybePModelId = ModelProtoUtils.getParentModelId(pSku);
        Assertions.assertThat(maybePModelId)
            .as("Check parent id is present for pSku %d", pSkuId)
            .isPresent();
        Assertions.assertThat(maybePModelId.get())
            .as("Check parent id for pSku %d is %d", pSkuId, maybePModelId.get())
            .isEqualTo(pModelId);

        ModelStorage.Model pModel = modelStorageHelper.findModel(pModelId).orElseThrow(
            () -> new RuntimeException("PModel is not found in storage by id " + pModelId)
        );
        Assertions.assertThat(pModel.getCurrentType()).isEqualTo(ModelStorage.ModelType.GURU.name());
        Assertions.assertThat(pModel.getSourceType()).isEqualTo(ModelStorage.ModelType.PARTNER.name());

        // check relation to psku
        Assertions.assertThat(ModelProtoUtils.getUniqueRelatedSkuIds(pModel))
            .as("Check pModel %d has relation to it's pSku %d only", pModelId, pSkuId)
            .containsExactly(pSkuId);

        Optional<ModelStorage.ParameterValue> convertedMarkParam = pSku.getParameterValuesList().stream()
            .filter(p -> p.getParamId() == CONVERTED_PARAM_ID)
            .findFirst();
        Assertions.assertThat(convertedMarkParam).isPresent();
        Assertions.assertThat(convertedMarkParam.get().getOptionId()).isEqualTo(CONVERTED_PARAM_TRUE_OPT);
    }

    private CategoryData prepareCategoryData() {
        return CategoryData.build(MboParameters.Category.newBuilder()
            .setHid(HID)
            .setLeaf(true)
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.VENDOR_ID)
                .setXslName(CategoryData.VENDOR)
                .addOption(MboParameters.Option.newBuilder().setId(VENDOR_OPT)
                    .addName(MboParameters.Word.newBuilder().setName(VENDOR_OPT_VAL).build()).build())
                .build()
            )
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.BARCODE_ID)
                .setXslName(ParameterValueComposer.BARCODE)
                .setValueType(MboParameters.ValueType.STRING)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL)
                .build()
            )
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(ParameterUtil.FORMER_BAR_CODE_PARAM_ID)
                .setXslName(ParameterUtil.FORMER_BAR_CODE_PARAM_NAME)
                .setValueType(MboParameters.ValueType.STRING)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL)
                .build()
            )
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(NON_SKU_PARAM_ID)
                .setXslName(NON_SKU_PARAM_NAME)
                .setValueType(MboParameters.ValueType.STRING)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
                .build()
            )
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(SKU_PARAM_ID)
                .setXslName(SKU_PARAM_NAME)
                .setValueType(MboParameters.ValueType.STRING)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                .build()
            )
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(SKU_PARAM_DOUBLED_ID)
                .setXslName(SKU_PARAM_DOUBLED_NAME)
                .setValueType(MboParameters.ValueType.STRING)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
                .build()
            )
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.BARCODE_ID)
                .setXslName(ParameterValueComposer.BARCODE)
                .setValueType(MboParameters.ValueType.STRING)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL)
                .build()
            )
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(OLDEST_DATE_PARAM_ID)
                .setXslName(ParameterUtil.OLDEST_DATE_XSL_NAME)
                .setValueType(MboParameters.ValueType.STRING)
                .setMultivalue(true))
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(USE_NAME_AS_TITLE_PARAM_ID)
                .setXslName(ParameterUtil.USE_NAME_AS_TITLE_XLS_NAME)
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .addOption(MboParameters.Option.newBuilder().setId(USE_NAME_AS_TITLE_TRUE_OPT)
                    .addName(MboParameters.Word.newBuilder().setName("true").build()).build())
                .addOption(MboParameters.Option.newBuilder().setId(2)
                    .addName(MboParameters.Word.newBuilder().setName("false").build()).build())
                .build())
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(CONVERTED_PARAM_ID)
                .setXslName(ParameterUtil.CONVERTED_FROM_PSKU10_XLS_NAME)
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .addOption(MboParameters.Option.newBuilder().setId(CONVERTED_PARAM_TRUE_OPT)
                    .addName(MboParameters.Word.newBuilder().setName("true").build()).build())
                .addOption(MboParameters.Option.newBuilder().setId(2)
                    .addName(MboParameters.Word.newBuilder().setName("false").build()).build())
                .build())
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(MODEL_QUALITY_PARAM_ID)
                .setXslName(ParameterUtil.MODEL_QUALITY_XLS_NAME)
                .setValueType(MboParameters.ValueType.ENUM)
                .addOption(MboParameters.Option.newBuilder().setId(ParameterUtil.MODEL_QUALITY_PARTNER)
                    .addName(MboParameters.Word.newBuilder().setName("true").build()).build())
                .build())
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(PARAM_WITH_WRONG_OPTION)
                .setXslName("ParamWithWrongOption")
                .setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL)
                .setValueType(MboParameters.ValueType.ENUM)
                .addOption(MboParameters.Option.newBuilder().setId(ParameterUtil.MODEL_QUALITY_PARTNER)
                    .addName(MboParameters.Word.newBuilder().setName("true").build()).build())
                .build())
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(MULTIVALUE_PARAM_ID)
                .setXslName(MULTIVALUE_PARAM_NAME)
                .setValueType(MboParameters.ValueType.ENUM)
                .setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL)
                .setMultivalue(true)
                .addOption(MboParameters.Option.newBuilder().setId(MULTIVALUE_PARAM_OPT_1)
                    .addName(MboParameters.Word.newBuilder().setName("multi opt 1").build()).build())
                .addOption(MboParameters.Option.newBuilder().setId(MULTIVALUE_PARAM_OPT_2)
                    .addName(MboParameters.Word.newBuilder().setName("multi opt 2").build()).build())
                .build())
            .build());
    }

    public static DataCampUnitedOffer.UnitedOffer newUnitedOffer(int businessId, String offerId, long mappedPSku10Id) {
        return DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(DataCampOffer.Offer.newBuilder()
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                    .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                            .setMarketSkuId(mappedPSku10Id)
                            .build())
                        .build())
                    .build())
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(businessId)
                    .setOfferId(offerId)
                    .build())
                .build())
            .build();
    }

    public ModelStorage.OperationStatus newOperationStatus(ModelStorage.OperationStatusType statusType,
                                                           long modelId,
                                                           ModelStorage.ModelType currentType,
                                                           ModelStorage.ModelType sourceType) {
        return ModelStorage.OperationStatus.newBuilder()
            .setType(ModelStorage.OperationType.CREATE)
            .setStatus(statusType)
            .setModelId(modelId)
            .setModel(ModelStorage.Model.newBuilder()
                .setId(modelId)
                .setCurrentType(currentType.name())
                .setSourceType(sourceType.name())
                .build())
            .build();
    }
}
