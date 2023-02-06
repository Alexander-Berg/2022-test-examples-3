package ru.yandex.market.mbo.db.modelstorage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.ByteString;
import com.googlecode.protobuf.format.JsonFormat;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.stubbing.Answer;
import org.springframework.core.convert.converter.Converter;

import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.modelstorage.data.GroupOperationStatusException;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.health.ModelStorageHealthService;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.image.ModelImageSyncService;
import ru.yandex.market.mbo.db.modelstorage.image.ParallelImageProcessingService;
import ru.yandex.market.mbo.db.modelstorage.index.GenericField;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelMergeServiceStub;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStoreInterfaceStub;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.user.AutoUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author york
 * @since 04.10.2017
 */
public class ModelStorageProtoTest {
    private Long idParamSeq = 1L;
    private static final Long CATEGORY_ID = 111L;
    private static final int AUTO_USER_ID = -100;
    private static final int IMAGE_PARAM_MAX_ID = 15;
    private static final long PICTURE_SIZE = 300L;
    private static final long VENDOR_ID = 1000L;

    private ModelStorageProtoService protoService;
    private ModelImageUploadingServiceImpl modelImageUploadingService;
    private CategoryParametersServiceClient categoryService;
    private StatsModelStorageServiceStub modelStorageService;
    private GeneratedSkuService generatedSkuService;
    private AutoUser autoUser = new AutoUser(AUTO_USER_ID);
    private List<MboParameters.Parameter> parameters;
    private ModelStorageHealthService modelStorageHealthService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        generatedSkuService = mock(GeneratedSkuService.class);
        modelStorageHealthService = mock(ModelStorageHealthService.class);
        protoService = new ModelStorageProtoService();
        protoService.setAutoUser(autoUser);
        protoService.setGeneratedSkuService(generatedSkuService);
        parameters = generateParams();

        modelStorageService = new StatsModelStorageServiceStub() {
            @Override
            public <T> Set<T> getFieldValues(GenericField genericField,
                                             MboIndexesFilter filter,
                                             Converter<String, T> converter, ReadStats readStats8) {
                return Collections.emptySet();
            }
        };
        ModelMergeServiceStub modelMergeServiceBase = new ModelMergeServiceStub(
            new ModelStoreInterfaceStub(modelStorageService));

        modelStorageService.setModelMergeService(modelMergeServiceBase);

        protoService.setStorageService(modelStorageService);

        categoryService = CategoryParametersServiceClientStub.ofCategory(CATEGORY_ID, parameters);

        protoService.setModelImageService(new ModelImageServiceImpl(modelStorageService, categoryService));
        ModelImageSyncService imageSyncService = new ModelImageSyncService(categoryService);
        protoService.setImageSyncService(imageSyncService);
        protoService.setModelStorageHealthService(modelStorageHealthService);

