package ru.yandex.market.gutgin.tms.config;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import ru.yandex.market.gutgin.tms.db.dao.LockInfoDao;
import ru.yandex.market.gutgin.tms.db.dao.ProblemInfoDao;
import ru.yandex.market.gutgin.tms.db.dao.pipeline.DataBucketMessagesService;
import ru.yandex.market.gutgin.tms.engine.locking.LockService;
import ru.yandex.market.gutgin.tms.engine.manager.LockManager;
import ru.yandex.market.gutgin.tms.engine.pipeline.PipelineProcessor;
import ru.yandex.market.gutgin.tms.engine.pipeline.PipelineTemplateHolder;
import ru.yandex.market.gutgin.tms.engine.pipeline.StepProcessorHolder;
import ru.yandex.market.gutgin.tms.engine.pipeline.template.PipelineTemplate;
import ru.yandex.market.gutgin.tms.engine.pipeline.template.PipelineTemplateBuilder;
import ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.start.Processor;
import ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.start.Template;
import ru.yandex.market.gutgin.tms.engine.schedule.LostLockScheduler;
import ru.yandex.market.gutgin.tms.engine.schedule.PipelineScheduler;
import ru.yandex.market.gutgin.tms.engine.schedule.TaskScheduler;
import ru.yandex.market.gutgin.tms.engine.service.CleanerService;
import ru.yandex.market.gutgin.tms.mocks.ScheduledExecutorServiceMock;
import ru.yandex.market.gutgin.tms.mocks.ThreadPoolExecutorMock;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.CreateFileProcessTaskAction;
import ru.yandex.market.gutgin.tms.service.health.TskvWriterMockImpl;
import ru.yandex.market.partner.content.common.db.dao.AbstractProcessDao;
import ru.yandex.market.partner.content.common.db.dao.FileDataProcessRequestDao;
import ru.yandex.market.partner.content.common.db.dao.FileMdsCopyDao;
import ru.yandex.market.partner.content.common.db.dao.FileProcessDao;
import ru.yandex.market.partner.content.common.db.dao.PipelineService;
import ru.yandex.market.partner.content.common.db.dao.ProcessInfoDao;
import ru.yandex.market.partner.content.common.db.dao.ProtocolMessageDao;
import ru.yandex.market.partner.content.common.db.dao.TaskDao;
import ru.yandex.market.partner.content.common.db.dao.TaskProcessDao;
import ru.yandex.market.partner.content.common.db.dao.TaskService;
import ru.yandex.market.partner.content.common.db.dao.WaitUntilFinishedPipelineDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessFileData;
import ru.yandex.market.partner.content.common.engine.parameter.RequestProcessFileData;
import ru.yandex.market.partner.content.common.service.logging.PipelineTimingsLogger;

@Configuration
@Import({
    CommonDaoConfig.class,
    TestPipelineConfig.class
})
public class TestScheduleConfig {
    public static final long CURRENT_SERVICE_INSTANCE_ID = 12234L;
    private static final long CHECK_DELAY_IN_SEC = 300L;
    private static final long LOST_LOCK_CHECK_DELAY_IN_SEC = 300L;
    private static final int TASKS_COUNT = 4;
    private static final long DELAY_AFTER_FAIL_IN_SEC = 7200L;
    private static final long DELAY_AFTER_NOT_SUCCESS_IN_SEC = 3600L;

