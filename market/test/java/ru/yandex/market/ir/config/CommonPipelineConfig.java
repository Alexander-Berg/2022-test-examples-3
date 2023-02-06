package ru.yandex.market.ir.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.gutgin.tms.db.dao.ProblemInfoDao;
import ru.yandex.market.gutgin.tms.engine.locking.LockService;
import ru.yandex.market.gutgin.tms.engine.pipeline.PipelineProcessor;
import ru.yandex.market.gutgin.tms.engine.pipeline.PipelineTemplateHolder;
import ru.yandex.market.gutgin.tms.engine.pipeline.StepProcessorHolder;
import ru.yandex.market.gutgin.tms.engine.schedule.PipelineScheduler;
import ru.yandex.market.gutgin.tms.engine.schedule.TaskScheduler;
import ru.yandex.market.gutgin.tms.engine.service.CleanerService;
import ru.yandex.market.gutgin.tms.engine.service.ServiceInstanceInfo;
import ru.yandex.market.gutgin.tms.mocks.CleanWebServiceMock;
import ru.yandex.market.gutgin.tms.mocks.ScheduledExecutorServiceMock;
import ru.yandex.market.gutgin.tms.mocks.ThreadPoolExecutorMock;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku.SkuDuplicateService;
import ru.yandex.market.gutgin.tms.service.FileDownloadService;
import ru.yandex.market.gutgin.tms.service.GutginResourcesReader;
import ru.yandex.market.gutgin.tms.service.health.TskvWriterMockImpl;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration_api.http.service.MboMappingsServiceMock;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.ir.excel.generator.CategoryParametersFormParser;
import ru.yandex.market.ir.http.MarkupService;
import ru.yandex.market.mbo.export.CategoryModelsService;
import ru.yandex.market.mbo.http.ModelStorageService;
import ru.yandex.market.mbo.http.SkuBDApiService;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.partner.content.common.config.CommonTestConfig;
import ru.yandex.market.partner.content.common.db.dao.ApplicationPropertyDao;
import ru.yandex.market.partner.content.common.db.dao.PartnerContentDao;
import ru.yandex.market.partner.content.common.db.dao.PipelineService;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.dao.TaskProcessDao;
import ru.yandex.market.partner.content.common.db.dao.TaskService;
import ru.yandex.market.partner.content.common.db.dao.TemplateFeedUploadDao;
import ru.yandex.market.partner.content.common.db.dao.WaitUntilFinishedPipelineDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.engine.manager.PipelineManager;
import ru.yandex.market.partner.content.common.mocks.CategoryParametersFormParserMock;
import ru.yandex.market.partner.content.common.service.ApplicationPropertyService;
import ru.yandex.market.partner.content.common.service.MdsFileStorageService;
import ru.yandex.market.partner.content.common.service.PartnerContentFileService;
import ru.yandex.market.partner.content.common.service.ResourceReader;
import ru.yandex.market.partner.content.common.service.logging.PipelineTimingsLogger;
import ru.yandex.passport.tvmauth.TvmClient;

/**
 * @author danfertev
 * @since 02.07.2019
 */
@Configuration
@Import({
    CommonTestConfig.class
})
public class CommonPipelineConfig {
    private static final Logger log = LogManager.getLogger();

    public static final long CURRENT_SERVICE_INSTANCE_ID = 12234L;
    private static final long CHECK_DELAY_IN_SEC = 0L;
    private static final int TASKS_COUNT = 4;
    private static final long DELAY_AFTER_FAIL_IN_SEC = 0L;
    private static final long DELAY_AFTER_NOT_SUCCESS_IN_SEC = 0L;

    @Bean(name = "currentServiceInstance")
    public ServiceInstanceInfo currentServiceInstance() {
        return new ServiceInstanceInfo(CURRENT_SERVICE_INSTANCE_ID);
    }

    @Bean
    public PartnerContentFileService partnerContentFileService(
        PartnerContentDao partnerContentDao,
        PipelineManager pipelineManager,
        GcSkuTicketDao gcSkuTicketDao,
        SourceDao sourceDao,
        TemplateFeedUploadDao templateFeedUploadDao
    ) {
        return new PartnerContentFileService(
            partnerContentDao,
            pipelineManager,
            gcSkuTicketDao,
            sourceDao,
            templateFeedUploadDao,
            Mockito.mock(MdsFileStorageService.class));
    }

