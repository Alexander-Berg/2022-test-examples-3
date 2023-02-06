package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SilverSskuQueueInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SilverSskuYtStorageQueue;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

public class SilverSskuYtStorageQueueCleanerTest extends MdmBaseDbTestClass {
    @Autowired
    private SilverSskuYtStorageQueue silverSskuYtStorageQueue;
    @Autowired
    private StorageKeyValueService storageKeyValueService;

    private SilverSskuYtStorageQueueCleaner silverSskuYtStorageQueueCleaner;

    @Before
    public void setUp() throws Exception {
        silverSskuYtStorageQueueCleaner = new SilverSskuYtStorageQueueCleaner(
            silverSskuYtStorageQueue,
            storageKeyValueService
        );
    }

    @Test
    public void testCleaning() {
        // given
        Stream.of(
                new SilverSskuQueueInfo()
                    .setSupplierId(1)
                    .setShopSku("11")
                    .setSourceType(MasterDataSourceType.MDM_OPERATOR)
                    .setSourceId("111"),
                new SilverSskuQueueInfo()
                    .setSupplierId(2)
                    .setShopSku("22")
                    .setSourceType(MasterDataSourceType.MDM_OPERATOR)
                    .setSourceId("222")
            )
            .peek(it -> it.setProcessed(true))
            .forEach(silverSskuYtStorageQueue::insert);

        silverSskuYtStorageQueue.insert(
            new SilverSskuQueueInfo()
                .setSupplierId(3)
                .setShopSku("33")
                .setSourceType(MasterDataSourceType.MDM_OPERATOR)
                .setSourceId("333")
        );

        Assertions.assertThat(silverSskuYtStorageQueue.countItems()).isEqualTo(3);
        Assertions.assertThat(silverSskuYtStorageQueue.getUnprocessedItemsCount()).isEqualTo(1);

        // when
        silverSskuYtStorageQueueCleaner.execute();

        //then
        Assertions.assertThat(silverSskuYtStorageQueue.countItems()).isEqualTo(1);
        Assertions.assertThat(silverSskuYtStorageQueue.getUnprocessedItemsCount()).isEqualTo(1);
    }
}
