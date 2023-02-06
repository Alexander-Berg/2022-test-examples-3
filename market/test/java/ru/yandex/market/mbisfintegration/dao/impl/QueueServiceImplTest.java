package ru.yandex.market.mbisfintegration.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import jdk.jfr.Description;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbisfintegration.MbiSfAbstractJdbcRecipeTest;
import ru.yandex.market.mbisfintegration.dao.QueueService;
import ru.yandex.market.mbisfintegration.entity.QueueElement;
import ru.yandex.market.mbisfintegration.entity.QueueElementStatus;
import ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType;

import static org.assertj.core.api.Assertions.assertThat;

class QueueServiceImplTest extends MbiSfAbstractJdbcRecipeTest {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    TransactionTemplate transactionTemplate;

    QueueService queueService;

    @BeforeEach
    void setup() {
        queueService = new QueueServiceImpl(jdbcTemplate, transactionTemplate, "testHost");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("TRUNCATE TABLE queue", Map.of());
    }

    @Test
    void test_basicOperations() {
        jdbcTemplate.update(
                "insert into entities (entity_id, entity_type) values (:id, :entity_type)",
                new MapSqlParameterSource()
                        .addValue("id", 1L)
                        .addValue("entity_type", ImportEntityType.SHOP.name())
        );

        queueService.add(1L, ImportEntityType.SHOP);
        QueueElement element = queueService.findPending(1L, ImportEntityType.SHOP);

        Assertions.assertEquals(QueueElementStatus.PENDING, element.getStatus());
        Assertions.assertEquals(1L, element.getEntityId());
        Assertions.assertEquals(ImportEntityType.SHOP, element.getImportEntityType());
        Assertions.assertNotNull(element.getId());
        Assertions.assertNotNull(element.getCreatedAt());

        queueService.updateElement(element.getId());
        QueueElement updatedElement = queueService.findPending(1L, ImportEntityType.SHOP);
        Assertions.assertNotNull(updatedElement.getUpdatedAt());
    }

    @Test
    void test_findPendingEmptyQueue() {
        Assertions.assertNull(queueService.findPending(1L, ImportEntityType.SHOP));
        Assertions.assertTrue(queueService.findPending(ImportEntityType.SHOP).isEmpty());
    }

    @Test
    void test_findAllPending() {
        addEntities(10, 0);
        for (int i = 0; i < 10; i++) {
            queueService.add((long) i, ImportEntityType.SHOP);
        }
        Assertions.assertEquals(10, queueService.findPending(ImportEntityType.SHOP).size());
    }

