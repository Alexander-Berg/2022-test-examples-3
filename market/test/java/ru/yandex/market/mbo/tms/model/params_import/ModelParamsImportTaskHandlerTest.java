package ru.yandex.market.mbo.tms.model.params_import;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ParamsImportState;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ParamsImportStatus;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelParamsImport;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelParamsImportStatus;
import ru.yandex.market.mbo.core.model.params_import.ModelParamsImportQueueTask;
import ru.yandex.market.mbo.db.DummyTransactionTemplate;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.SizeMeasureService;
import ru.yandex.market.mbo.db.modelstorage.image.HttpImageDownloader;
import ru.yandex.market.mbo.db.modelstorage.image.ImageData;
import ru.yandex.market.mbo.db.modelstorage.image.ImageDownloader;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.repo.ModelParamsImportRepositoryStub;
import ru.yandex.market.mbo.db.repo.ModelParamsImportStatusRepository;
import ru.yandex.market.mbo.db.repo.ModelParamsImportStatusRepositoryStub;
import ru.yandex.market.mbo.gwt.models.gurulight.GLMeasure;
import ru.yandex.market.mbo.gwt.models.gurulight.SizeMeasureDto;
import ru.yandex.market.mbo.gwt.models.gurulight.SizeMeasureUnitOptionDto;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.ThinCategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Unit;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.utils.PictureUtils;
import ru.yandex.market.mbo.image.ImageUploadContext;
import ru.yandex.market.mbo.image.ModelImageService;
import ru.yandex.market.mbo.params.AmazonS3ParamImportServiceStub;
import ru.yandex.market.mbo.synchronizer.export.xls.XlsExportUtils;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.gwt.utils.XslNames.XL_PICTURE;

@SuppressWarnings("checkstyle:MagicNumber")
public class ModelParamsImportTaskHandlerTest {

    private static final Long CATEGORY_HID = 1L;
    private static final String REMOVE_FLAG = "#";
    private static final String PICTURE1_URL = "http://ya.ru/1.jpg";
    private static final String PICTURE2_URL = "http://ya.ru/2.jpg";
    private static final String PICTURE3_URL = "http://ya.ru/3.jpg";
    private static final String PICTURE4_URL = "http://ya.ru/22.jpg";
    private static final String PICTURE_FAIL_URL = "http://ya.ru/fail.jpg";
    private static final byte[] PICTURE1_BYTE = new byte[]{1};
    private static final byte[] PICTURE2_BYTE = new byte[]{2};
    private static final byte[] PICTURE3_BYTE = new byte[]{3};
    private static final byte[] PICTURE4_BYTE = new byte[]{4};
    private static final byte[] PICTURE_FAIL_BYTE = new byte[]{5};

    private static final String MEASURE_XSL_NAME = "measure";
    private static final String VALUE_SUFFIX = "_value_";
    private static final String UNIT_SUFFIX = "_unit_";
    private static final Map<Long, Pair<Long, Long>> MEASURES = ImmutableMap.<Long, Pair<Long, Long>>builder()
        .put(1L, Pair.of(11L, 12L))
        .put(2L, Pair.of(21L, 22L))
        .build();

    private static final Map<Long, List<Pair<Long, String>>> SCALES
        = ImmutableMap.<Long, List<Pair<Long, String>>>builder()
        .put(12L, ImmutableList.of(Pair.of(121L, "scale121"), Pair.of(122L, "scale122")))
        .put(22L, ImmutableList.of(Pair.of(221L, "scale221"), Pair.of(222L, "scale222")))
        .build();

    private static final Map<Long, List<Pair<Long, String>>> MEASURE_OPTIONS
        = ImmutableMap.<Long, List<Pair<Long, String>>>builder()
        .put(121L, ImmutableList.of(Pair.of(1211L, "size1211"), Pair.of(1212L, "size1212")))
        .put(122L, ImmutableList.of(Pair.of(1221L, "size1221"), Pair.of(1222L, "size1222")))
        .put(221L, ImmutableList.of(Pair.of(2211L, "size2211"), Pair.of(2212L, "size2212")))
        .put(222L, ImmutableList.of(Pair.of(2221L, "size2221"), Pair.of(2222L, "size2222")))
        .build();

    private ModelParamsImportRepositoryStub importRepositoryStub;
    private ModelStorageServiceStub modelStorageService;
    private ModelParamsImportTaskHandler modelParamsImportTaskHandler;
    private CommonModelBuilder<CommonModel> modelBuilder;
    private AmazonS3ParamImportServiceStub importHandler;
    private ModelParamsImportStatusRepository statusRepositoryStub;
    private List<SizeMeasureDto> measures;

