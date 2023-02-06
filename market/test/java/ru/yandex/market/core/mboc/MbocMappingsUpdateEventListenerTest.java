package ru.yandex.market.core.mboc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.supplier.summary.SupplierMappingSummaryInfo;

import static org.mockito.Mockito.verifyNoInteractions;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

@DbUnitDataSet(before = "MbocMappingsUpdateEventListenerTest.before.csv")
public class MbocMappingsUpdateEventListenerTest extends FunctionalTest {
    @Autowired
    private MbocMappingsUpdateEventListener tested;

    @Test
    @DbUnitDataSet(before = "MbocMappingsUpdateEventListenerFlagFalse.csv")
    void testBaseMetricsAndNotifyWithNotifyFlagDisable() {
        // новый партнер, товары недоехали
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(1, 0, 0, 0)));
        // новый партнер, товары доехали, нет подтвержденных маппингов
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(2, 1, 0, 0)));
        // новый партнер, товары доехали, есть подтвержденные маппинги
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(3, 1, 1, 0)));
        // партнер, по нему уже есть маппинги, пришел первый подтвержденный маппинг
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(5, 1, 1, 0)));
        // партнер, по нему уже есть маппинги в том числе и подтвержденные
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(6, 2, 1, 0)));

        verifySentNotificationType(partnerNotificationClient, 3, 1601024443L);
    }

    @Test
    @DbUnitDataSet(before = "MbocMappingsUpdateEventListenerFlagTrue.csv")
    void testBaseMetricsWithNotifyFlagEnable() {
        // новый партнер, товары недоехали
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(1, 0, 0, 0)));
        // новый партнер, товары доехали, нет подтвержденных маппингов
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(2, 1, 0, 0)));
        // новый партнер, товары доехали, есть подтвержденные маппинги
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(3, 1, 1, 0)));
        // партнер, по нему уже есть маппинги, пришел первый подтвержденный маппинг
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(5, 1, 1, 0)));
        // партнер, по нему уже есть маппинги в том числе и подтвержденные
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(6, 2, 1, 0)));

        verifyNoInteractions(partnerNotificationClient);
    }

    @Test
    @DbUnitDataSet(before = "MbocMappingsUpdateEventListenerFlagFalse.csv")
    void testNotifyNotSpammingFlagDisable() {
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(1, 1, 1, 0)));
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(1, 2, 2, 0)));
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(1, 3, 3, 0)));
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(1, 4, 4, 0)));

        verifySentNotificationType(partnerNotificationClient, 1, 1601024443L);
    }

    @Test
    @DbUnitDataSet(before = "MbocMappingsUpdateEventListenerFlagTrue.csv")
    void testNotifyNotSpammingFlagEnable() {
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(1, 1, 1, 0)));
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(1, 2, 2, 0)));
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(1, 3, 3, 0)));
        tested.onApplicationEvent(new MbocMappingsUpdateEvent(new SupplierMappingSummaryInfo(1, 4, 4, 0)));

        verifyNoInteractions(partnerNotificationClient);
    }
}