        ParallelImageProcessingService parallelImageProcessingService = new ParallelImageProcessingService();
        parallelImageProcessingService.setThreadCount(1);
        parallelImageProcessingService.afterPropertiesSet();
        protoService.setParallelImageProcessingService(parallelImageProcessingService);

    }

    private List<MboParameters.Parameter> generateParams() {
        List<MboParameters.Parameter> result = new ArrayList<>();
        for (String xslName : createImageParams()) {
            ImageType type = ImageType.getImageType(xslName);
            result.add(createStringParam(xslName));
            result.add(createStringParam(type.getUrlParamName(xslName)));
            result.add(createStringParam(type.getRawUrlParamName(xslName)));
            result.add(createNumericParam(type.getWidthParamName(xslName)));
            result.add(createNumericParam(type.getHeightParamName(xslName)));
        }
        result.add(createStringParam(XslNames.NAME));
        result.add(createNumericParam(XslNames.VENDOR));
        return result;
    }

    private MboParameters.Parameter createNumericParam(String xslName) {
        return MboParameters.Parameter.newBuilder()
            .setId(idParamSeq++)
            .setXslName(xslName)
            .setValueType(MboParameters.ValueType.NUMERIC)
            .setPrecision(1)
            .setMultivalue(false)
            .build();
    }

    private MboParameters.Parameter createStringParam(String xslName) {
        return MboParameters.Parameter.newBuilder()
            .setId(idParamSeq++)
            .setXslName(xslName)
            .setValueType(MboParameters.ValueType.STRING)
            .setMultivalue(false)
            .build();
    }

    private List<String> createImageParams() {
        List<String> result = new ArrayList<>();
        result.add(XslNames.XL_PICTURE);
        for (int i = 2; i < IMAGE_PARAM_MAX_ID; i++) {
            result.add("XL-Picture_" + i);
        }
        return result;
    }

    /**
     * Creating and updating guru model from autogenerated model with pictures.
     * Generated picture changes should not apply on next sync as guru have picture already.
     */
    @Test
    public void testGuruCreateUpdate() {
        mockGeneratedSkuService();
        mockImageUploadService();

        CommonModel generatedModel = createDefaultGeneratedModel(true);
        CommonModel guruModel = syncGeneratedToGuru(generatedModel);

        //check url remains the same
        assertEquals("xlpic_url.jpeg", getValueSafe(guruModel, "XLPictureUrl"));

        Object xlPic = getValueSafe(guruModel, XslNames.XL_PICTURE);
        Assert.assertNotNull(xlPic);
        Assert.assertNotEquals(xlPic, "xlpic.jpeg"); //check picture uploaded

        CommonModel autoModel = getModel(generatedModel.getId());
        ModelStorage.Model.Builder model = ModelProtoConverter.convert(autoModel).toBuilder();

        model.clearParameterValues();
        model.addAllParameterValues(getMandatoryParams());

        model.addAllParameterValues(Arrays.asList(
            createStringPV(XslNames.XL_PICTURE, ModelStorage.ModificationSource.AUTO, "xlpic2.jpeg"),
            createStringPV("XLPictureUrl", ModelStorage.ModificationSource.AUTO, "xlpic2_url"),
            createNumericPV("XLPictureSizeX", ModelStorage.ModificationSource.AUTO, PICTURE_SIZE),
            createNumericPV("XLPictureSizeY", ModelStorage.ModificationSource.AUTO, PICTURE_SIZE)
        ));

        guruModel = updateGuruFromGenerated(ModelProtoConverter.convert(model.build()));

        //check old remains
        Object newXlPic = getValueSafe(guruModel, XslNames.XL_PICTURE);
        assertEquals(xlPic, newXlPic);
    }

    @Test
    public void testGuruPicturesUpdatedIfEmptyNoSku() {
        mockGeneratedSkuService();
        mockImageUploadService();

        CommonModel generatedModel = createDefaultGeneratedModel(false);
        CommonModel guruModel = syncGeneratedToGuru(generatedModel);

        //check guru have no picture
        Assert.assertNull(getValueSafe(guruModel, XslNames.XL_PICTURE));

        CommonModel autoModel = getModel(generatedModel.getId());
        ModelStorage.Model.Builder model = ModelProtoConverter.convert(autoModel).toBuilder();
        model.addAllParameterValues(getXlPictureParams());

        guruModel = updateGuruFromGenerated(ModelProtoConverter.convert(model.build()));

        assertThat(guruModel.getPictures()).containsExactlyInAnyOrder(createXlPicture());
    }

    @Test
    public void testGuruPicturesUpdatedIfEmptyHasSku() {
        mockGeneratedSkuService();
        mockImageUploadService();

        CommonModel generatedModel = createDefaultGeneratedModel(false);
        CommonModel guruModel = syncGeneratedToGuru(generatedModel);
        createSkuModel(guruModel.getId(), CommonModel.Source.SKU, false);

        CommonModel autoModel = getModel(generatedModel.getId());
        ModelStorage.Model.Builder model = ModelProtoConverter.convert(autoModel).toBuilder();
        model.addAllParameterValues(getXlPictureParams());

        guruModel = updateGuruFromGenerated(ModelProtoConverter.convert(model.build()));


        assertThat(guruModel.getPictures()).containsExactlyInAnyOrder(createXlPicture());
    }

    @Test
    public void testGuruPicturesNotUpdatedIfSkuHasPictures() {
        mockGeneratedSkuService();
        mockImageUploadService();

        CommonModel generatedModel = createDefaultGeneratedModel(false);
        CommonModel guruModel = syncGeneratedToGuru(generatedModel);
        createSkuModel(guruModel.getId(), CommonModel.Source.SKU, true);

        CommonModel autoModel = getModel(generatedModel.getId());
        ModelStorage.Model.Builder model = ModelProtoConverter.convert(autoModel).toBuilder();
        model.addAllParameterValues(getXlPictureParams());

        guruModel = updateGuruFromGenerated(ModelProtoConverter.convert(model.build()));

        assertEquals(0, guruModel.getPictures().size());
    }

    @Test
    public void testImageValidation() {
        ModelStorage.ValidateImagesRequest request = ModelStorage.ValidateImagesRequest.newBuilder()
            .addImageData(ModelStorage.ImageData.newBuilder()
                .setContentBytes(ByteString.copyFrom(new byte[1]))
                .setContentType("jpeg")
                .setId(0)
                .setUrl("http://test.jpeg")
                .build())
            .build();
        ModelStorage.ValidateImagesResponse response = protoService.validateImages(request);

        Assert.assertNotNull(response);
        assertEquals(1, response.getValidationStatusCount());

        ModelStorage.ValidateImageStatus validationStatus = response.getValidationStatus(0);
        assertEquals(0, validationStatus.getId());
        Assert.assertTrue(validationStatus.hasProcessedImage());

        ModelStorage.OperationStatus status = validationStatus.getStatus();
        Assert.assertNotNull(status);
        assertEquals(ModelStorage.OperationStatusType.OK, status.getStatus());
        assertEquals(0, status.getValidationErrorCount());
    }

    @Test
    public void testImageValidationFail() {
        ModelStorage.ValidateImagesRequest request = ModelStorage.ValidateImagesRequest.newBuilder()
            .addImageData(ModelStorage.ImageData.newBuilder()
                .setContentType("jpeg")
                .setId(0)
                .setUrl("http://test.jpeg")
                .build())
            .build();
        ModelStorage.ValidateImagesResponse response = protoService.validateImages(request);

        Assert.assertNotNull(response);
        assertEquals(1, response.getValidationStatusCount());

        ModelStorage.ValidateImageStatus validationStatus = response.getValidationStatus(0);
        assertEquals(0, validationStatus.getId());
        Assert.assertFalse(validationStatus.hasProcessedImage());

        ModelStorage.OperationStatus status = validationStatus.getStatus();
        Assert.assertNotNull(status);
        assertEquals(ModelStorage.OperationStatusType.VALIDATION_ERROR, status.getStatus());
        assertEquals(1, status.getValidationErrorCount());
        ModelStorage.ValidationError validationError = status.getValidationError(0);
        assertEquals(ModelStorage.ValidationErrorType.INVALID_IMAGE_SIZE, validationError.getType());
    }

    /**
     * Check that guru model is not created from autogenerated, if uploading images failed.
     */
    @Test
    public void testGuruNotCreated() {
        ModelStorage.Model.Builder model = createGeneratedModel();
        List<ModelStorage.ParameterValue> mandatoryParams = getMandatoryParams();
        model.addAllParameterValues(mandatoryParams);
        List<ModelStorage.ParameterValue> xlPictureParams = getXlPictureParams();
        model.addAllParameterValues(xlPictureParams);

        OperationStatus op = modelStorageService.saveModel(
            ModelProtoConverter.convert(model.build()), autoUser.getId())
            .getSingleModelStatus();
        Assert.assertTrue(op.isOk());

        // failed image uploading
        modelImageUploadingService = mock(ModelImageUploadingServiceImpl.class);
        when(modelImageUploadingService.reuploadPicture(any(ModelSaveContext.class),
            any(CommonModel.class), any(Picture.class), any(OperationStatus.class)))
            .then(invocation -> null);
        protoService.setModelImageUploadingService(modelImageUploadingService);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Unable to create guru models");
        ModelStorage.OperationResponse resp = protoService.createGuruModels(
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .addSourceModelId(op.getModelId())
                .setCategoryId(model.getCategoryId())
                .setUserId(autoUser.getId())
                .setSourceModelType(ModelStorage.ModelType.GENERATED)
                .setPlaceOfCreation(ModelCardApi.GuruModelsCreationPlace.MBO)
                .build());

        thrown = ExpectedException.none();

        // check guru model was not created
        Long guruId = resp.getStatuses(0).getRelatedModelId();
        CommonModel guruModel = getModel(guruId);
        Assert.assertNull(guruModel);
    }

    @Test
    public void testVendorDoNotReuploadPictures() {
        ModelStorage.Model.Builder model = createVendorModel();
        List<ModelStorage.ParameterValue> mandatoryParams = getMandatoryParams();
        model.addAllParameterValues(mandatoryParams);
        List<ModelStorage.ParameterValue> xlPictureParams = getXlPictureParams();
        model.addAllParameterValues(xlPictureParams);

        OperationStatus op = modelStorageService.saveModel(
            ModelProtoConverter.convert(model.build()), autoUser.getId())
            .getSingleModelStatus();
        Assert.assertTrue(op.isOk());

        // Failed image uploading to check that images are not going to be reuploaded for Vendor model.
        modelImageUploadingService = mock(ModelImageUploadingServiceImpl.class);
        when(modelImageUploadingService.reuploadPicture(any(ModelSaveContext.class),
            any(CommonModel.class), any(Picture.class), any(OperationStatus.class)))
            .then(invocation -> null);
        protoService.setModelImageUploadingService(modelImageUploadingService);

        when(generatedSkuService.createOrUpdateSku(any(CommonModel.class), any(CommonModel.class),
            any(ModelSaveContext.class), any(GeneratedSkuService.GeneratedSkuSyncContext.class)))
            .thenReturn(new GroupOperationStatus(
                new OperationStatus(OperationStatusType.OK, OperationType.CREATE, 1L))
            );

        ModelStorage.OperationResponse resp = protoService.createGuruModels(
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .addSourceModelId(op.getModelId())
                .setCategoryId(model.getCategoryId())
                .setUserId(autoUser.getId())
                .setSourceModelType(ModelStorage.ModelType.VENDOR)
                .build());

        assertEquals(JsonFormat.printToString(resp),
            ModelStorage.OperationStatusType.OK, resp.getStatuses(0).getStatus());

        // Check that guru model was created.
        Long guruId = resp.getStatuses(0).getRelatedModelId();
        CommonModel guruModel = getModel(guruId);
        Assert.assertNotNull(guruModel);
    }

    @Test
    public void testCreateSkuFromGSku() {
        mockGeneratedSkuService();
        mockImageUploadService();

        CommonModel generatedModel = createDefaultGeneratedModel(false);
        CommonModel guruModel = syncGeneratedToGuru(generatedModel);
        CommonModel gskuModel = createSkuModel(generatedModel.getId(), CommonModel.Source.GENERATED_SKU, false);

        ModelStorage.OperationResponse resp = protoService.createSkuFromGSku(
            ModelCardApi.CreateSkuFromGSkuRequest.newBuilder()
                .addGskuIds(gskuModel.getId())
                .setCategoryId(gskuModel.getCategoryId())
                .setUserId(autoUser.getId())
                .build());

        Assertions.assertThat(resp.getStatusesList()).hasSize(1);
        Assertions.assertThat(resp.getStatuses(0).getStatus()).isEqualTo(ModelStorage.OperationStatusType.OK);
        Assertions.assertThat(resp.getStatuses(0).getModelId()).isEqualTo(gskuModel.getId());
        Assertions.assertThat(resp.getStatuses(0).getModel().getId()).isNotEqualTo(gskuModel.getId());
    }

    @Test
    public void testCreateSkuFromGSkuWillFailIfSomeGskuNotFound() {
        mockGeneratedSkuService();
        mockImageUploadService();

        CommonModel generatedModel = createDefaultGeneratedModel(false);
        CommonModel guruModel = syncGeneratedToGuru(generatedModel);
        CommonModel gskuModel = createSkuModel(generatedModel.getId(), CommonModel.Source.GENERATED_SKU, false);

        ModelStorage.OperationResponse resp = protoService.createSkuFromGSku(
            ModelCardApi.CreateSkuFromGSkuRequest.newBuilder()
                .addGskuIds(gskuModel.getId()).addGskuIds(100) // <-- 100 not existing gsku
                .setCategoryId(gskuModel.getCategoryId())
                .setUserId(autoUser.getId())
                .build());

        Assertions.assertThat(resp.getStatusesList())
            .containsExactlyInAnyOrder(
                ModelStorage.OperationStatus.newBuilder()
                    .setModelId(gskuModel.getId()).setFailureModelId(gskuModel.getId())
                    .setType(ModelStorage.OperationType.CREATE)
                    .setStatus(ModelStorage.OperationStatusType.FAILED_MODEL_IN_GROUP)
                    .setStatusMessage("Failed to process model due to other errors in save")
                    .build(),
                ModelStorage.OperationStatus.newBuilder()
                    .setModelId(100).setFailureModelId(100)
                    .setType(ModelStorage.OperationType.CREATE)
                    .setStatus(ModelStorage.OperationStatusType.MODEL_NOT_FOUND)
                    .setStatusMessage("Failed to find gsku (100) in category " + gskuModel.getCategoryId())
                    .build()
            );
    }

    @Test(expected = RuntimeException.class)
    public void testCreateSkuFromGSkuWillFailIfNoGuruModel() {
        mockGeneratedSkuService();
        mockImageUploadService();

        CommonModel generatedModel = createDefaultGeneratedModel(false);
        CommonModel gskuModel = createSkuModel(generatedModel.getId(), CommonModel.Source.GENERATED_SKU, false);

        protoService.createSkuFromGSku(
            ModelCardApi.CreateSkuFromGSkuRequest.newBuilder()
                .addGskuIds(gskuModel.getId())
                .setCategoryId(gskuModel.getCategoryId())
                .setUserId(autoUser.getId())
                .build());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateSkuFromGSkuWillFailIfGskuNotRelatedToVendorModel() {
        mockGeneratedSkuService();
        mockImageUploadService();

        CommonModel gskuModel = createSkuModel(CommonModel.Source.GENERATED_SKU, false);

        protoService.createSkuFromGSku(
            ModelCardApi.CreateSkuFromGSkuRequest.newBuilder()
                .addGskuIds(gskuModel.getId())
                .setCategoryId(gskuModel.getCategoryId())
                .setUserId(autoUser.getId())
                .build());
    }

    @Test
    public void testCreateSkuFromGSkuWillFailIfGskuFromDifferentGeneratedModels() {
        mockGeneratedSkuService();
        mockImageUploadService();

        CommonModel generatedModel1 = createDefaultGeneratedModel(false);
        CommonModel generatedModel2 = createDefaultGeneratedModel(false);
        CommonModel gskuModel1 = createSkuModel(generatedModel1.getId(), CommonModel.Source.GENERATED_SKU, false);
        CommonModel gskuModel2 = createSkuModel(generatedModel2.getId(), CommonModel.Source.GENERATED_SKU, false);

        assertEquals(gskuModel1.getCategoryId(), gskuModel2.getCategoryId());

        ModelStorage.OperationResponse resp = protoService.createSkuFromGSku(
            ModelCardApi.CreateSkuFromGSkuRequest.newBuilder()
                .addGskuIds(gskuModel1.getId()).addGskuIds(gskuModel2.getId())
                .setCategoryId(gskuModel1.getCategoryId())
                .setUserId(autoUser.getId())
                .build());

        Assertions.assertThat(resp.getStatusesList())
            .containsExactlyInAnyOrder(
                ModelStorage.OperationStatus.newBuilder()
                    .setModelId(gskuModel1.getId()).setFailureModelId(gskuModel1.getId())
                    .setType(ModelStorage.OperationType.CREATE)
                    .setStatus(ModelStorage.OperationStatusType.NOT_SUPPORTED)
                    .setStatusMessage("Generated sku (" + gskuModel1.getId() + " " +
                        "is from different vendor model (" + generatedModel1.getId() + ")")
                    .build(),
                ModelStorage.OperationStatus.newBuilder()
                    .setModelId(gskuModel2.getId()).setFailureModelId(gskuModel2.getId())
                    .setType(ModelStorage.OperationType.CREATE)
                    .setStatus(ModelStorage.OperationStatusType.NOT_SUPPORTED)
                    .setStatusMessage("Generated sku (" + gskuModel2.getId() + " " +
                        "is from different vendor model (" + generatedModel2.getId() + ")")
                    .build()
            );
    }

    @NotNull
    private List<ModelStorage.ParameterValue> getXlPictureParams() {
        return Arrays.asList(
            createStringPV(XslNames.XL_PICTURE, ModelStorage.ModificationSource.AUTO, "xlpic.jpeg"),
            createStringPV("XLPictureUrl", ModelStorage.ModificationSource.AUTO, "xlpic_url.jpeg"),
            createNumericPV("XLPictureSizeX", ModelStorage.ModificationSource.AUTO, 100L),
            createNumericPV("XLPictureSizeY", ModelStorage.ModificationSource.AUTO, 100L)
        );
    }

    @NotNull
    private Picture createXlPicture() {
        Picture picture = new Picture();
        picture.setXslName(XslNames.XL_PICTURE);
        picture.setUrl("xlpic.jpeguploaded");
        picture.setUrlSource("xlpic_url.jpeg");
        picture.setWidth(100);
        picture.setHeight(100);
        picture.setModificationSource(ModificationSource.AUTO);
        picture.setPictureStatus(Picture.PictureStatus.APPROVED);
        return picture;
    }

    @NotNull
    private ModelStorage.Model.Builder createGeneratedModel() {
        return ModelStorageTestUtil.generateModel().toBuilder()
            .setCurrentType(CommonModel.Source.GENERATED.name())
            .setCategoryId(CATEGORY_ID)
            .clearParentId()
            .clearDeleted()
            .clearRelations()
            .clearPictures()
            .clearParameterValues();
    }

    @NotNull
    private ModelStorage.Model.Builder createVendorModel() {
        return ModelStorageTestUtil.generateModel().toBuilder()
            .setCurrentType(CommonModel.Source.VENDOR.name())
            .setCategoryId(CATEGORY_ID)
            .setSourceType(CommonModel.Source.GENERATED.name())
            .clearParentId()
            .clearDeleted()
            .clearRelations()
            .clearPictures()
            .clearParameterValues();
    }

    @NotNull
    private ModelStorage.Model.Builder createSKU(Long parentId, CommonModel.Source source) {
        return createSKU(source)
            .addRelations(ModelStorage.Relation.newBuilder()
                .setId(parentId)
                .setCategoryId(CATEGORY_ID)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                .build());
    }

    @NotNull
    private ModelStorage.Model.Builder createSKU(CommonModel.Source source) {
        return ModelStorageTestUtil.generateModel().toBuilder()
            .setCurrentType(source.name())
            .setCategoryId(CATEGORY_ID)
            .clearParentId()
            .clearDeleted()
            .clearRelations()
            .clearPictures()
            .clearParameterValues();
    }

    @NotNull
    private List<ModelStorage.ParameterValue> getMandatoryParams() {
        return Arrays.asList(
            createStringPV(XslNames.NAME, ModelStorage.ModificationSource.AUTO, "Auto1"),
            createNumericPV(XslNames.VENDOR, ModelStorage.ModificationSource.AUTO, VENDOR_ID)
        );
    }

    private void mockGeneratedSkuService() {
        when(generatedSkuService.createOrUpdateSku(any(CommonModel.class), any(CommonModel.class),
            any(ModelSaveContext.class), any(GeneratedSkuService.GeneratedSkuSyncContext.class)))
            .then(invocation -> {
                CommonModel generated = invocation.getArgument(0);
                return new GroupOperationStatus(
                    new OperationStatus(OperationStatusType.OK, OperationType.CREATE, generated.getId())
                );
            });
        try {
            when(generatedSkuService.createAndAddSkuToGuruModel(any(), any(), anyList(), any(), any()))
                .then((Answer<List<CommonModel>>) invocation -> {
                    CommonModel generated = invocation.getArgument(0);
                    CommonModel guru = invocation.getArgument(1);
                    List<CommonModel> gskus = invocation.getArgument(2);

                    return gskus.stream()
                        .map(gsku -> {
                            CommonModel sku = new CommonModel(gsku);
                            sku.setId(CommonModel.NO_ID);
                            sku.setCurrentType(CommonModel.Source.SKU);
                            sku.clearRelations();
                            sku.addRelation(new ModelRelation(guru.getId(), guru.getCategoryId(),
                                ModelRelation.RelationType.SKU_PARENT_MODEL));
                            sku.addRelation(new ModelRelation(gsku.getId(), gsku.getCategoryId(),
                                ModelRelation.RelationType.SYNC_SOURCE));
                            return sku;
                        })
                        .collect(Collectors.toList());
                });
        } catch (GroupOperationStatusException e) {
            throw new RuntimeException(e);
        }
    }

    private void mockImageUploadService() {
        modelImageUploadingService = new ModelImageUploadingServiceImpl(
            modelStorageService, categoryService
        );
        protoService.setModelImageUploadingService(modelImageUploadingService);
    }

    private CommonModel getModel(Long modelId) {
        Optional<CommonModel> model = modelStorageService.getModel(CATEGORY_ID, modelId);
        return model.orElse(null);
    }

    private Object getValueSafe(CommonModel model, String xslName) {
        return model.getEffectiveSingleParameterValue(xslName)
            .map(parameterValue -> {
                switch (parameterValue.getType()) {
                    case ENUM:
                    case NUMERIC_ENUM:
                        return parameterValue.getOptionId();
                    case STRING:
                        return WordUtil.getDefaultWord(parameterValue.getStringValue());
                    case BOOLEAN:
                        return parameterValue.getBooleanValue();
                    case NUMERIC:
                        return parameterValue.getNumericValue().longValue();
                    default:
                        throw new IllegalStateException();
                }
            })
            .orElse(null);
    }

    private ModelStorage.ParameterValue createStringPV(String xslName, ModelStorage.ModificationSource source,
                                                       String... values) {
        return ModelStorageTestUtil.stringParamValue(getParamId(xslName), xslName, values).toBuilder()
            .setValueSource(source)
            .build();
    }

    private ModelStorage.ParameterValue createNumericPV(String xslName, ModelStorage.ModificationSource source,
                                                        Long value) {
        return ModelStorage.ParameterValue.newBuilder()
            .setXslName(xslName)
            .setParamId(getParamId(xslName))
            .setTypeId(Parameter.Type.NUMERIC.ordinal())
            .setValueType(MboParameters.ValueType.NUMERIC)
            .setNumericValue(new BigDecimal(value).toPlainString())
            .setValueSource(source)
            .build();
    }

    private long getParamId(String xslName) {
        return parameters.stream()
            .filter(pv -> xslName.equals(pv.getXslName()))
            .findAny()
            .map(MboParameters.Parameter::getId)
            .orElseThrow(() -> new RuntimeException("not found parameter with xslName '" + xslName + "'"));
    }

    private CommonModel createDefaultGeneratedModel(boolean havePictures) {
        ModelStorage.Model.Builder model = createGeneratedModel();
        List<ModelStorage.ParameterValue> mandatoryParams = getMandatoryParams();
        model.addAllParameterValues(mandatoryParams);
        if (havePictures) {
            List<ModelStorage.ParameterValue> xlPictureParams = getXlPictureParams();
            model.addAllParameterValues(xlPictureParams);
        }

        OperationStatus op = modelStorageService.saveModel(
            ModelProtoConverter.convert(model.build()), autoUser.getId())
            .getSingleModelStatus();
        assert op.isOk();

        return op.getModel();
    }

    private CommonModel createSkuModel(long parentModelId, CommonModel.Source source, boolean havePictures) {
        ModelStorage.Model.Builder sku = createSKU(parentModelId, source);
        if (havePictures) {
            Picture pic = createXlPicture();
            pic.setXslName(null);
            sku.addPictures(ModelProtoConverter.convertPicture(pic));
        }

        OperationStatus op = modelStorageService.saveModel(
            ModelProtoConverter.convert(sku.build()), autoUser.getId())
            .getSingleModelStatus();
        assert op.isOk();

        CommonModel skuModel = op.getModel();
        CommonModel parentModel = modelStorageService.searchById(parentModelId);

        parentModel.getRelations().add(
            new ModelRelation(skuModel.getId(), skuModel.getCategoryId(), ModelRelation.RelationType.SKU_MODEL));

        op = modelStorageService.saveModel(parentModel, autoUser.getId())
            .getSingleModelStatus();
        assert op.isOk();

        return skuModel;
    }

    private CommonModel createSkuModel(CommonModel.Source source, boolean havePictures) {
        ModelStorage.Model.Builder sku = createSKU(source);
        if (havePictures) {
            Picture pic = createXlPicture();
            pic.setXslName(null);
            sku.addPictures(ModelProtoConverter.convertPicture(pic));
        }

        OperationStatus op = modelStorageService.saveModel(
            ModelProtoConverter.convert(sku.build()), autoUser.getId())
            .getSingleModelStatus();
        assert op.isOk();

        return op.getModel();
    }

    private CommonModel syncGeneratedToGuru(CommonModel generated) {
        ModelStorage.OperationResponse resp = protoService.createGuruModels(
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .addSourceModelId(generated.getId())
                .setCategoryId(generated.getCategoryId())
                .setUserId(autoUser.getId())
                .setSourceModelType(ModelStorage.ModelType.GENERATED)
                .build());

        assertEquals(JsonFormat.printToString(resp),
            ModelStorage.OperationStatusType.OK, resp.getStatuses(0).getStatus());

        Long guruId = resp.getStatuses(0).getRelatedModelId();
        return getModel(guruId);
    }

    private CommonModel updateGuruFromGenerated(CommonModel generated) {
        OperationStatus op = modelStorageService.saveModel(generated, autoUser.getId())
            .getSingleModelStatus();
        assert op.isOk();

        ModelStorage.OperationResponse resp = protoService.updateGuruModels(
            ModelCardApi.SyncGuruModelsRequest.newBuilder()
                .addSourceModelId(op.getModelId())
                .setCategoryId(generated.getCategoryId())
                .setUserId(autoUser.getId())
                .setSourceModelType(ModelStorage.ModelType.GENERATED)
                .build());

        assertEquals(JsonFormat.printToString(resp),
            resp.getStatuses(0).getStatus(), ModelStorage.OperationStatusType.OK);

        long guruId = generated.getRelations(ModelRelation.RelationType.SYNC_TARGET).stream()
            .map(ModelRelation::getId)
            .findFirst().orElseThrow(() -> new IllegalStateException("Failed to find guru model relation"));
        return getModel(guruId);
    }
}
