package ru.yandex.market.ir.pipeline;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.common.services.translate.Language;
import ru.yandex.market.gutgin.tms.config.TestContextInitializer;
import ru.yandex.market.gutgin.tms.engine.schedule.PipelineScheduler;
import ru.yandex.market.gutgin.tms.engine.schedule.TaskScheduler;
import ru.yandex.market.gutgin.tms.mocks.ThreadPoolExecutorMock;
import ru.yandex.market.gutgin.tms.service.FileDownloadService;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration.common.rating.DefaultRatingEvaluator;
import ru.yandex.market.ir.autogeneration.common.rating.SkuRatingEvaluator;
import ru.yandex.market.ir.config.CommonPipelineConfig;
import ru.yandex.market.ir.excel.TestExcelFileGenerator;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.excel.generator.XslInfo;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.dao.PipelineService;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.dao.TaskDao;
import ru.yandex.market.partner.content.common.db.dao.TaskService;
import ru.yandex.market.partner.content.common.db.jooq.enums.MrgrienPipelineStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.enums.ProcessStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.TaskStatus;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.entity.PriorityIdentityWrapper;
import ru.yandex.market.partner.content.common.mocks.CategoryParametersFormParserMock;
import ru.yandex.market.partner.content.common.service.MdsFileStorageService;
import ru.yandex.market.partner.content.common.service.PartnerContentFileService;
import ru.yandex.market.robot.db.ParameterValueComposer;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.SERVICE_INSTANCE;

/**
 * @author danfertev
 * @since 02.07.2019
 */
@ContextConfiguration(initializers = {TestContextInitializer.class})
public abstract class BasePipelineTest extends BaseDbCommonTest {

    private static final String SERVICE_HOST = "test.host";
    private static final int SERVICE_PORT = 8080;
    private static final String SOURCE_NAME = "test_source";
    private static final String SOURCE_URL = "test_source_url";

    protected static final int FIRST_SKU_ROW = 7;
    protected static final int SOURCE_ID = 1;
    protected static final int PARTNER_SHOP_ID = 11;
    protected static final String DESCRIPTION_XSL_NAME = "description";

    protected static final long CATEGORY1_ID = 1L;

    protected static final String DEFAULT_BLOCK_NAME = "Общие характеристики";
    protected static final long VENDOR1_ID = 1;
    protected static final String VENDOR1_NAME = "vendor 1";

    protected static final String FILE_URL = "http://good-file-url";
    protected static final String TEMP_FILE = "good-temp-file";
    protected static final String MDS_FILE_BUCKET = "good-mds-bucket";
    protected static final String MDS_FILE_KEY = "good-mds-key";
    protected static final String MDS_FILE_URL = "good-mds-url";

    protected static final String MAIN_PIC_URL = "http://main-pic-url";


    @Resource
    protected SourceDao sourceDao;

    @Resource
    protected CategoryDataKnowledgeMock categoryDataKnowledgeMock;

    @Resource
    protected CategoryParametersFormParserMock categoryParametersFormParserMock;

    @Resource
    protected CategoryInfoProducer categoryInfoProducer;

    protected SkuRatingEvaluator skuRatingEvaluator;

    @Resource
    protected CategoryDataHelper categoryDataHelper;

    @Resource
    protected PartnerContentFileService partnerContentFileService;

    @Resource
    protected FileDownloadService fileDownloadService;

    @Resource
    protected MdsFileStorageService mdsFileStorageService;

    @Resource
    protected PipelineScheduler pipelineScheduler;

    @Resource
    protected ThreadPoolExecutorMock pipelinesProcessService;

    @Resource
    protected PipelineService pipelineService;

    @Resource
    protected TaskScheduler taskScheduler;

    @Resource
    protected ThreadPoolExecutorMock tasksProcessService;

    @Resource
    protected TaskDao taskDao;

    @Resource
    protected TaskService taskService;

    protected TestExcelFileGenerator.Builder generatorBuilder;

