package ru.yandex.market.robot.tasks.crawlers;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.robot.db.RobotTmsDao;
import ru.yandex.market.robot.db.sqlite.SqliteLocalStorage;
import ru.yandex.market.robot.db.storage.FileStorageService;
import ru.yandex.market.robot.patterns.Extractor;
import ru.yandex.market.robot.shared.models.Entity;
import ru.yandex.market.robot.shared.models.RobotTaskInfo;
import ru.yandex.market.robot.shared.models.Source;
import ru.yandex.market.robot.shared.models.SourceTask;
import ru.yandex.market.robot.shared.patterns.ExtractedPattern;
import ru.yandex.market.robot.tasks.RobotTaskContext;
import ru.yandex.market.robot.tasks.extractors.ExtractorTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;

@Ignore
public class StartTaskTest {
    private static final int SOURCE_TASK_ID = 1;
    private static final int SOURCE_ID = 2;
    private static final int TASK_ID = 3;
    private static final int TASK_TIMEOUT_MILLIS = 5000;
    private static final int SESSION_ID = 4;
    private static final int TASK_PRIORITY = 9;
    private static final int ENTITY_ID = 1001;

    private JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

    private RobotTmsDao newRobotTmsDao() {
        RobotTmsDao robotTmsDao = mock(RobotTmsDao.class);
        Mockito.doAnswer(invocation -> {
            return getPatterns();
        }).when(robotTmsDao).getPatterns(anyInt(), anyInt());

        return robotTmsDao;
    }

    /**
     * Стенд для запуска задач.
     * Шедуллер В таблице
     *
     * @see ru.yandex.market.robot.services.RobotSessionService#startNewSessions
     * @see ru.yandex.market.robot.services.RobotTaskService#startNewTasks
     * @see ru.yandex.market.robot.services.RobotTaskService#createRobotTask
     */
    @Test
    public void test() throws Exception {
        RobotTaskContext context = newRobotTaskContext();
        GetSinglePageTask getSinglePageTask = newGetSinglePageTask(context);

        getSinglePageTask.init();
    }

    private RobotTaskContext newRobotTaskContext() throws Exception {
        return new RobotTaskContext(
            getTaskInfo(),
            newSource(),
            newEntity(),
            new HashMap<>(),
            newSqliteLocalStorage()
        );
    }

    private Source newSource() {
        return new Source(1, "http://ya.ru");
    }

    private Entity newEntity() {
        Entity entity = new Entity();
        entity.setId(ENTITY_ID);
        return entity;
    }

    private SqliteLocalStorage newSqliteLocalStorage() throws Exception {
        SqliteLocalStorage storage = new SqliteLocalStorage();
        storage.setTmpPath("src/test/resources/tmp");
        storage.setStoragePath("src/test/resources");
        storage.setDriverClassName("org.sqlite.JDBC");
        storage.setUrl("jdbc:sqlite:");

        FileStorageService fileStorageService = mock(FileStorageService.class);
        storage.setFileStorageService(fileStorageService);

        storage.afterPropertiesSet();

        return storage;
    }

    private RobotTaskInfo getTaskInfo() {
        RobotTaskInfo taskInfo = new RobotTaskInfo();
        taskInfo.setSourceTaskId(SOURCE_TASK_ID);
        taskInfo.setStatus(RobotTaskInfo.TaskStatus.STARTED);
        taskInfo.setSourceId(SOURCE_ID);
        taskInfo.setName("name");
        taskInfo.setSourceTaskStatus(SourceTask.Status.RUNNING);
        taskInfo.setDependTaskId(0);
        taskInfo.setEntityId(1);

        taskInfo.setId(TASK_ID);
        taskInfo.setTimeout(TASK_TIMEOUT_MILLIS);
        taskInfo.setSessionId(SESSION_ID);
        taskInfo.setPriority(TASK_PRIORITY);

        return taskInfo;
    }

    private GetSinglePageTask newGetSinglePageTask(RobotTaskContext context) {
        GetSinglePageTask getSinglePageTask = new GetSinglePageTask();

        ExtractorTask extractorTask = newExtractorTask(jdbcTemplate, transactionTemplate);
        getSinglePageTask.setExtractorTask(extractorTask);

        getSinglePageTask.setContext(context);

        return getSinglePageTask;
    }

    private ExtractorTask newExtractorTask(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        ExtractorTask extractorTask = new ExtractorTask();
        extractorTask.setJdbcTemplate(jdbcTemplate);
        extractorTask.setTransactionTemplate(transactionTemplate);
        extractorTask.setRobotTmsDao(newRobotTmsDao());
        return extractorTask;
    }

    private Map<String, List<Extractor>> getPatterns() {
        final Map<String, List<Extractor>> result = new HashMap<>();

        String pageType = "offer";

        int entityId = 1;

        String fieldName = "name";
        int fieldId = 1;
        String fieldType = "TrimText";
        String type = "XPath";

        Map<String, ExtractedPattern> fieldsPatterns = new HashMap<>();
        fieldsPatterns.put(
            fieldName, new ExtractedPattern("//DIV[translate(@id,' ','') = 'urun_sayfasi_baslik']/H1", type)
        );

        List<Extractor> typePatterns = new ArrayList<>();
        typePatterns.add(
            new Extractor(
                entityId,
                null,
                fieldsPatterns
            )
        );
        result.put(pageType, typePatterns);

        return result;
    }
}