    @Test
    void test_updateElements() {
        addEntities(10, 0);
        for (int i = 0; i < 10; i++) {
            queueService.add((long) i, ImportEntityType.SHOP);
        }
        List<Long> entityIds = LongStream.iterate(0, i -> i + 1).limit(10).boxed().collect(Collectors.toList());
        queueService.updateElementsStatusFromPendingToSent(entityIds, ImportEntityType.SHOP, QueueElementStatus.SENT);

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from queue where status = :status and entity_type = :entity_type",
                new MapSqlParameterSource()
                        .addValue("status", QueueElementStatus.SENT.name())
                        .addValue("entity_type", ImportEntityType.SHOP.name()),
                Integer.class
        );
        Assertions.assertEquals(10, count);
    }


    @Description("Должен остаться только последний pending элемент в очереди, sent просто удалится тк более старый")
    @Test
    void test_requeueOldSent_sent_should_be_just_deleted() {
        addEntities(1, 1);
        Long sent = jdbcTemplate.queryForObject("insert into queue (entity_id, entity_type, status, created_at, updated_at) " +
                        "values (:entity_id, :entity_type, :status, :created_at, :updated_at) returning id",
                new MapSqlParameterSource()
                        .addValue("entity_id", 1)
                        .addValue("entity_type", ImportEntityType.SHOP.name())
                        .addValue("status", QueueElementStatus.SENT.name())
                        .addValue("created_at", Timestamp.from(Instant.now().minus(5, ChronoUnit.HOURS)))
                        .addValue("updated_at", Timestamp.from(Instant.now().minus(4, ChronoUnit.HOURS))),
                Long.class
        );
        Long pending = jdbcTemplate.queryForObject("insert into queue (entity_id, entity_type, status, created_at, updated_at) " +
                        "values (:entity_id, :entity_type, :status, :created_at, :updated_at) returning id",
                new MapSqlParameterSource()
                        .addValue("entity_id", 1)
                        .addValue("entity_type", ImportEntityType.SHOP.name())
                        .addValue("status", QueueElementStatus.PENDING.name())
                        .addValue("created_at", Timestamp.from(Instant.now().minus(5, ChronoUnit.HOURS)))
                        .addValue("updated_at", Timestamp.from(Instant.now().minus(4, ChronoUnit.HOURS))),
                Long.class
        );
        assertThat(getQueueSize(ImportEntityType.SHOP)).isEqualTo(2);
        queueService.requeueOldSent();
        assertThat(getQueueSize(ImportEntityType.SHOP)).isEqualTo(1);
        assertThat(queueService.findPending(1L, ImportEntityType.SHOP))
                .isNotNull()
                .extracting(QueueElement::getId)
                .isEqualTo(pending);
    }

    @Description("sent меняет статус на pending и остается единственным в очереди, тк другой pending более старый")
    @Test
    void test_requeueOldSent_sent_replaces_pending() {
        addEntities(1, 1);
        Long pending = jdbcTemplate.queryForObject("insert into queue (entity_id, entity_type, status, created_at, updated_at) " +
                        "values (:entity_id, :entity_type, :status, :created_at, :updated_at) returning id",
                new MapSqlParameterSource()
                        .addValue("entity_id", 1)
                        .addValue("entity_type", ImportEntityType.SHOP.name())
                        .addValue("status", QueueElementStatus.PENDING.name())
                        .addValue("created_at", Timestamp.from(Instant.now().minus(5, ChronoUnit.HOURS)))
                        .addValue("updated_at", Timestamp.from(Instant.now().minus(4, ChronoUnit.HOURS))),
                Long.class
        );
        Long sent = jdbcTemplate.queryForObject("insert into queue (entity_id, entity_type, status, created_at, updated_at) " +
                        "values (:entity_id, :entity_type, :status, :created_at, :updated_at) returning id",
                new MapSqlParameterSource()
                        .addValue("entity_id", 1)
                        .addValue("entity_type", ImportEntityType.SHOP.name())
                        .addValue("status", QueueElementStatus.SENT.name())
                        .addValue("created_at", Timestamp.from(Instant.now().minus(5, ChronoUnit.HOURS)))
                        .addValue("updated_at", Timestamp.from(Instant.now().minus(4, ChronoUnit.HOURS))),
                Long.class
        );
        assertThat(getQueueSize(ImportEntityType.SHOP)).isEqualTo(2);
        queueService.requeueOldSent();
        assertThat(getQueueSize(ImportEntityType.SHOP)).isEqualTo(1);
        assertThat(queueService.findPending(1L, ImportEntityType.SHOP))
                .isNotNull()
                .extracting(QueueElement::getId)
                .isEqualTo(sent);
    }

    @Description("из нескольких sent остается только последний")
    @Test
    void test_requeueOldSent_old_sent_are_deleted() {
        addEntities(1, 1);
        Long lastQueueElementId = 0L;
        for (int i = 0; i < 10; i++) {
            lastQueueElementId = jdbcTemplate.queryForObject("insert into queue (entity_id, entity_type, status, created_at, updated_at) " +
                            "values (:entity_id, :entity_type, :status, :created_at, :updated_at) returning id",
                    new MapSqlParameterSource()
                            .addValue("entity_id", 1)
                            .addValue("entity_type", ImportEntityType.SHOP.name())
                            .addValue("status", QueueElementStatus.SENT.name())
                            .addValue("created_at", Timestamp.from(Instant.now().minus(5, ChronoUnit.HOURS)))
                            .addValue("updated_at", Timestamp.from(Instant.now().minus(4, ChronoUnit.HOURS))),
                    Long.class
            );
        }
        assertThat(getQueueSize(ImportEntityType.SHOP)).isEqualTo(10);
        queueService.requeueOldSent();
        assertThat(getQueueSize(ImportEntityType.SHOP)).isEqualTo(1);
        assertThat(queueService.findPending(1L, ImportEntityType.SHOP))
                .isNotNull()
                .extracting(QueueElement::getId)
                .isEqualTo(lastQueueElementId);
    }

    @Test
    void test_markSentAsFailedAndRequeue() {
        addEntities(1, 1);
        jdbcTemplate.update("insert into queue (entity_id, entity_type, status, created_at, updated_at) " +
                        "values (:entity_id, :entity_type, :status, :created_at, :updated_at)",
                new MapSqlParameterSource()
                        .addValue("entity_id", 1)
                        .addValue("entity_type", ImportEntityType.SHOP.name())
                        .addValue("status", QueueElementStatus.SENT.name())
                        .addValue("created_at", Timestamp.from(Instant.now().minus(5, ChronoUnit.HOURS)))
                        .addValue("updated_at", Timestamp.from(Instant.now().minus(4, ChronoUnit.HOURS)))
        );
        queueService.markSentAsFailedAndRequeue(1L, ImportEntityType.SHOP);

        Collection<QueueElement> collection = jdbcTemplate.query("select * from queue where entity_id = :entity_id " +
                        "and entity_type = :entity_type",
                new MapSqlParameterSource()
                        .addValue("entity_id", 1L)
                        .addValue("entity_type", ImportEntityType.SHOP.name()),
                (rs, rowNumber) -> getQueueElement(rs)
        );
        Assertions.assertEquals(2, collection.size());
        Assertions.assertTrue(collection.stream().anyMatch(queueElement ->
                queueElement.getStatus() == QueueElementStatus.PENDING));
        Assertions.assertTrue(collection.stream().anyMatch(queueElement ->
                queueElement.getStatus() == QueueElementStatus.FAILURE));
    }

    @Test
    void test_deleteOld() {
        addEntities(1, 1);
        jdbcTemplate.update("insert into queue (entity_id, entity_type, status, created_at, updated_at) " +
                        "values (:entity_id, :entity_type, :status, :created_at, :updated_at)",
                new MapSqlParameterSource()
                        .addValue("entity_id", 1)
                        .addValue("entity_type", ImportEntityType.SHOP.name())
                        .addValue("status", QueueElementStatus.SENT.name())
                        .addValue("created_at", Timestamp.from(Instant.now().minus(16, ChronoUnit.DAYS)))
                        .addValue("updated_at", Timestamp.from(Instant.now().minus(15, ChronoUnit.DAYS)))
        );
        queueService.deleteOld();
        Assertions.assertNull(queueService.findLast(1L, ImportEntityType.SHOP));
    }

    @Test
    void test_resendAllByImportEntityType() {
        addEntities(5, 1);
        queueService.resendAllByImportEntityType(Set.of(1L, 2L, 3L, 4L, 5L), ImportEntityType.SHOP);
        Collection<QueueElement> elements = queueService.findPending(ImportEntityType.SHOP);
        Assertions.assertNotNull(elements);
        Assertions.assertEquals(5, elements.size());
    }

    private void addEntities(int number, int start) {
        for (int i = start; i < start + number; i++) {
            jdbcTemplate.update(
                    "insert into entities (entity_id, entity_type) values (:id, :entity_type)",
                    new MapSqlParameterSource()
                            .addValue("id", i)
                            .addValue("entity_type", ImportEntityType.SHOP.name())
            );
        }
    }

    private QueueElement getQueueElement(ResultSet rs) throws SQLException {
        return new QueueElement(
                rs.getLong("id"),
                rs.getLong("entity_id"),
                ImportEntityType.valueOf(rs.getString("entity_type")),
                QueueElementStatus.valueOf(rs.getString("status")),
                rs.getString("host_fqdn"),
                rs.getTimestamp("created_at").toInstant(),
                Optional.ofNullable(rs.getTimestamp("updated_at"))
                        .map(Timestamp::toInstant)
                        .orElse(null)
        );
    }

    private Integer getQueueSize(ImportEntityType importEntityType) {
        return jdbcTemplate.queryForObject(
                "select count(*) from queue where entity_type = :entity_type",
                new MapSqlParameterSource().addValue("entity_type", importEntityType.name()),
                Integer.class
        );
    }
}