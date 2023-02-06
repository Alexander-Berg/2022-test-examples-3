package ru.yandex.market.deepmind.common;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.junit.Before;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.ChangedSsku;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.ssku.ChangedSskuRepository;
import ru.yandex.market.mbo.taskqueue.TaskQueueHandler;
import ru.yandex.market.mbo.taskqueue.TaskQueueHandlerRegistry;
import ru.yandex.market.mbo.taskqueue.TaskQueueRegistrator;
import ru.yandex.market.mbo.taskqueue.TaskQueueRepository;
import ru.yandex.market.mbo.taskqueue.TaskQueueTask;
import ru.yandex.market.mbo.taskqueue.TaskRecord;
import ru.yandex.market.mbo.taskqueue.UnsafeTaskQueueExecutor;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

public abstract class DeepmindBaseAvailabilitiesTaskQueueTestClass extends DeepmindBaseDbTestClass {
    @Resource(name = "deepmindDsl")
    protected DSLContext dsl;
    @Resource(name = "deepmindTransactionHelper")
    protected TransactionHelper transactionHelper;
    @Resource
    protected TransactionTemplate transactionTemplate;
    @Resource(name = "availabilitiesTaskQueueObjectMapper")
    protected ObjectMapper objectMapper;
    @Resource(name = "availabilitiesTaskQueueHandlerRegistry")
    protected TaskQueueHandlerRegistry taskQueueHandlerRegistry;
    @Resource(name = "availabilitiesTaskQueueRepository")
    protected TaskQueueRepository taskQueueRepository;
    @Resource(name = "availabilitiesTaskQueueRegistrator")
    protected TaskQueueRegistrator taskQueueRegistrator;
    @Resource
    protected ChangedSskuRepository changedSskuRepository;

    protected UnsafeTaskQueueExecutor taskQueueExecutor;

    @Before
    public void setupExecutor() {
        taskQueueExecutor = new UnsafeTaskQueueExecutor(
            taskQueueHandlerRegistry, transactionTemplate, taskQueueRepository, objectMapper, Duration.ofHours(1));
    }

    protected void execute() {
        while (true) {
            if (!taskQueueExecutor.processNextStep()) {
                break;
            }
        }
        var failed = taskQueueRepository.getFailedTasks();
        Assertions.assertThat(failed).isEmpty();
    }

    protected void clearQueue() {
        taskQueueRepository.deleteAll();
    }

    protected <T extends TaskQueueTask> List<T> getQueueTasksOfType(Class<T> tClass) {
        List<TaskRecord> all = taskQueueRepository.findAll();
        //noinspection unchecked
        return all.stream()
            .map(taskRecord -> {
                String taskType = taskRecord.getTaskType();
                TaskQueueHandler<?> handler = taskQueueHandlerRegistry.getHandler(taskType);
                if (handler == null) {
                    throw new IllegalStateException("Failed to find handler for type " + taskType + ". " +
                        "Did you forget to add it into availabilitiesTaskQueueHandlerRegistry?");
                }
                try {
                    return handler.readTask(
                        taskRecord.getTaskData(),
                        taskRecord.getTaskDataVersion(),
                        objectMapper
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .filter(o -> tClass.isAssignableFrom(o.getClass()))
            .map(o -> (T) o)
            .collect(Collectors.toList());
    }

    protected <T extends TaskQueueTask> List<T> getQueueTasks() {
        //noinspection unchecked
        return (List<T>) getQueueTasksOfType(TaskQueueTask.class);
    }

    protected ChangedSsku changedSsku(ServiceOfferReplica offer) {
        return new ChangedSsku().setSupplierId(offer.getBusinessId()).setShopSku(offer.getShopSku());
    }

    protected ChangedSsku changedSsku(ServiceOfferKey shopSkuKey) {
        return new ChangedSsku().setSupplierId(shopSkuKey.getSupplierId()).setShopSku(shopSkuKey.getShopSku());
    }

    protected ChangedSsku changedSsku(int supplierId, String sku) {
        return new ChangedSsku().setSupplierId(supplierId).setShopSku(sku);
    }
}
