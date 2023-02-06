package ru.yandex.market.pricelabs.tms;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mockito.Mockito;
import org.opentest4j.AssertionFailedError;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.misc.ToJsonString;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.Shop;
import ru.yandex.market.pricelabs.model.ShopsDat;
import ru.yandex.market.pricelabs.model.types.ShopType;
import ru.yandex.market.pricelabs.services.database.model.Job;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.services.database.model.Task;
import ru.yandex.market.pricelabs.services.database.model.TaskStatus;
import ru.yandex.market.pricelabs.tms.ConfigurationForTests.Exports.ExportItem;
import ru.yandex.market.pricelabs.tms.cache.CachedDataSource;
import ru.yandex.market.pricelabs.tms.jobs.ShopLoopFullJob;
import ru.yandex.market.pricelabs.tms.jobs.TaskMonitoringJob;
import ru.yandex.market.pricelabs.tms.processing.ExecutorSources;
import ru.yandex.market.pricelabs.tms.processing.TasksController;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.imports.ShopsDatProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.ShopsProcessor;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersArg;
import ru.yandex.market.pricelabs.tms.services.database.TableRevisionService;
import ru.yandex.market.pricelabs.tms.services.database.TableServiceImpl;
import ru.yandex.market.pricelabs.tms.services.database.TasksService;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.yt.client.YtClientProxy;
import ru.yandex.market.yt.client.YtClientProxySource;
import ru.yandex.market.yt.utils.LimitedExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestControls {

    @Autowired
    private TasksService tasksService;

    @Autowired
    private TasksController tasksController;

    @Autowired
    @Qualifier("externalTableVersionService")
    private TableRevisionService tableRevisionService;

    @Autowired
    private ShopsProcessor shopProcessor;

    @Autowired
    private LimitedExecutor initExecutor;

    @Autowired
    private Supplier<ExportItem> exportServiceQueue;

    @Autowired
    private YtClientProxySource clusterSource;

    @Autowired
    private TaskMonitoringJob taskMonitoringJob;

    @Autowired
    private CachedDataSource cachedDataSource;

    @Autowired
    private ExecutorSources executors;

    @Autowired
    private YtClientProxy client;

    @Autowired
    private ShopsDatProcessor shopsDatProcessor;

    private final JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
    private final Set<Class<?>> initializers = new HashSet<>();

    public void executeJob(Executor job) {
        job.doJob(context);
    }


    public LimitedExecutor getExecutor() {
        return initExecutor;
    }

    public void executeSequential(Runnable... tasks) {
        for (var task : tasks) {
            task.run();
        }
    }

    public void executeInParallel(Runnable... tasks) {
        try {
            initExecutor.executeImmediate(tasks);
        } catch (Exception e) {
            var cause = (Throwable) e;

            while (true) {
                if (cause == null) {
                    break;
                } else if (cause instanceof AssertionFailedError) {
                    throw (AssertionFailedError) cause;
                }

                cause = cause.getCause();
            }

            throw e;
        }
    }

    public void resetExportService() {
        while (true) {
            if (exportServiceQueue.get() == null) {
                break;
            }
        }
    }

    public void cleanupTasksService() {
        tasksService.cleanAll();
    }

    public Job initShopLoopJob() {
        // Код магазина - такой, чтобы не пересекался с текущим тестовым магазином
        return initShopLoopJob(1);
    }

    public Job initShopLoopJob(int shopId) {
        // Для работы 'scheduleOffers'
        var table = Objects.requireNonNull(executors.getSourceOffersTable());
        var generation = Objects.requireNonNull(executors.getSourceGenerationTable());
        var indexer = getCurrentIndexer();
        var cluster = getCurrentCluster();
        var jobArgs = Utils.stableMap(
                ShopLoopFullJob.CLUSTER, cluster,
                ShopLoopFullJob.INDEXER, indexer,
                ShopLoopFullJob.GENERATION, generation,
                ShopLoopFullJob.OFFERS, table,
                ShopLoopFullJob.CATEGORIES, generation,
                ShopLoopFullJob.SHOPS, table);
        var taskArgs = new OffersArg()
                .setShopId(shopId)
                .setType(ShopType.DSBS)
                .setCluster(cluster)
                .setIndexer(indexer)
                .setGeneration(generation)
                .setCategoriesTable(table);
        var job = tasksService.registerJob(TasksService.UniqueType.None, JobType.SHOP_LOOP_FULL,
                ToJsonString.wrap(jobArgs), List.of(taskArgs)).orElseThrow();
        tasksService.completeJob(job, null);

        return job;
    }

    public void cleanupTableRevisions() {
        ((TableServiceImpl) tableRevisionService).clearTables();
    }

    public void resetShops() {
        YtScenarioExecutor.clearTable(this.shopProcessor.getCfg());
    }

    public void resetCaches() {
        var task = new Task();
        task.setType(JobType.SHOP_LOOP_FULL);
        task.setJob_id(System.currentTimeMillis());
    }

    public ExportItem getExportMessageFromQueue() {
        var item = exportServiceQueue.get();
        assertNotNull(item);
        return item;
    }

    public void checkNoExportMessagesInQueue() {
        assertNull(exportServiceQueue.get());
    }

    public void checkActiveJobs(Job... jobs) {
        assertEquals(List.of(jobs), tasksService.getActiveJobs());
    }

    public void checkActiveJobTypes(JobType... types) {
        assertEquals(List.of(types), tasksService.getActiveJobs().stream()
                .map(Job::getType)
                .collect(Collectors.toList()));
    }

    public List<Task> startScheduledTasks(int maxTasks, JobType jobType) {
        return startScheduledTasks(maxTasks, jobType, maxTasks);
    }

    public List<Task> startScheduledTasks(int maxTasks, JobType jobType, int expectTasks) {
        var tasks = tasksService.startTasks(maxTasks);
        assertEquals(expectTasks, tasks.size());
        // Пересортируем в оригинальном порядке
        tasks.sort(Comparator.<Task>comparingInt(t -> t.getType().isHighPriority() ? 0 : 1)
                .thenComparingLong(Task::getTask_id));
        for (Task task : tasks) {
            assertEquals(TaskStatus.RUNNING, task.getStatus());
            assertEquals(jobType, task.getType());
        }
        return tasks;
    }

    public List<Task> getShopTasks(int shopId) {
        return tasksService.getShopTasks(shopId, List.of(Utils.formatToDate(TimingUtils.getInstant())), 0, 100);
    }

    public List<Task> getJobTasks(int shopId, long jobId) {
        return tasksService.getJobTasks(shopId, jobId, 0, 100);
    }

    public Task getTask(long taskId) {
        return Objects.requireNonNull(tasksService.getTask(taskId));
    }

    public Task startScheduledTask() {
        var tasks = tasksService.startTasks(1);
        assertEquals(1, tasks.size(), () -> "Expect 1 active task, got " + tasks);
        assertEquals(TaskStatus.RUNNING, tasks.get(0).getStatus());
        return tasks.get(0);
    }

    public Task startScheduledTask(JobType expectType) {
        assertNotNull(expectType);
        var task = startScheduledTask();
        assertEquals(expectType, task.getType());
        return task;
    }

    public void checkNoScheduledTasks() {
        assertEquals(List.of(), tasksService.startTasks(1), "Expect no active tasks");
    }

    public void executeTask(Task task) {
        assertNotNull(task);
        assertEquals(TaskStatus.RUNNING, task.getStatus(), "You can execute only running task");
        tasksController.processTask(task);
    }

    public void completeTaskSuccess(Task task) {
        assertNotNull(task);
        assertEquals(TaskStatus.RUNNING, task.getStatus(), "You can close only running task");
        tasksService.completeTask(task, TaskStatus.SUCCESS, "Force close", null);
    }

    public void saveShop(Shop shop) {
        this.shopProcessor.saveShop(shop);
    }

    public void saveDsbsShopDat(int shopId) {
        saveShopDat(shopId, 0, true);
    }

    public void saveShopDat(long shopId, long businessId, boolean dsbs) {
        this.shopsDatProcessor.saveShopDat(shopDat((int) shopId, businessId, dsbs));
    }

    public void resetShopDat() {
        YtScenarioExecutor.clearTable(this.shopsDatProcessor.getCfg());
    }

    public String getCurrentCluster() {
        return clusterSource.getCurrentClient().getClusterName();
    }

    public String getCurrentIndexer() {
        return Objects.requireNonNull(executors.getSourceIndexerName());
    }

    public void taskMonitoringJob() {
        taskMonitoringJob.doJob(context);
    }

    public void initOnce(Class<?> testClass, Runnable init) {
        if (initializers.add(testClass)) {
            init.run();
        }
    }

    private ShopsDat shopDat(int id, long businessId, boolean dsbs) {
        ShopsDat dat = new ShopsDat();
        dat.setShop_id(id);
        dat.setBusiness_id(businessId);
        dat.set_dsbs(dsbs);
        dat.setUpdated_at(TimingUtils.getInstant());
        return dat;
    }
}
