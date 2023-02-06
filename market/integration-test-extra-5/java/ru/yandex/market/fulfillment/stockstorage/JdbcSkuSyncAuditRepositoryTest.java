package ru.yandex.market.fulfillment.stockstorage;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.repository.JdbcSkuSyncAuditRepository;

public class JdbcSkuSyncAuditRepositoryTest extends AbstractContextualTest {

    @Autowired
    private JdbcSkuSyncAuditRepository repository;

    /**
     * Кейс, когда передаём большую, чем имеется, дату.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku_sync_audit/upsert/setup.xml")
    @ExpectedDatabase(value = "classpath:database/expected/sku_sync_audit/upsert/greater_synced.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void upsertSkuSyncAuditWithGreaterSynced() {
        repository.upsertSkuSyncAudits(
                Collections.singletonList(getFirstSkuUnitId()), LocalDateTime.parse("2018-03-11T11:00:22")
        );
    }

    /**
     * Кейс, когда передаём меньшую, чем имеется, дату.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku_sync_audit/upsert/setup.xml")
    @ExpectedDatabase(value = "classpath:database/expected/sku_sync_audit/upsert/lower_synced.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void upsertSkuSyncAuditWithLowerSynced() {
        repository.upsertSkuSyncAudits(
                Collections.singletonList(getFirstSkuUnitId()), LocalDateTime.parse("2018-03-11T09:00:22")
        );
    }

    /**
     * Кейс, когда передаём равную той, которая имеется, дату.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku_sync_audit/upsert/setup.xml")
    @ExpectedDatabase(value = "classpath:database/expected/sku_sync_audit/upsert/equal_synced.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void upsertSkuSyncAuditWithEqualSynced() {
        repository.upsertSkuSyncAudits(
                Collections.singletonList(getFirstSkuUnitId()), LocalDateTime.parse("2018-03-11T10:00:00")
        );
    }

    /**
     * Тест-кейс на вставку батчем.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku_sync_audit/upsert/setup.xml")
    @ExpectedDatabase(value = "classpath:database/expected/sku_sync_audit/upsert/batch_update.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void upsertSkuSyncAuditBatch() {
        repository.upsertSkuSyncAudits(
                Arrays.asList(getFirstSkuUnitId(), getSecondSkuUnitId()), LocalDateTime.parse("2018-03-11T11:00:22")
        );
    }

    private UnitId getFirstSkuUnitId() {
        return new UnitId("sku1", 10L, 2);
    }

    private UnitId getSecondSkuUnitId() {
        return new UnitId("disabled_sku", 10L, 2);
    }
}