    @Bean
    DataSourceTransactionManager transactionManager(
        @Qualifier("transactionAwareDataSourceProxy") DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public PipelineTimingsLogger pipelineTimingsLogger() {
        return new PipelineTimingsLogger(new TskvWriterMockImpl());
    }

    @Bean
    public ProblemInfoDao problemInfoDao(@Qualifier("jooq.config.configuration") org.jooq.Configuration configuration) {
        return new ProblemInfoDao(configuration);
    }

    @Bean
    public ProcessInfoDao processInfoDao(@Qualifier("jooq.config.configuration") org.jooq.Configuration configuration) {
        return new ProcessInfoDao(configuration);
    }

    @Bean
    PipelineService pipelinesDao(@Qualifier("jooq.config.configuration") org.jooq.Configuration configuration,
                                 ProcessInfoDao processInfoDao,
                                 PipelineTimingsLogger pipelineTimingsLogger) {
        return new PipelineService(configuration, processInfoDao, pipelineTimingsLogger);
    }

    @Bean
    public LockInfoDao lockInfoDao(@Qualifier("jooq.config.configuration") org.jooq.Configuration configuration) {
        return new LockInfoDao(configuration);
    }

    @Bean
    public LockManager lockManager(
        LockInfoDao lockInfoDao
    ) {
        final LockManager lockManager = new LockManager();
        lockManager.setLockInfoDao(lockInfoDao);
        lockManager.setSelfServiceId(CURRENT_SERVICE_INSTANCE_ID);
        return lockManager;
    }

    @Bean
    public LockService lockService(
        LockManager lockManager,
        LostLockScheduler lostLockScheduler
        ) {
        LockService lockService = new LockService();
        lockService.setLockManager(lockManager);
        lockService.setLostLockScheduler(lostLockScheduler);
        return lockService;
    }

    @Bean
    public TaskService tasksDao(@Qualifier("jooq.config.configuration") org.jooq.Configuration configuration,
                                ProcessInfoDao processInfoDao,
                                TaskDao taskDao) {
        return new TaskService(configuration, processInfoDao, taskDao, new ArrayList<>());
    }

    @Bean
    public PipelineProcessor pipelineProcessor(
        TaskService taskService
    ) {
        final PipelineProcessor pipelineProcessor = new PipelineProcessor();
        final StepProcessorHolder stepProcessorHolder = stepProcessors(taskService);
        pipelineProcessor.setStepProcessorHolder(stepProcessorHolder);
        return pipelineProcessor;
    }

    @Bean
    ProtocolMessageDao protocolMessageDao(@Qualifier("jooq.config.configuration") org.jooq.Configuration configuration) {
        return new ProtocolMessageDao(configuration);
    }

    @Bean
    DataBucketMessagesService badExamplesDao(
        @Qualifier("jooq.config.configuration") org.jooq.Configuration configuration,
        ProtocolMessageDao protocolMessageDao
    ) {
        return new DataBucketMessagesService(configuration, protocolMessageDao);
    }

    public StepProcessorHolder stepProcessors(
        TaskService taskService
    ) {
        StepProcessorHolder stepProcessorHolder = new StepProcessorHolder();
        stepProcessorHolder.addStepProcessors(
            Template.class,
            new Processor<>()
        );
        stepProcessorHolder.addStepProcessors(
            ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Template.class,
            new ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Processor<>(
                taskService
            )
        );

        return stepProcessorHolder;
    }

    @Bean
    FileDataProcessRequestDao fileDataProcessRequestDao(@Qualifier("jooq.config.configuration") org.jooq.Configuration configuration) {
        return new FileDataProcessRequestDao(configuration);
    }

    @Bean
    FileProcessDao fileProcessDao(@Qualifier("jooq.config.configuration") org.jooq.Configuration configuration) {
        return new FileProcessDao(configuration, new AbstractProcessDao(configuration));
    }

    @Bean
    FileMdsCopyDao fileMdsCopyDao(@Qualifier("jooq.config.configuration") org.jooq.Configuration configuration) {
        return new FileMdsCopyDao(configuration);
    }

    @Bean
    public ScheduledExecutorServiceMock pipelinesCheckerService() {
        return new ScheduledExecutorServiceMock();
    }

    @Bean
    public ThreadPoolExecutorMock pipelinesProcessService() {
        return ThreadPoolExecutorMock.newInstance();
    }

    @SuppressWarnings({"checkstyle:parameternumber"})
    @Bean(initMethod = "init")
    public PipelineScheduler pipelineScheduler(
        ProblemInfoDao problemInfoDao,
        ProcessInfoDao processInfoDao,
        LockInfoDao lockInfoDao,
        TaskProcessDao taskProcessDao,
        PipelineService pipelineService,
        LockService lockService,
        FileDataProcessRequestDao fileDataProcessRequestDao,
        FileProcessDao fileProcessDao,
        PipelineProcessor pipelineProcessor,
        PipelineTimingsLogger pipelineTimingsLogger,
        ScheduledExecutorService pipelinesCheckerService,
        ThreadPoolExecutorMock pipelinesProcessService,
        WaitUntilFinishedPipelineDao waitUntilFinishedPipelineDao
    ) {

        CreateFileProcessTaskAction createFileProcessTaskAction = new CreateFileProcessTaskAction();
        createFileProcessTaskAction.setFileDataProcessRequestDao(fileDataProcessRequestDao);
        createFileProcessTaskAction.setFileProcessDao(fileProcessDao);

        PipelineTemplate<RequestProcessFileData, ProcessFileData> pipeline =
                PipelineTemplateBuilder.<RequestProcessFileData>get()
                    .process(createFileProcessTaskAction)
                    .build();


        PipelineTemplateHolder pipelines = new PipelineTemplateHolder();
        pipelines.addPipeline(PipelineType.DCP_SINGLE_XLS, 1, pipeline);

        final PipelineScheduler pipelineScheduler = new PipelineScheduler(
            pipelinesCheckerService,
            pipelinesProcessService,
            waitUntilFinishedPipelineDao,
            problemInfoDao,
            taskProcessDao,
            pipelineService,
            lockService,
            pipelines,
            pipelineProcessor,
            pipelineTimingsLogger,
            new CleanerService(taskProcessDao),
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
    @SuppressWarnings("checkstyle:magicnumber")
    public PipelineTemplateHolder pipelines(
            FileDataProcessRequestDao fileDataProcessRequestDao,
            FileProcessDao fileProcessDao
    ) {

        CreateFileProcessTaskAction createFileProcessTaskAction = new CreateFileProcessTaskAction();
        createFileProcessTaskAction.setFileDataProcessRequestDao(fileDataProcessRequestDao);
        createFileProcessTaskAction.setFileProcessDao(fileProcessDao);

        PipelineTemplate<RequestProcessFileData, ProcessFileData> pipeline =
                PipelineTemplateBuilder.<RequestProcessFileData>get()
                        .process(createFileProcessTaskAction)
                        .build();


        PipelineTemplateHolder pipelines = new PipelineTemplateHolder();
        pipelines.addPipeline(PipelineType.DCP_SINGLE_XLS, 1, pipeline);
        return pipelines;
    }

    @Bean
    public ScheduledExecutorServiceMock tasksCheckerService() {
        return new ScheduledExecutorServiceMock();
    }

    @Bean
    public ThreadPoolExecutorMock tasksProcessService() {
        return ThreadPoolExecutorMock.newInstance();
    }

    @SuppressWarnings({"checkstyle:parameternumber"})
    @Bean(initMethod = "init")
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

    @Bean
    public ScheduledExecutorServiceMock lostLockCheckerService() {
        return new ScheduledExecutorServiceMock();
    }

    @Bean(initMethod = "init")
    public LostLockScheduler lostLockScheduler(
        LockManager lockManager,
        ScheduledExecutorService lostLockCheckerService
    ) {
        LostLockScheduler lostLockScheduler = new LostLockScheduler();
        lostLockScheduler.setCheckDelayInSec(LOST_LOCK_CHECK_DELAY_IN_SEC);
        lostLockScheduler.setCurrentServiceInstanceId(CURRENT_SERVICE_INSTANCE_ID);
        lostLockScheduler.setLockManager(lockManager);
        lostLockScheduler.setPipelinesCheckerService(lostLockCheckerService);
        return lostLockScheduler;
    }
}
