package ru.yandex.market.core.util;

import java.util.Optional;

import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;

public class LogisticPartnerServiceUtil {

    private LogisticPartnerServiceUtil() {
    }

    public static Optional<PartnerResponse> getMockedLmsResponse(boolean allSyncs, PartnerStatus partnerStatus) {
        return Optional.of(PartnerResponse.newBuilder()
                .id(12L)
                .autoSwitchStockSyncEnabled(allSyncs)
                .korobyteSyncEnabled(allSyncs)
                .stockSyncEnabled(allSyncs)
                .stockSyncSwitchReason(StockSyncSwitchReason.AUTO_CHANGED_AFTER_FAIL)
                .status(partnerStatus)
                .locationId(1)
                .trackingType("TRACK")
                .build());
    }
}