    @Before
    public void setUp() throws IOException {
        importHandler = new AmazonS3ParamImportServiceStub();
        importRepositoryStub = new ModelParamsImportRepositoryStub();
        modelStorageService = new ModelStorageServiceStub();
        statusRepositoryStub = new ModelParamsImportStatusRepositoryStub();

        ParametersBuilder<CommonModelBuilder<CommonModel>> parametersBuilder =
            ParametersBuilder.defaultBuilder();

        List<CategoryParam> parameters = parametersBuilder.getParameters();
        CategoryEntities entities = new CategoryEntities(CATEGORY_HID, Collections.emptyList());
        entities.addAllParameters(parameters);
        fillParameterForImages(entities);
        measures = prepareMeasures(entities);
        SizeMeasureService sizeMeasureService = prepareSizeMeasureServiceMock(measures);

        ParameterLoaderServiceStub parameterLoaderService = new ParameterLoaderServiceStub();
        parameterLoaderService.addCategoryEntities(entities);

        modelBuilder = parametersBuilder.endParameters();

        ImageDownloader httpImageDownloader = getMockImageDownloader();
        ModelImageService modelImageService = getMockModelImageService();

        ImageUploaderContext imageUploaderContext = new ImageUploaderContext(
            modelImageService,
            httpImageDownloader
        );

        modelParamsImportTaskHandler = new ModelParamsImportTaskHandler(
            importRepositoryStub,
            statusRepositoryStub,
            modelStorageService,
            parameterLoaderService,
            new DummyTransactionTemplate(),
            imageUploaderContext,
            importHandler,
            sizeMeasureService
        );
    }

    private String generateMeasureParamXslName(SizeMeasureDto measure) {
        return measure.getBaseParamXslName() + VALUE_SUFFIX + measure.getMeasure().getValueParamId();
    }

    private List<SizeMeasureDto> prepareMeasures(CategoryEntities entities) {
        List<SizeMeasureDto> measureList = MEASURES.entrySet().stream()
            .map(e -> {
                GLMeasure measure = new GLMeasure();
                measure.setId(e.getKey());
                measure.setValueParamId(e.getValue().getLeft());
                measure.setUnitParamId(e.getValue().getRight());
                return measure;
            })
            .map(m -> new SizeMeasureDto(
                m,
                new Unit("unit", "unit", BigDecimal.ONE, 0L, 0L),
                MEASURE_XSL_NAME,
                MEASURE_XSL_NAME
            ))
            .collect(Collectors.toList());

        measureList.forEach(m -> {
            Parameter param = new Parameter();
            param.setId(m.getMeasure().getValueParamId());
            param.setXslName(generateMeasureParamXslName(m));
            param.setType(Param.Type.ENUM);
            entities.addParameter(param);
            param = new Parameter();
            param.setId(m.getMeasure().getUnitParamId());
            param.setXslName(m.getBaseParamXslName() + UNIT_SUFFIX + m.getMeasure().getUnitParamId());
            param.setType(Param.Type.ENUM);
            entities.addParameter(param);
        });

        return measureList;
    }

