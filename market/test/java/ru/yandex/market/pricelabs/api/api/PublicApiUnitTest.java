package ru.yandex.market.pricelabs.api.api;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.pricelabs.generated.server.pub.model.ShopLogResponse;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.services.database.model.Task;
import ru.yandex.market.pricelabs.services.database.model.TaskStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.generated.server.pub.model.ShopLogResponse.StatusEnum.FAILURE;
import static ru.yandex.market.pricelabs.generated.server.pub.model.ShopLogResponse.StatusEnum.valueOf;

class PublicApiUnitTest {

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void toStatusEnum(TaskStatus status) {
        assertEquals(valueOf(status.name()), PublicApi.toStatusEnum(status));
    }

    @Test
    void convertTask() {
        var task = new Task();
        task.setShop_id(1);
        task.setFeed_id(2);
        task.setType(JobType.SHOP_LOOP_FULL);
        task.setCreated(Instant.ofEpochMilli(1001));
        task.setStarted(Instant.ofEpochMilli(1002));
        task.setUpdated(Instant.ofEpochMilli(1003));
        task.setStatus(TaskStatus.FAILURE);
        task.setRestart_count(3);
        task.setMax_restart_count(4);
        task.setTotal_rows(5);
        task.setTotal_time_millis(6);
        task.setJob_id(7);
        task.setTask_id(8);
        task.setUid(9L);
        task.setInfo("info");
        task.setArgs("args");
        task.setResult("result");

        var response = new ShopLogResponse()
                .shopId(1)
                .feedId(2)
                .type(1)
                .name(JobType.SHOP_LOOP_FULL.getTitle())
                .created(1001L)
                .started(1002L)
                .updated(1003L)
                .finished(1003L)
                .status(FAILURE)
                .retry(3)
                .maxRetries(4)
                .totalRows(5L)
                .totalTimeMillis(6L)
                .jobId(7L)
                .taskId(8L)
                .uid(9L)
                .info("info")
                .args("args")
                .result("result");

        assertEquals(response, PublicApi.convertTask(task));
    }
}
