package ru.yandex.market.mboc.tms.executors.notifications.creators;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.storage.StorageKeyValueRepository;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mbo.storage.StorageKeyValueServiceImpl;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.notifications.repository.NotificationRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.tms.executors.LockingExecutor;

public abstract class TimestampExecutorTest extends BaseDbTestClass {

    @Autowired
    protected OfferRepository offerRepository;
    @Autowired
    protected NotificationRepository notificationRepository;
    @Autowired
    protected StorageKeyValueRepository storageKeyValueRepository;
    @Autowired
    protected SupplierRepository supplierRepository;

    protected StorageKeyValueService storageService;
    protected LockingExecutor executor;
    protected String storageKey;
    protected LocalDateTime beforeRun;

    protected void setUp() {
        storageService = new StorageKeyValueServiceImpl(storageKeyValueRepository, null);
        supplierRepository.insertBatch(
            YamlTestUtil.readSuppliersFromResource("notifications/tms-suppliers.yml"));
    }

    @Test
    public void testExecutorSettings() {
        // запуск, который ничего не нашел, не меняет timestamp
        setUpBeforeRun();
        executor.execute();

        Assertions.assertThat(storageService.getValue(storageKey, LocalDateTime.class))
            .isEqualTo(beforeRun);
    }

    protected void setUpBeforeRun() {
        beforeRun = storageService.getValue(storageKey, LocalDateTime.class);
    }

    protected void assertTimestampChanged() {
        Assertions.assertThat(storageService.getValue(storageKey, LocalDateTime.class))
            .isAfter(beforeRun);
    }
}