    private SizeMeasureService prepareSizeMeasureServiceMock(List<SizeMeasureDto> measureList) {
        Map<Long, List<SizeMeasureUnitOptionDto>> scales = SCALES.entrySet().stream()
            .flatMap(e ->
                e.getValue().stream()
                    .map(v -> Pair.of(
                        e.getKey(),
                        new SizeMeasureUnitOptionDto(new OptionImpl(v.getLeft(), v.getRight()), 0L))
                    )
            )
            .collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));

        Map<Long, List<OptionImpl>> options = MEASURE_OPTIONS.entrySet().stream()
            .flatMap(e ->
                e.getValue().stream()
                    .map(v -> Pair.of(e.getKey(), new OptionImpl(v.getLeft(), v.getRight())))
            )
            .collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));

        SizeMeasureService res = mock(SizeMeasureService.class);
        when(res.listSizeMeasures(anyLong())).thenReturn(measureList);

        doAnswer(inv -> {
            GLMeasure m = inv.getArgument(0);
            return scales.get(m.getUnitParamId());
        }).when(res).listSizeMeasureScalesWithOverrides(any(GLMeasure.class), anyLong());
        doAnswer(inv -> {
            Collection<Long> scaleIds = inv.getArgument(1);
            return scaleIds.stream()
                .collect(Collectors.toMap(
                    Function.identity(),
                    options::get
                ));
        }).when(res).listSizeMeasureScalesValuesWithoutRules(anyLong(), any());
        return res;
    }

    private void fillParameterForImages(CategoryEntities entities) {
        Parameter param = new Parameter();
        param.setXslName(XL_PICTURE);
        entities.addParameter(param);
        param = new Parameter();
        param.setXslName(XL_PICTURE + "_" + 2);
        entities.addParameter(param);
        param = new Parameter();
        param.setXslName(XL_PICTURE + "_" + 3);
        entities.addParameter(param);
    }

    @NotNull
    private ModelImageService getMockModelImageService() {
        ModelImageService modelImageService = mock(ModelImageService.class);
        when(modelImageService.uploadPicture(any(), eq(PICTURE1_BYTE), any(), any()))
            .then(x -> {
                ImageUploadContext context = x.getArgument(0);
                Picture pic = new Picture();
                pic.setUrlSource(context.getSourceUrl());
                return pic;
            });
        when(modelImageService.uploadPicture(any(), eq(PICTURE2_BYTE), any(), any()))
            .then(x -> {
                ImageUploadContext context = x.getArgument(0);
                Picture pic = new Picture();
                pic.setUrlSource(context.getSourceUrl());
                return pic;
            });
        when(modelImageService.uploadPicture(any(), eq(PICTURE3_BYTE), any(), any()))
            .then(x -> {
                ImageUploadContext context = x.getArgument(0);
                Picture pic = new Picture();
                pic.setUrlSource(context.getSourceUrl());
                return pic;
            });
        when(modelImageService.uploadPicture(any(), eq(PICTURE4_BYTE), any(), any()))
            .then(x -> {
                ImageUploadContext context = x.getArgument(0);
                Picture pic = new Picture();
                pic.setUrlSource(context.getSourceUrl());
                return pic;
            });
        when(modelImageService.uploadPicture(any(), eq(PICTURE_FAIL_BYTE), any(), any()))
            .then(x -> {
                List<ModelValidationError> errors = x.getArgument(3);
                errors.add(new ModelValidationError(1L, ModelValidationError.ErrorType.INVALID_IMAGE));
                return new Picture();
            });
        return modelImageService;
    }

    @NotNull
    private ImageDownloader getMockImageDownloader() throws IOException {
        ImageDownloader httpImageDownloader = mock(HttpImageDownloader.class);
        when(httpImageDownloader.downloadImage(PICTURE1_URL))
            .thenReturn(new ImageData(PICTURE1_BYTE, "jpg"));
        when(httpImageDownloader.downloadImage(PICTURE2_URL))
            .thenReturn(new ImageData(PICTURE2_BYTE, "jpg"));
        when(httpImageDownloader.downloadImage(PICTURE3_URL))
            .thenReturn(new ImageData(PICTURE3_BYTE, "jpg"));
        when(httpImageDownloader.downloadImage(PICTURE4_URL))
            .thenReturn(new ImageData(PICTURE4_BYTE, "jpg"));
        when(httpImageDownloader.downloadImage(PICTURE_FAIL_URL))
            .thenReturn(new ImageData(PICTURE_FAIL_BYTE, "jpg"));
        return httpImageDownloader;
    }


    @Test
    public void testSuccessfulValidation() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createDefaultWorkbook();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, false), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        CommonModel model2 = modelStorageService.getModel(CATEGORY_HID, 2L).get();

        MboAssertions.assertThat(model1, ParametersBuilder.STRING_PARAM_ID).values("test", "test1");
        MboAssertions.assertThat(model1, ParametersBuilder.NUMERIC_PARAM_ID).values(1.0);
        MboAssertions.assertThat(model1, ParametersBuilder.ENUM_PARAM_ID).values(3L);
        MboAssertions.assertThat(model1, ParametersBuilder.BOOL_PARAM_ID).values(false);
        MboAssertions.assertThat(model1, ParametersBuilder.NUMERIC_ENUM_PARAM_ID).values(8L, 9L);

        MboAssertions.assertThat(model2, ParametersBuilder.STRING_PARAM_ID).notExists();
        MboAssertions.assertThat(model2, ParametersBuilder.NUMERIC_PARAM_ID).notExists();
        MboAssertions.assertThat(model2, ParametersBuilder.ENUM_PARAM_ID).notExists();
        MboAssertions.assertThat(model2, ParametersBuilder.BOOL_PARAM_ID).notExists();
        MboAssertions.assertThat(model2, ParametersBuilder.NUMERIC_ENUM_PARAM_ID).notExists();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.VERIFIED);
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Валидация завершена");
    }


    @Test
    public void testSuccessfulImportPicture() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createPictureWorkbook();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();

        MboAssertions.assertThat(model1, ParametersBuilder.STRING_PARAM_ID).values("qwerty");
        List<Picture> pics = model1.getPictures();
        assertThat(pics.size() == 2).isTrue();
        assertThat(pics.get(0).getXslName().equals(PictureUtils.getXLPictureNameByIndex(0))).isTrue();
        assertThat(pics.get(1).getXslName().equals(PictureUtils.getXLPictureNameByIndex(1))).isTrue();
        assertThat(pics.get(0).getUrlSource().equals(PICTURE3_URL)).isTrue();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);

    }


    @Test
    public void testSuccessfulAddFirstPicture() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        //PICTURE3_URL(1), PICTURE2_URL (1)
        Workbook workbook = createPictureWorkbookAddFirstPicture();


        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();

        MboAssertions.assertThat(model1, ParametersBuilder.STRING_PARAM_ID).values("qwerty");
        List<Picture> pics = model1.getPictures();
        assertThat(pics.size() == 1).isTrue();
        assertThat(pics.get(0).getXslName().equals(PictureUtils.getXLPictureNameByIndex(0))).isTrue();
        assertThat(pics.get(0).getUrlSource().equals(PICTURE3_URL)).isTrue();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);

    }

    @Test
    public void testImportSetPicture() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        // result = PICTURE1_URL
        Workbook workbook = createPictureWorkbookSet();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        List<Picture> pics = model1.getPictures();
        int count = 1;
        assertThat(pics.size() == count).isTrue();
        assertThat(pics.get(0).getUrlSource().equals(PICTURE1_URL)).isTrue();

        for (int i = 0; i < count; i++) {
            assertThat(pics.get(i).getXslName().equals(PictureUtils.getXLPictureNameByIndex(i))).isTrue();
        }

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testImportAddAddAddSetPicture() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);
        // result = PICTURE3_URL, PICTURE2_URL, PICTURE1_URL

        Workbook workbook = createPictureWorkbookAddAddAddSet();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        List<Picture> pics = model1.getPictures();
        int count = 2;
        assertThat(pics.size() == count).isTrue();
        assertThat(pics.get(0).getUrlSource().equals(PICTURE3_URL)).isTrue();
        assertThat(pics.get(1).getUrlSource().equals(PICTURE1_URL)).isTrue();

        for (int i = 0; i < count; i++) {
            assertThat(pics.get(i).getXslName().equals(PictureUtils.getXLPictureNameByIndex(i))).isTrue();
        }

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testImportookAddAddDeletePublishedPicture() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createPictureWorkbookAddAddDelete();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        List<Picture> pics = model1.getPictures();
        assertThat(pics.size() == 0).isTrue(); // картинки не удалены, а просто не добавлены
        CommonModel model2 = modelStorageService.getModel(CATEGORY_HID, 2L).get();
        List<Picture> pics2 = model2.getPictures();
        assertThat(pics2.size() == 1).isTrue();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testImportookAddAddDeleteCachePicture() throws Exception {
        List<CommonModel> models = createModels(false);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createPictureWorkbookAddAddDelete();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        List<Picture> pics = model1.getPictures();
        assertThat(pics.size() == 0).isTrue();
        CommonModel model2 = modelStorageService.getModel(CATEGORY_HID, 2L).get();
        List<Picture> pics2 = model2.getPictures();
        assertThat(pics2.size() == 1).isTrue();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testImportAddAddCachePicture() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createPictureWorkbookAddAdd();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        List<Picture> pics = model1.getPictures();
        int count = 1;
        assertThat(pics.size() == count).isTrue();
        assertThat(pics.get(0).getUrlSource().equals(PICTURE1_URL)).isTrue();

        for (int i = 0; i < count; i++) {
            assertThat(pics.get(i).getXslName().equals(PictureUtils.getXLPictureNameByIndex(i))).isTrue();
        }

        CommonModel model2 = modelStorageService.getModel(CATEGORY_HID, 2L).get();
        List<Picture> pics2 = model2.getPictures();
        assertThat(pics2.size() == count).isTrue();
        assertThat(pics2.get(0).getUrlSource().equals(PICTURE1_URL)).isTrue();
        assertThat(pics.get(0) != pics2.get(0)).isTrue();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testImportAddAddLAddSetPicture() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);
        // result = PICTURE1_URL, PICTURE2_URL, PICTURE3_URL

        Workbook workbook = createPictureWorkbookAddAddLAddSet();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        List<Picture> pics = model1.getPictures();
        int count = 2;
        assertThat(pics.size() == count).isTrue();
        assertThat(pics.get(0).getUrlSource().equals(PICTURE1_URL)).isTrue();
        assertThat(pics.get(1).getUrlSource().equals(PICTURE3_URL)).isTrue();

        for (int i = 0; i < count; i++) {
            assertThat(pics.get(i).getXslName().equals(PictureUtils.getXLPictureNameByIndex(i))).isTrue();
        }

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }


    @Test
    public void testImportAddLastMaxPicture() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);
        // result = PICTURE1_URL, PICTURE2_URL, PICTURE3_URL

        Workbook workbook = createPictureWorkbookAddLastMax();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        List<Picture> pics = model1.getPictures();
        int count = 0;
        assertThat(pics.size() == count).isTrue();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testImportAddMaxPicture() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);
        // result = PICTURE1_URL, PICTURE2_URL, PICTURE3_URL

        Workbook workbook = createPictureWorkbookAddMax();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        List<Picture> pics = model1.getPictures();
        int count = 0;
        assertThat(pics.size() == count).isTrue();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }


    @Test
    public void testDoubleImportPicture() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createPictureWorkbookDoubleAdd();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        List<Picture> pics = model1.getPictures();
        assertThat(pics.size() == 0).isTrue();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testBrokenPictureImport() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createPictureWorkbookBrokenPictureAdd();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        List<Picture> pics = model1.getPictures();
        assertThat(pics.size() == 0).isTrue();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testCleanEmptyPictureImport() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createPictureWorkbookCleanEmpty();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        List<Picture> pics = model1.getPictures();
        assertThat(pics.size() == 0).isTrue();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testSuccessfulImportWithRemovePicture() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createPictureWorkbook2();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model11 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        List<Picture> pics11 = model11.getPictures();
        assertThat(pics11.size() == 2).isTrue();
        // "ADD_FIRST_PICTURE", "ADD_FIRST_PICTURE", "ADD_LAST_PICTURE" "SET_FIRST_PICTURE"),
        // PICTURE1_URL, PICTURE2_URL, PICTURE3_URL, PICTURE3_URL
        assertThat(pics11.get(0).getXslName().equals(PictureUtils.getXLPictureNameByIndex(0))).isTrue();
        assertThat(pics11.get(1).getXslName().equals(PictureUtils.getXLPictureNameByIndex(1))).isTrue();
        assertThat(pics11.get(0).getUrlSource().equals(PICTURE3_URL)).isTrue();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testSuccessfulImport() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createDefaultWorkbook();

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        CommonModel model2 = modelStorageService.getModel(CATEGORY_HID, 2L).get();

        MboAssertions.assertThat(model1, ParametersBuilder.STRING_PARAM_ID).values("qwerty");
        MboAssertions.assertThat(model1, ParametersBuilder.NUMERIC_PARAM_ID).values(1.0);
        MboAssertions.assertThat(model1, ParametersBuilder.ENUM_PARAM_ID).values(3L, 4L);
        MboAssertions.assertThat(model1, ParametersBuilder.BOOL_PARAM_ID).values(false);
        MboAssertions.assertThat(model1, ParametersBuilder.NUMERIC_ENUM_PARAM_ID).values(8L, 9L);

        MboAssertions.assertThat(model2, ParametersBuilder.STRING_PARAM_ID).values("qwerty1,;[]\\/|");
        MboAssertions.assertThat(model2, ParametersBuilder.NUMERIC_PARAM_ID).values(20);
        MboAssertions.assertThat(model2, ParametersBuilder.ENUM_PARAM_ID).values(4L, 5L);
        MboAssertions.assertThat(model2, ParametersBuilder.BOOL_PARAM_ID).values(true);
        MboAssertions.assertThat(model2, ParametersBuilder.NUMERIC_ENUM_PARAM_ID).values(9L, 10L);

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testSuccessfulRemove() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createWorkbook(
            ImmutableList.of("name"),
            ImmutableList.of("SET"),
            ImmutableList.of(1L),
            REMOVE_FLAG
        );

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();

        MboAssertions.assertThat(model1, ParametersBuilder.STRING_PARAM_ID).notExists();
        MboAssertions.assertThat(model1, ParametersBuilder.NUMERIC_PARAM_ID).values(1.0);
        MboAssertions.assertThat(model1, ParametersBuilder.ENUM_PARAM_ID).values(3L);
        MboAssertions.assertThat(model1, ParametersBuilder.BOOL_PARAM_ID).values(false);
        MboAssertions.assertThat(model1, ParametersBuilder.NUMERIC_ENUM_PARAM_ID).values(8L, 9L);

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testFailureRemove() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createWorkbook(
            ImmutableList.of("name"),
            ImmutableList.of("APPEND"),
            ImmutableList.of(1L),
            REMOVE_FLAG
        );

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();

        MboAssertions.assertThat(model1, ParametersBuilder.STRING_PARAM_ID).values("test", "test1");
        MboAssertions.assertThat(model1, ParametersBuilder.NUMERIC_PARAM_ID).values(1.0);
        MboAssertions.assertThat(model1, ParametersBuilder.ENUM_PARAM_ID).values(3L);
        MboAssertions.assertThat(model1, ParametersBuilder.BOOL_PARAM_ID).values(false);
        MboAssertions.assertThat(model1, ParametersBuilder.NUMERIC_ENUM_PARAM_ID).values(8L, 9L);

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORT_FAILED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo(
            "Ошибка при обработке импорта: Некорректное действие в 3 строке - 'APPEND'. " +
                "Флаг удалениия '#' может использоваться только с операцией 'SET' или 'CLEAN_PICTURES'.");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testFailureInvalidTransformationName() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createWorkbook(
            ImmutableList.of("name"),
            ImmutableList.of("Invalid"),
            ImmutableList.of(1L),
            "qwerty"
        );

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        CommonModel model2 = modelStorageService.getModel(CATEGORY_HID, 2L).get();

        MboAssertions.assertThat(model1, ParametersBuilder.STRING_PARAM_ID).values("test", "test1");
        MboAssertions.assertThat(model1, ParametersBuilder.NUMERIC_PARAM_ID).values(1.0);
        MboAssertions.assertThat(model1, ParametersBuilder.ENUM_PARAM_ID).values(3L);
        MboAssertions.assertThat(model1, ParametersBuilder.BOOL_PARAM_ID).values(false);
        MboAssertions.assertThat(model1, ParametersBuilder.NUMERIC_ENUM_PARAM_ID).values(8L, 9L);

        MboAssertions.assertThat(model2, ParametersBuilder.STRING_PARAM_ID).notExists();
        MboAssertions.assertThat(model2, ParametersBuilder.NUMERIC_PARAM_ID).notExists();
        MboAssertions.assertThat(model2, ParametersBuilder.ENUM_PARAM_ID).notExists();
        MboAssertions.assertThat(model2, ParametersBuilder.BOOL_PARAM_ID).notExists();
        MboAssertions.assertThat(model2, ParametersBuilder.NUMERIC_ENUM_PARAM_ID).notExists();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORT_FAILED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        String possibleActions = Stream.of(ModelParamTransformationType.values())
            .map(ModelParamTransformationType::name)
            .collect(Collectors.joining("/"));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo(
            "Ошибка при обработке импорта: Некорректное действие в 2 колонке - " +
                "'Invalid'. " +
                "Возможные значения: " +
                possibleActions);
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testSuccessfulImportWithSize() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createSizeMeasureWorkbook(measures,
            ImmutableList.of(1L, 2L),
            "scale121;size1212", "scale222;size2221",
            "scale122;size1221", "scale221;size2212"
        );

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();
        MboAssertions.assertThat(model1, measures.get(0).getMeasure().getValueParamId()).values(1212L);
        MboAssertions.assertThat(model1, measures.get(1).getMeasure().getValueParamId()).values(2221L);

        CommonModel model2 = modelStorageService.getModel(CATEGORY_HID, 2L).get();
        MboAssertions.assertThat(model2, measures.get(0).getMeasure().getValueParamId()).values(1221L);
        MboAssertions.assertThat(model2, measures.get(1).getMeasure().getValueParamId()).values(2212L);

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);
    }

    @Test
    public void testSkipWhenWrongScale() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createSizeMeasureWorkbook(measures,
            Collections.singletonList(1L),
            "wrong_scale;size1212", "scale222;size2221"
        );

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();

        MboAssertions.assertThat(model1, measures.get(0).getMeasure().getValueParamId()).notExists();
        MboAssertions.assertThat(model1, measures.get(1).getMeasure().getValueParamId()).notExists();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);

        List<ModelParamsImportStatus> statuses = statusRepositoryStub.find(
            ModelParamsImportStatusRepository.Filter.all(paramsImport.getId(), false)
                .withRowIndexes(Collections.singleton(2))
        );

        assertThat(statuses).hasSize(1);
        assertThat(statuses.get(0).getStatus()).isEqualTo(ParamsImportStatus.NO_OP);
        assertThat(statuses.get(0).getStatusMsg()).contains(
            "measure_value_11: Scale 'wrong_scale' not found for parameter 11");
    }

    @Test
    public void testFailureWhenWrongSize() throws Exception {
        List<CommonModel> models = createModels(true);
        modelStorageService.initializeWithModels(models);

        Workbook workbook = createSizeMeasureWorkbook(measures,
            Collections.singletonList(1L),
            "scale121;wrong_size", "scale222;size2221"
        );

        ModelParamsImport paramsImport = createAndSaveImport(workbook);

        modelParamsImportTaskHandler.handle(new ModelParamsImportQueueTask(paramsImport.getId(), 100L, true), null);

        CommonModel model1 = modelStorageService.getModel(CATEGORY_HID, 1L).get();

        MboAssertions.assertThat(model1, measures.get(0).getMeasure().getValueParamId()).notExists();
        MboAssertions.assertThat(model1, measures.get(1).getMeasure().getValueParamId()).notExists();

        paramsImport = importRepositoryStub.getById(paramsImport.getId());
        assertThat(paramsImport.getState()).isEqualTo(ParamsImportState.IMPORTED);
        assertThat(getUploadFileData(paramsImport.getId())).isEqualTo(getWbData(workbook));
        assertThat(paramsImport.getLastOperationResult()).isEqualTo("Заливка завершена");
        assertThat(paramsImport.getUploadedUid()).isEqualTo(100L);

        List<ModelParamsImportStatus> statuses = statusRepositoryStub.find(
            ModelParamsImportStatusRepository.Filter.all(paramsImport.getId(), false)
                .withRowIndexes(Collections.singleton(2))
        );

        assertThat(statuses).hasSize(1);
        assertThat(statuses.get(0).getStatus()).isEqualTo(ParamsImportStatus.NO_OP);
        assertThat(statuses.get(0).getStatusMsg()).contains(
            "measure_value_11: Missing size value 'scale121;wrong_size' in parameter 11");
    }

    private ModelParamsImport createAndSaveImport(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbook.write(bos);
            ModelParamsImport modelParamsImport = new ModelParamsImport()
                .setState(ParamsImportState.NEW)
                .setCreatedUid(50L);
            ModelParamsImport paramsImport = importRepositoryStub.save(modelParamsImport);
            importHandler.uploadInputFile(paramsImport.getId(), bos.toByteArray(), "test.xls");
            return paramsImport;
        }
    }

    private byte[] getWbData(Workbook wb) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private Workbook createWorkbook(List<String> xslNames,
                                    List<String> actions,
                                    List<Long> modelIds,
                                    String... values) {
        List<List<String>> allModelValues =
            Lists.partition(Stream.of(values).collect(Collectors.toList()), xslNames.size());
        assertThat(xslNames.size()).isEqualTo(actions.size());
        assertThat(allModelValues.size()).isEqualTo(modelIds.size());
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test");
        workbook.setActiveSheet(workbook.getSheetIndex(sheet));

        Row xslNamesRow = sheet.createRow(0);
        Row actionsRow = sheet.createRow(1);
        CellStyle style = workbook.createCellStyle();
        for (int i = 0; i < xslNames.size(); i++) {
            String xslName = xslNames.get(i);
            String action = actions.get(i);
            XlsExportUtils.createTextCell(xslNamesRow, i + 1, xslName, style);
            XlsExportUtils.createTextCell(actionsRow, i + 1, action, style);
        }
        for (int i = 0; i < modelIds.size(); i++) {
            Long modelId = modelIds.get(i);
            Row modelRow = sheet.createRow(i + 2);
            List<String> modelValues = allModelValues.get(i);
            XlsExportUtils.createTextCell(modelRow, 0, modelId.toString(), style);
            for (int j = 0; j < xslNames.size(); j++) {
                XlsExportUtils.createTextCell(modelRow, j + 1, modelValues.get(j), style);
            }
        }
        return workbook;
    }


    private Workbook createPictureWorkbookAddFirstPicture() {
        return createWorkbook(
            ImmutableList.of("name", "numeric", "enum", "IsSku", "numericEnum", "", "", "-"),
            ImmutableList.of("SET", "SET_IF_EMPTY", "APPEND", "SET", "SET_IF_EMPTY",
                "ADD_LAST_PICTURE", "ADD_LAST_PICTURE", "SET_FIRST_PICTURE"),
            ImmutableList.of(1L),
            "qwerty", "10", "enum3||enum4", "false", "200",
            PICTURE1_URL, PICTURE3_URL, PICTURE3_URL
        );
    }

    private Workbook createPictureWorkbook() {
        return createWorkbook(
            ImmutableList.of("name", "numeric", "enum", "IsSku", "numericEnum", "", "", "", "-"),
            ImmutableList.of("SET", "SET_IF_EMPTY", "APPEND", "SET", "SET_IF_EMPTY",
                "ADD_FIRST_PICTURE", "ADD_FIRST_PICTURE", "ADD_LAST_PICTURE", "SET_FIRST_PICTURE"),
            ImmutableList.of(1L),
            "qwerty", "10", "enum3||enum4", "false", "200",
            PICTURE1_URL, PICTURE2_URL, PICTURE3_URL, PICTURE3_URL
        );
    }

    private Workbook createPictureWorkbook2() {
        return createWorkbook(
            ImmutableList.of("name", "numeric", "enum", "IsSku", "numericEnum", "", "", "", ""),
            ImmutableList.of("SET", "SET_IF_EMPTY", "APPEND", "SET", "SET_IF_EMPTY",
                "ADD_FIRST_PICTURE", "ADD_FIRST_PICTURE", "ADD_LAST_PICTURE",
                "SET_FIRST_PICTURE"),
            ImmutableList.of(1L),
            "qwerty", "10", "enum3||enum4", "false", "200",
            PICTURE1_URL, PICTURE2_URL, PICTURE3_URL, PICTURE3_URL
        );
    }

    // result = PICTURE3_URL, PICTURE2_URL, PICTURE1_URL
    private Workbook createPictureWorkbookAddAddAddSet() {
        return createWorkbook(
            ImmutableList.of("name", "numeric", "enum", "IsSku", "numericEnum", "", "", "", ""),
            ImmutableList.of("SET", "SET_IF_EMPTY", "APPEND", "SET", "SET_IF_EMPTY",
                "ADD_FIRST_PICTURE", "ADD_FIRST_PICTURE", "ADD_FIRST_PICTURE", "SET_FIRST_PICTURE"),
            ImmutableList.of(1L),
            "qwerty", "10", "enum3||enum4", "false", "200",
            PICTURE1_URL, PICTURE2_URL, PICTURE3_URL, PICTURE3_URL
        );
    }

    // result = PICTURE1_URL, PICTURE1_URL
    private Workbook createPictureWorkbookAddAdd() {

        return createWorkbook(
            ImmutableList.of("name", "numeric", "enum", "IsSku", "numericEnum", ""),
            ImmutableList.of("SET", "SET_IF_EMPTY", "APPEND", "SET", "SET_IF_EMPTY",
                "ADD_FIRST_PICTURE"),
            ImmutableList.of(1L, 2L),
            "qwerty", "10", "enum3||enum4", "false", "200",
            PICTURE1_URL,
            "qwerty1,;[]\\/|", "20", "enum4||enum5", "true", "200||300",
            PICTURE1_URL
        );
    }

    // result = PICTURE1_URL, PICTURE1_URL
    private Workbook createPictureWorkbookAddAddDelete() {

        return createWorkbook(
            ImmutableList.of("name", "numeric", "enum", "IsSku", "numericEnum", "", ""),
            ImmutableList.of("SET", "SET_IF_EMPTY", "APPEND", "SET", "SET_IF_EMPTY",
                "ADD_FIRST_PICTURE",
                "CLEAN_PICTURES"),
            ImmutableList.of(1L, 2L),
            "qwerty", "10", "enum3||enum4", "false", "200",
            PICTURE1_URL, "#",
            "qwerty1,;[]\\/|", "20", "enum4||enum5", "true", "200||300",
            PICTURE1_URL, ""
        );
    }

    // result = PICTURE1_URL , PICTURE2_URL, PICTURE3_URL
    private Workbook createPictureWorkbookAddLastMax() {
        return createWorkbook(
            ImmutableList.of("name", "numeric", "enum", "IsSku", "numericEnum", "", "", "", ""),
            ImmutableList.of("SET", "SET_IF_EMPTY", "APPEND", "SET", "SET_IF_EMPTY",
                "ADD_LAST_PICTURE", "ADD_LAST_PICTURE", "ADD_LAST_PICTURE", "ADD_LAST_PICTURE"),
            ImmutableList.of(1L),
            "qwerty", "10", "enum3||enum4", "false", "200",
            PICTURE1_URL, PICTURE2_URL, PICTURE3_URL, PICTURE4_URL
        );
    }

    // result = PICTURE1_URL , PICTURE2_URL, PICTURE3_URL
    private Workbook createPictureWorkbookAddMax() {
        return createWorkbook(
            ImmutableList.of("name", "numeric", "enum", "IsSku", "numericEnum", "", "", "", ""),
            ImmutableList.of("SET", "SET_IF_EMPTY", "APPEND", "SET", "SET_IF_EMPTY",
                "ADD_FIRST_PICTURE", "ADD_FIRST_PICTURE", "ADD_FIRST_PICTURE", "ADD_FIRST_PICTURE"),
            ImmutableList.of(1L),
            "qwerty", "10", "enum3||enum4", "false", "200",
            PICTURE1_URL, PICTURE2_URL, PICTURE3_URL, PICTURE4_URL
        );
    }

    // result = PICTURE1_URL , PICTURE2_URL, PICTURE3_URL
    private Workbook createPictureWorkbookAddAddLAddSet() {
        return createWorkbook(
            ImmutableList.of("name", "numeric", "enum", "IsSku", "numericEnum", "", "", "", ""),
            ImmutableList.of("SET", "SET_IF_EMPTY", "APPEND", "SET", "SET_IF_EMPTY",
                "ADD_FIRST_PICTURE", "ADD_FIRST_PICTURE", "ADD_LAST_PICTURE", "SET_FIRST_PICTURE"),
            ImmutableList.of(1L),
            "qwerty", "10", "enum3||enum4", "false", "200",
            PICTURE1_URL, PICTURE2_URL, PICTURE3_URL, PICTURE1_URL
        );
    }


    // result = PICTURE1_URL
    private Workbook createPictureWorkbookSet() {
        return createWorkbook(
            ImmutableList.of("name", "numeric", "enum", "IsSku", "numericEnum", ""),
            ImmutableList.of("SET", "SET_IF_EMPTY", "APPEND", "SET", "SET_IF_EMPTY",
                "SET_FIRST_PICTURE"),
            ImmutableList.of(1L),
            "qwerty", "10", "enum3||enum4", "false", "200",
            PICTURE1_URL
        );
    }

    private Workbook createPictureWorkbookBrokenPictureAdd() {
        return createWorkbook(
            ImmutableList.of("name", "numeric", "enum", "IsSku", "numericEnum", "", "-"),
            ImmutableList.of("SET", "SET_IF_EMPTY", "APPEND", "SET", "SET_IF_EMPTY",
                "ADD_FIRST_PICTURE", "ADD_FIRST_PICTURE"),
            ImmutableList.of(1L),
            "qwerty", "10", "enum3||enum4", "false", "200",
            PICTURE1_URL, PICTURE_FAIL_URL
        );
    }

    private Workbook createPictureWorkbookDoubleAdd() {
        return createWorkbook(
            ImmutableList.of("name", "numeric", "enum", "IsSku", "numericEnum", "", "-"),
            ImmutableList.of("SET", "SET_IF_EMPTY", "APPEND", "SET", "SET_IF_EMPTY",
                "ADD_FIRST_PICTURE", "ADD_FIRST_PICTURE"),
            ImmutableList.of(1L),
            "qwerty", "10", "enum3||enum4", "false", "200",
            PICTURE1_URL, PICTURE1_URL
        );
    }


    private Workbook createPictureWorkbookCleanEmpty() {
        return createWorkbook(
            ImmutableList.of("name", "numeric", "enum", "IsSku", "numericEnum", "", "-"),
            ImmutableList.of("SET", "SET_IF_EMPTY", "APPEND", "SET", "SET_IF_EMPTY",
                "CLEAN_PICTURES", "CLEAN_PICTURES"),
            ImmutableList.of(1L),
            "qwerty", "10", "enum3||enum4", "false", "200",
            PICTURE1_URL, REMOVE_FLAG
        );
    }

    private Workbook createDefaultWorkbook() {
        return createWorkbook(
            ImmutableList.of("name", "numeric", "enum", "IsSku", "numericEnum"),
            ImmutableList.of("SET", "SET_IF_EMPTY", "APPEND", "SET", "SET_IF_EMPTY"),
            ImmutableList.of(1L, 2L),
            "qwerty", "10", "enum3||enum4", "false", "200",
            "qwerty1,;[]\\/|", "20", "enum4||enum5", "true", "200||300"
        );
    }

    private Workbook createSizeMeasureWorkbook(List<SizeMeasureDto> measures, List<Long> modelIds, String... values) {
        return createWorkbook(
            measures.stream()
                .map(this::generateMeasureParamXslName)
                .collect(Collectors.toList()),
            Collections.nCopies(measures.size(), "SET"),
            modelIds,
            values
        );
    }

    public List<CommonModel> createModels(boolean isPublished) {
        return ImmutableList.of(
            createModel(
                1L,
                ImmutableList.of(ParametersBuilder.ENUM3_OPTION),
                ImmutableList.of("test", "test1"),
                ImmutableList.of(1.0),
                false,
                ImmutableList.of(ParametersBuilder.NUMERIC_ENUM8_OPTION, ParametersBuilder.NUMERIC_ENUM9_OPTION),
                isPublished),
            createModel(
                2L,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                Collections.emptyList(),
                isPublished
            ));
    }

    public CommonModel createModel(Long id,
                                   List<Long> enumIds,
                                   List<String> stringValues,
                                   List<Double> numericValues,
                                   Boolean boolValue,
                                   List<Long> numericEnumIds,
                                   boolean isPublished) {
        CommonModelBuilder<CommonModel> result = modelBuilder.startModel()
            .id(id)
            .category(CATEGORY_HID)
            .source(CommonModel.Source.GURU)
            .currentType(CommonModel.Source.GURU)
            .published(isPublished)
            .vendorId(ParametersBuilder.GLOBAL_VENDOR_1_ID);

        enumIds.forEach(enumId -> result.param(ParametersBuilder.ENUM_PARAM_ID).setOption(enumId));
        if (!stringValues.isEmpty()) {
            result.param(ParametersBuilder.STRING_PARAM_ID).setString(stringValues);
        }
        numericValues.forEach(numeric -> result.param(ParametersBuilder.NUMERIC_PARAM_ID).setNumeric(numeric));
        if (boolValue != null) {
            ThinCategoryParam param = modelBuilder.getParamDescription(ParametersBuilder.BOOL_PARAM_ID);
            Option option = param.getOptions().stream()
                .filter(o -> o.getName().equals(boolValue.toString().toUpperCase()))
                .findFirst().orElseThrow(IllegalArgumentException::new);
            result.param(ParametersBuilder.BOOL_PARAM_ID).setBoolean(boolValue).setOption(option.getId());
        }
        numericEnumIds.forEach(enumId -> result.param(ParametersBuilder.NUMERIC_ENUM_PARAM_ID).setOption(enumId));
        return result.endModel();
    }

    private byte[] getUploadFileData(Long importId) {
        return importHandler.downloadInputFile(importId);
    }
}