    @Before
    public void setUp() {
        dsl()
            .insertInto(SERVICE_INSTANCE)
            .set(SERVICE_INSTANCE.ID, CommonPipelineConfig.CURRENT_SERVICE_INSTANCE_ID)
            .set(SERVICE_INSTANCE.HOST, SERVICE_HOST)
            .set(SERVICE_INSTANCE.PORT, SERVICE_PORT)
            .set(SERVICE_INSTANCE.LAST_ALIVE_TIME, new Timestamp(System.currentTimeMillis()))
            .set(SERVICE_INSTANCE.IS_ALIVE, true)
            .execute();

        sourceDao.mergeSource(SOURCE_ID, SOURCE_NAME, SOURCE_URL, PARTNER_SHOP_ID);

        MboParameters.Category.Builder category = buildCategory();
        categoryDataKnowledgeMock.addCategoryData(CATEGORY1_ID, CategoryData.build(category));
        skuRatingEvaluator = new DefaultRatingEvaluator(categoryDataKnowledgeMock);

        Map<String, XslInfo> xslInfoMap = new HashMap<>();
        Collection<MboParameters.Parameter> parameterList = category.getParameterList()
            .stream().filter(p -> !p.getService() && !p.getHidden())
            .collect(Collectors.toList());
        int offset = 0;
        for (MboParameters.Parameter parameter : parameterList) {
            xslInfoMap.put(parameter.getXslName(), new XslInfo(offset++, DEFAULT_BLOCK_NAME));
        }
        categoryParametersFormParserMock.addCategoryAttrs(CATEGORY1_ID, xslInfoMap);

        generatorBuilder = TestExcelFileGenerator.Builder.newInstance()
            .setCategoryInfoProducer(categoryInfoProducer)
            .setCategoryDataHelper(categoryDataHelper)
            .setSkuRatingEvaluator(skuRatingEvaluator);

        mockFileDownloadServiceSuccess(SOURCE_ID, FILE_URL, TEMP_FILE);
        mockMdsFileUploadSuccess(SOURCE_ID, TEMP_FILE, MDS_FILE_BUCKET, MDS_FILE_KEY, MDS_FILE_URL);
    }