    @Bean
    public ThreadPoolExecutorMock pipelinesProcessService() {
        return ThreadPoolExecutorMock.newInstance();
    }

    @Bean
    public PipelineTimingsLogger pipelineTimingsLogger() {
        return new PipelineTimingsLogger(new TskvWriterMockImpl());
    }

    @Bean
    public ThreadPoolExecutorMock tasksProcessService() {
        return ThreadPoolExecutorMock.newInstance();
    }

    @Bean
    public PipelineProcessor pipelineProcessor(
        TaskService taskService
    ) {
        log.info("Init PipelineProcessor");
        final PipelineProcessor pipelineProcessor = new PipelineProcessor();
        final StepProcessorHolder stepProcessorHolder = stepProcessors(pipelineProcessor, taskService);
        pipelineProcessor.setStepProcessorHolder(stepProcessorHolder);
        return pipelineProcessor;
    }

    @SuppressWarnings({"checkstyle:parameternumber"})
    @Bean
    public PipelineScheduler pipelineScheduler(
        ProblemInfoDao problemInfoDao,
        TaskProcessDao taskProcessDao,
        PipelineService pipelineService,
        LockService lockService,
        PipelineProcessor pipelineProcessor,
        ScheduledExecutorServiceMock pipelinesCheckerService,
        ThreadPoolExecutorMock pipelinesProcessService,
        PipelineTemplateHolder pipelineTemplateHolder,
        WaitUntilFinishedPipelineDao waitUntilFinishedPipelineDao,
        PipelineTimingsLogger pipelineTimingsLogger,
        CleanerService cleanerService
    ) {
        final PipelineScheduler pipelineScheduler = new PipelineScheduler(
            pipelinesCheckerService,
            pipelinesProcessService,
            waitUntilFinishedPipelineDao,
            problemInfoDao,
            taskProcessDao,
            pipelineService,
            lockService,
            pipelineTemplateHolder,
            pipelineProcessor,
            pipelineTimingsLogger,
            cleanerService,
            10
        );
        pipelineScheduler.setCurrentServiceInstanceId(CURRENT_SERVICE_INSTANCE_ID);
        pipelineScheduler.setCheckDelayInSec(CHECK_DELAY_IN_SEC);
        pipelineScheduler.setTasksCount(TASKS_COUNT);
        pipelineScheduler.setDelayAfterFailInSec(DELAY_AFTER_FAIL_IN_SEC);
        pipelineScheduler.setDelayAfterNotSuccessInSec(DELAY_AFTER_NOT_SUCCESS_IN_SEC);
        return pipelineScheduler;
    }

    @Bean
    public CleanerService cleanerService(TaskProcessDao taskProcessDao) {
        return new CleanerService(taskProcessDao);
    }

    @Bean
    public ScheduledExecutorServiceMock tasksCheckerService() {
        return new ScheduledExecutorServiceMock();
    }

    @Bean
    public TaskScheduler taskScheduler(
        ScheduledExecutorServiceMock tasksCheckerService,
        ThreadPoolExecutorMock tasksProcessService,
        ProblemInfoDao problemInfoDao,
        PipelineService pipelineService,
        TaskService taskService,
        LockService lockService,
        PipelineTemplateHolder pipelines,
        PipelineProcessor pipelineProcessor
    ) {
        final TaskScheduler taskScheduler = new TaskScheduler(tasksCheckerService, tasksProcessService);
        taskScheduler.setProblemInfoDao(problemInfoDao);
        taskScheduler.setPipelineService(pipelineService);
        taskScheduler.setTaskService(taskService);
        taskScheduler.setLockService(lockService);
        taskScheduler.setPipelineTemplateHolder(pipelines);
        taskScheduler.setPipelineProcessor(pipelineProcessor);
        taskScheduler.setCurrentServiceInstanceId(CURRENT_SERVICE_INSTANCE_ID);
        taskScheduler.setCheckDelayInSec(CHECK_DELAY_IN_SEC);
        taskScheduler.setTasksCount(TASKS_COUNT);
        taskScheduler.setQueueSize(TASKS_COUNT);
        taskScheduler.setDelayAfterFailInSec(DELAY_AFTER_FAIL_IN_SEC);
        taskScheduler.setDelayAfterNotSuccessInSec(DELAY_AFTER_NOT_SUCCESS_IN_SEC);
        return taskScheduler;
    }

