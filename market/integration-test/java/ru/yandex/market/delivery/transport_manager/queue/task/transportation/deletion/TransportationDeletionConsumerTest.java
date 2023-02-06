package ru.yandex.market.delivery.transport_manager.queue.task.transportation.deletion;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

public class TransportationDeletionConsumerTest extends AbstractContextualTest {

    @Autowired
    private TransportationDeletionConsumer transportationDeletionConsumer;

    @Test
    @DatabaseSetup(
        value = {
            "/repository/transportation/transportations_with_full_metadata.xml",
            "/repository/transportation/register_meta.xml"
        }
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_transportations_cleanup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteOutdatedTransportations() {
        final Instant deleteHistoryBefore = LocalDateTime.of(2021, 1, 31, 0, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant();

        clock.setFixed(deleteHistoryBefore, ZoneId.systemDefault());

        transportationDeletionConsumer.executeTask(
            Task.<TransportationDeletionDto>builder(new QueueShardId("1"))
                .withPayload(
                    new TransportationDeletionDto()
                        .setTransportationIds(List.of(1L, 2L))
                        .setMonthToExpire(1)
                ).build()
        );
    }
}
