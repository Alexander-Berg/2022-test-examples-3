package ru.yandex.market.core.feature.precondition.listeners;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.yam.service.PrepayRequestStatusListener;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.mbi.util.Changed;
import ru.yandex.market.mbi.util.Created;

/**
 * тест для {@link MarketplaceFeatureOnChangeRequestStatusListener}
 */
class MarketplaceFeatureOnChangeRequestStatusListenerTest extends FunctionalTest {

    @Autowired
    MarketplaceFeatureOnChangeRequestStatusListener marketplaceFeatureChangeStatusListener;


    @Test
    @DbUnitDataSet(before = "testCompletedStatus.before.csv", after = "testCompletedStatus.after.csv")
    void testCompletedStatus() {
        marketplaceFeatureChangeStatusListener.processStatusChanges(statusChanged(101L, PartnerApplicationStatus.COMPLETED), 1L);
    }

    @Test
    @DbUnitDataSet(before = "testCompletedStatus.before.csv", after = "testDontChangeRevokeStatus.after.csv")
    void testDontChangeRevokeStatus() {
        marketplaceFeatureChangeStatusListener.processStatusChanges(statusChanged(102L, PartnerApplicationStatus.COMPLETED), 1L);
    }

    @Test
    @DbUnitDataSet(before = "testClosedStatus.before.csv", after = "testClosedStatus.after.csv")
    void testClosedStatus() {
        marketplaceFeatureChangeStatusListener.processStatusChanges(statusChanged(101L, PartnerApplicationStatus.CLOSED), 1L);
    }

    @Test
    @DbUnitDataSet(before = "testCompletedStatus.before.csv", after = "testNeedInfoStatus.after.csv")
    void testNeedInfoStatus() {
        marketplaceFeatureChangeStatusListener.processStatusChanges(statusChanged(103L, PartnerApplicationStatus.NEED_INFO), 1L);
    }

    private Changed<PrepayRequestStatusListener.StatusInfo> statusChanged(long datasourceId,
                                                                          PartnerApplicationStatus newStatus) {
        return new Created<>(
                PrepayRequestStatusListener.StatusInfo.builder()
                        .setRequestId(1001L)
                        .setDatasourceIds(Collections.singletonList(datasourceId))
                        .setStatus(newStatus)
                        .setPrepayType(PrepayType.YANDEX_MARKET)
                        .setContactEmail("a@b.ru")
                        .build());
    }
}