    private StepProcessorHolder stepProcessors(
        PipelineProcessor pipelineProcessor,
        TaskService taskService
    ) {
        log.info("Init StepProcessorHolder");
        StepProcessorHolder stepProcessorHolder = new StepProcessorHolder();
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.wait.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.wait.Processor<>()
        );
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.dowhile.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.dowhile.Processor<>(
                pipelineProcessor
            )
        );
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.parallelprocess.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.parallelprocess.Processor<>(
                pipelineProcessor
            )
        );
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.paralleldata.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.paralleldata.Processor<>(
                pipelineProcessor
            )
        );
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Processor<>(
                taskService
            )
        );
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.start.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.start.Processor<>()
        );
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.ifthenelse.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.ifthenelse.Processor<>(
                pipelineProcessor
            )
        );
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.processpipeline.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.processpipeline.Processor<>(
                pipelineProcessor
            )
        );
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.lightprocess.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.lightprocess.Processor<>()
        );
        return stepProcessorHolder;
    }

    @Bean
    public CategoryParametersFormParserMock categoryParametersFormParserMock() {
        return new CategoryParametersFormParserMock();
    }

    @Bean
    public CategoryParametersFormParser categoryParametersFormParser() {
        return categoryParametersFormParserMock();
    }

    @Bean
    public MarkupService markupService() {
        return Mockito.mock(MarkupService.class);
    }

    @Bean
    public MdsFileStorageService mdsFileStorageService() {
        return Mockito.mock(MdsFileStorageService.class);
    }

    @Bean
    public CategoryDataKnowledgeMock categoryDataKnowledgeMock() {
        return new CategoryDataKnowledgeMock();
    }

    @Bean
    public CategoryDataKnowledge categoryDataKnowledge(CategoryDataKnowledgeMock categoryDataKnowledgeMock) {
        return categoryDataKnowledgeMock;
    }

    @Bean
    public MboMappingsServiceMock mboMappingsServiceMock() {
        return Mockito.spy(new MboMappingsServiceMock());
    }

    @Bean
    public MboMappingsService mboMappingsService(MboMappingsServiceMock mboMappingsServiceMock) {
        return mboMappingsServiceMock;
    }

    @Bean
    public SkuBDApiService skuBDApiService() {
        return Mockito.mock(SkuBDApiService.class);
    }

    @Bean
    public Yt arnoldYtApi() {
        return Mockito.mock(Yt.class);
    }

    @Bean
    public Yt hahnYtApi() {
        return Mockito.mock(Yt.class);
    }

    @Bean
    public FileDownloadService fileDownloadService() {
        return Mockito.mock(FileDownloadService.class);
    }

    @Bean
    public ModelStorageServiceMock modelStorageServiceMock() {
        return Mockito.spy(new ModelStorageServiceMock());
    }

    @Bean(name = "model.storage.service.with.retry")
    public ModelStorageService modelStorageServiceWithRetry(ModelStorageServiceMock modelStorageServiceMock) {
        return modelStorageServiceMock;
    }

    @Bean(name = "model.storage.service.without.retry")
    public ModelStorageService modelStorageServiceWithoutRetry(ModelStorageServiceMock modelStorageServiceMock) {
        return modelStorageServiceMock;
    }

    @Bean
    public ModelStorageHelper modelStorageHelper(ModelStorageServiceMock modelStorageServiceMock) {
        return new ModelStorageHelper(modelStorageServiceMock, modelStorageServiceMock);
    }

    @Bean
    public CategoryModelsService categoryModelsService() {
        return new CategoryModelsServiceMock();
    }

    @Bean
    public TvmClient tvmClient() {
        return null;
    }

    @Bean
    public CleanWebServiceMock cleanWebServiceMock() {
        return new CleanWebServiceMock();
    }

    @Bean
    public SkuDuplicateService skuDuplicateService() {
        return Mockito.mock(SkuDuplicateService.class);
    }

    @Bean
    public GutginResourcesReader gutginResourcesReader() {
        return new GutginResourcesReader();
    }

    @Bean
    public ApplicationPropertyService propertiesService(ResourceReader resourceReader,
                                                        ApplicationPropertyDao applicationPropertyDao) {
        return new ApplicationPropertyService(resourceReader, applicationPropertyDao);
    }
}
