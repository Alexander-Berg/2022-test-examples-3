package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.EnrichReturnRegistryQueueConsumer;
import ru.yandex.market.ff.model.dbqueue.EnrichReturnRegistryPayload;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class EnrichReturnRegistryQueueConsumerTest extends IntegrationTestWithDbQueueConsumers {

    @Autowired
    private EnrichReturnRegistryQueueConsumer consumer;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DatabaseSetup("classpath:service/returns/before-enrich-registry.xml")
    @ExpectedDatabase(value = "classpath:service/returns/after-enrich-registry.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyEnrichRegistry() {
        executeTask(1L);
    }

    @Test
    @DatabaseSetup("classpath:service/returns/before-enrich-registry-items-without-boxes.xml")
    @ExpectedDatabase(value = "classpath:service/returns/after-enrich-registry-items-without-boxes.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyEnrichRegistryWhenItemsWithNullBoxes() {
        executeTask(1L);
    }

    @Test
    @DatabaseSetup("classpath:service/returns/before-enrich-registry-with-modification.xml")
    @ExpectedDatabase(value = "classpath:service/returns/after-enrich-registry-with-modification.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyModifyRegistry() {
        jdbcTemplate.update("update request_subtype set " +
                "invalid_registry_unit_service = 'DefaultReturnInvalidRegistryUnitsServiceImpl' where id = 7");
        executeTask(1L);
        jdbcTemplate.update("update request_subtype set " +
                "invalid_registry_unit_service = null where id = 7");
    }

    @Test
    @DatabaseSetup("classpath:service/returns/before-enrich-registry-with-full-modification.xml")
    @ExpectedDatabase(value = "classpath:service/returns/after-enrich-registry-with-full-modification.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldMarkRequestInvalidInCaseOfFullModification() {
        jdbcTemplate.update("update request_subtype set " +
                "invalid_registry_unit_service = 'DefaultReturnInvalidRegistryUnitsServiceImpl' where id = 7");
        executeTask(1L);
        jdbcTemplate.update("update request_subtype set " +
                "invalid_registry_unit_service = null where id = 7");
    }

    @Test
    @DatabaseSetup("classpath:service/returns/before-enrich-registry-with-not_acceptable.xml")
    @ExpectedDatabase(value = "classpath:service/returns/after-enrich-registry-with-not-acceptable.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyMarkUnitsNotAcceptable() {
        jdbcTemplate.update("update request_subtype set " +
                "invalid_registry_unit_service = 'DefaultReturnInvalidRegistryUnitsServiceImpl' where id = 7");
        executeTask(1L);
        jdbcTemplate.update("update request_subtype set " +
                "invalid_registry_unit_service = null where id = 7");
    }

    private void executeTask(long requestId) {
        var payload = new EnrichReturnRegistryPayload(requestId);
        var task = new Task<>(new QueueShardId("shard"), payload, 0,
                ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        transactionTemplate.execute(status -> consumer.execute(task));
    }

}