    protected MboParameters.Category.Builder buildCategory() {
        return MboParameters.Category.newBuilder()
            .setHid(CATEGORY1_ID)
            .setLeaf(true)
            .addName(MboParameters.Word.newBuilder().setName("Category " + CATEGORY1_ID).setLangId(225))
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.NAME_ID).setXslName(ParameterValueComposer.NAME)
                .setValueType(MboParameters.ValueType.STRING)
                .setService(true))
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.VENDOR_ID).setXslName(ParameterValueComposer.VENDOR)
                .setValueType(MboParameters.ValueType.ENUM)
                .addName(MboParameters.Word.newBuilder().setLangId(Language.RUS.getId()).setName("производитель"))
                .addOption(MboParameters.Option.newBuilder()
                    .setId(VENDOR1_ID)
                    .addName(MboParameters.Word.newBuilder().setLangId(Language.RUS.getId()).setName(VENDOR1_NAME))
                    .setIsGuruVendor(true)))
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.BARCODE_ID).setXslName(ParameterValueComposer.BARCODE)
                .setValueType(MboParameters.ValueType.STRING)
                .addName(MboParameters.Word.newBuilder().setLangId(Language.RUS.getId()).setName("Баркод")))
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.VENDOR_CODE_ID).setXslName(ParameterValueComposer.VENDOR_CODE)
                .setValueType(MboParameters.ValueType.STRING))
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.ALIASES_ID).setXslName(ParameterValueComposer.ALIASES)
                .setValueType(MboParameters.ValueType.STRING))
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.URL_ID).setXslName(ParameterValueComposer.URL)
                .setValueType(MboParameters.ValueType.STRING)
                .setService(true))
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(MainParamCreator.DESCRIPTION_ID).setXslName(DESCRIPTION_XSL_NAME)
                .setValueType(MboParameters.ValueType.STRING)
                .addName(MboParameters.Word.newBuilder().setLangId(Language.RUS.getId()).setName("Описание")))
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(MainParamCreator.SHOP_SKU_PARAM_ID).setXslName(MainParamCreator.SHOP_SKU_XSL_NAME)
                .setValueType(MboParameters.ValueType.STRING)
                .addName(MboParameters.Word.newBuilder().setLangId(Language.RUS.getId()).setName("Оригинальный SKU")));
    }

    protected void runPipeline(long pipelineId) {
        boolean finished;
        do {
            pipelineScheduler.doCheckPipelines();
            runAllAndRemove(pipelinesProcessService.getRunnableList().iterator());
            List<PriorityIdentityWrapper> availableTaskIds = taskService.getAvailableForProcessTaskIds();
            taskScheduler.doCheckTasks();
            runAllAndRemove(tasksProcessService.getRunnableList().iterator());

            Pipeline pipeline = pipelineService.getPipeline(pipelineId);
            boolean pipelineFinished = pipeline.getStatus() == MrgrienPipelineStatus.FINISHED;
            boolean pipelineFailed = pipeline.getStatus() == MrgrienPipelineStatus.WAIT_AFTER_FAIL;

            // ранее тест проходил, так как считалось, что количество потоков в тесте бесконечно
            if (!availableTaskIds.isEmpty() &&
                availableTaskIds
                    .stream()
                    .anyMatch(t -> taskService.getTask(t.getId()).getStatus() != TaskStatus.FINISHED)
            ) {
                finished = false;
                continue;
            }

            boolean taskFinished = availableTaskIds.isEmpty() || availableTaskIds.stream()
                .allMatch(tid -> taskService.getLastProcessInfo(tid.getId())
                        .getStatus() == ProcessStatus.FINISHED);
            boolean taskFailed = availableTaskIds.stream()
                .anyMatch(tid -> taskService.getLastProcessInfo(tid.getId()).getStatus() == ProcessStatus.FAILED);

            finished = pipelineFailed || taskFailed || pipelineFinished && taskFinished;
        } while (!finished);
    }

    private void runAllAndRemove(Iterator<Runnable> runnables) {
        while (runnables.hasNext()) {
            Runnable runnable = runnables.next();
            runnable.run();
            runnables.remove();
        }
    }

    protected void assertAllTasksFinished(long pipelineId) {
        Assertions.assertThat(taskDao.getTasks(pipelineId))
            .allMatch(task -> task.getStatus() == TaskStatus.FINISHED);
    }

    protected long getLastPipelineId() {
        return getLastPipelineId(PipelineType.GOOD_CONTENT_SINGLE_XLS);
    }

    protected long getLastPipelineId(PipelineType pipelineType) {
        return pipelineService.getLastPipeline(pipelineType).get().getId();
    }

    protected Pipeline getPipeline(long pipelineId) {
        return pipelineService.getPipeline(pipelineId);
    }

    protected PartnerContent.FileInfoResponse getFileInfoResponse(long processRequestId) {
        return partnerContentFileService.getFileInfoResponse(
            PartnerContent.ProcessRequest.newBuilder()
                .setProcessRequestId(processRequestId)
                .build()
        );
    }

    protected void mockFileDownloadServiceSuccess(int sourceId, String url, String tempFile) {
        when(fileDownloadService.downloadToTempFile(eq(sourceId), eq(url), any())).thenReturn(Paths.get(tempFile));
    }

    protected void mockMdsFileUploadSuccess(int sourceId, String tempFile,
                                            String mdsFileBucket, String mdsFileKey, String mdsFileUrl) {
        when(mdsFileStorageService.upload(eq(Paths.get(tempFile)), eq(sourceId), any(), anyString(), any()))
            .thenReturn(new MdsFileStorageService.MdsFileInfo(mdsFileBucket, mdsFileKey, mdsFileUrl));
    }

    protected void mockMdsFileDownloadSuccess(long categoryId, int sourceId, List<String> shopSkus,
                                              String mdsBucket, String mdsKey,
                                              TestExcelFileGenerator generator) {
        doAnswer(args -> {
            InputStream in = new ByteArrayInputStream(generator.generate(categoryId, sourceId, shopSkus));
            Path tempFilePath = args.getArgument(2);
            FileUtils.copyInputStreamToFile(in, tempFilePath.toFile());

            return null;
        }).when(mdsFileStorageService).download(eq(mdsBucket), eq(mdsKey), any(Path.class));
    }
}
