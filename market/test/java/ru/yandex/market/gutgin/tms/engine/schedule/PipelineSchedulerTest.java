package ru.yandex.market.gutgin.tms.engine.schedule;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.gutgin.tms.config.TestScheduleConfig;
import ru.yandex.market.gutgin.tms.mocks.ScheduledExecutorServiceMock;
import ru.yandex.market.gutgin.tms.mocks.ThreadPoolExecutorMock;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.partner.content.common.db.dao.PipelineService;
import ru.yandex.market.partner.content.common.db.jooq.enums.MrgrienPipelineStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.engine.parameter.RequestProcessFileData;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.FILE_DATA_PROCESS_REQUEST;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.FILE_PROCESS;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.SERVICE_INSTANCE;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.TASK;
import static ru.yandex.market.partner.content.common.db.jooq.enums.FileType.DCP_SINGLE_EXCEL;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class, classes = TestScheduleConfig.class)
public class PipelineSchedulerTest {
    private static final int SOURCE_ID = 100500;
    private static final int SERVICE_PORT = 8080;

    private long processRequestId;

    @Autowired
    private Configuration configuration;

    @Autowired
    private PipelineScheduler pipelineScheduler;

    @Autowired
    private PipelineService pipelineManager;

    @Autowired
    private ScheduledExecutorServiceMock pipelinesCheckerService;

    @Autowired
    private ThreadPoolExecutorMock pipelinesProcessService;

    @Autowired
    private ThreadPoolExecutorMock tasksProcessService;

    @Autowired
    private TaskScheduler taskScheduler;

    private void initDb(DSLContext dsl) {
        dsl
            .insertInto(SERVICE_INSTANCE)
            .set(SERVICE_INSTANCE.ID, TestScheduleConfig.CURRENT_SERVICE_INSTANCE_ID)
            .set(SERVICE_INSTANCE.HOST, "test.host")
            .set(SERVICE_INSTANCE.PORT, SERVICE_PORT)
            .set(SERVICE_INSTANCE.LAST_ALIVE_TIME, new Timestamp(System.currentTimeMillis()))
            .set(SERVICE_INSTANCE.IS_ALIVE, true)
            .execute();
        processRequestId = dsl.insertInto(FILE_DATA_PROCESS_REQUEST)
            .set(FILE_DATA_PROCESS_REQUEST.SOURCE_ID, SOURCE_ID)
            .set(FILE_DATA_PROCESS_REQUEST.FILE_TYPE, DCP_SINGLE_EXCEL)
            .set(FILE_DATA_PROCESS_REQUEST.URL, "http://test.ru")
            .set(FILE_DATA_PROCESS_REQUEST.CREATE_TIME, new Timestamp(System.currentTimeMillis()))
            .set(FILE_DATA_PROCESS_REQUEST.DYNAMIC, false)
            .returning(FILE_DATA_PROCESS_REQUEST.ID)
            .fetchOne()
            .getId();
    }

    @Test
    public void doCheckPipelines() throws Exception {
        DSLContext dsl = DSL.using(configuration);

        initDb(dsl);

        // Пайплайн создает ручка API для партнерского UI
        // Начинается все с PartnerContentController.addFile()
        // но здесь немного сократим путь
        RequestProcessFileData data = new RequestProcessFileData();
        data.setRequestId(processRequestId);
        pipelineManager.createPipeline(data, PipelineType.DCP_SINGLE_XLS, 1);

        // Затем пайплайн стартует по расписанию
        assertEquals(1, pipelinesCheckerService.getRunnableList().size());
        //assertEquals(pipelineScheduler::doCheckPipelines, pipelinesCheckerService.getRunnableList().get(0));
        pipelineScheduler.doCheckPipelines();

        // Старт пайплайна планирует в бекграунде задачу:
        // @see PipelineScheduler.addForProcessPipeline
        assertEquals(1, pipelinesProcessService.getRunnableList().size());
        Runnable pipelineRunnable = pipelinesProcessService.getRunnableList().get(0);
        assertEquals(PipelineScheduler.PipelineWrapper.class, pipelineRunnable.getClass());
        PipelineScheduler.PipelineWrapper pipelineWrapper = (PipelineScheduler.PipelineWrapper) pipelineRunnable;

        // Задача запускается
        // должен быть вызван PipelineProcessor.startProcessPipeline
        // потом метод internalProcess
        // в классе ru.yandex.market.gutgin.tms.engine.pipeline.template.steps.process.Processor
        pipelineWrapper.run();

        // и в таблице TASK должна появиться одна строчка
        List<String> tasks = dsl
            .select(DSL.field("input_data->'@c'", String.class))
            .from(TASK)
            .fetch(0, String.class);
        assertEquals(1, tasks.size());
        String task = tasks.get(0);
        assertEquals("\".RequestProcessFileData\"", task);

        // Из таблицы TASK задачу должен по расписанию подхватить TaskScheduler.doCheckTasks
        taskScheduler.doCheckTasks();

        // планирует в бекграунде задачу:
        assertEquals(1, tasksProcessService.getRunnableList().size());
        Runnable taskRunnable = tasksProcessService.getRunnableList().get(0);
        assertEquals(TaskScheduler.TaskWrapper.class, taskRunnable.getClass());
        TaskScheduler.TaskWrapper taskWrapper = (TaskScheduler.TaskWrapper) taskRunnable;

        // Задача запускается (SingleCategoryPipeline)
        // должен быть вызван FileProcessManager::startProcess
        // а в нем FileProcessDao.insertNewFileProcess
        taskWrapper.run();

        // и в таблице FILE_PROCESS должна появиться одна строчка
        Integer count = dsl
            .select(DSL.count())
            .from(FILE_PROCESS)
            .fetchOne(DSL.count());
        assertEquals((Integer) 1, count);
    }

    /**
     * @author s-ermakov
     */
    @Test
    public void canProcessDoesntThrowException() {
        for (MrgrienPipelineStatus status : MrgrienPipelineStatus.values()) {
            Pipeline pipeline = new Pipeline();
            pipeline.setStatus(status);
            pipeline.setUpdateDate(Timestamp.from(Instant.now()));
            pipelineScheduler.canProcess(pipeline);
        }
    }
}
