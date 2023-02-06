package ru.yandex.market.logistics.nesu.controller.partner;

import java.util.Optional;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse.PartnerResponseBuilder;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

abstract class AbstractPartnerControllerSettingsTest extends AbstractContextualTest {
    @Autowired
    protected LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    protected void mockGetPartner(long partnerId) {
        mockGetPartner(partnerId, UnaryOperator.identity());
    }

    protected void mockGetPartner(long partnerId, UnaryOperator<PartnerResponseBuilder> responseModifier) {
        when(lmsClient.getPartner(partnerId))
            .thenReturn(Optional.of(responseModifier.apply(
                PartnerResponse.newBuilder()
                    .id(partnerId)
                    .marketId(partnerId)
                    .locationId(213)
                    .trackingType("Tracking type")
                    .partnerType(PartnerType.DROPSHIP)
                    .status(PartnerStatus.ACTIVE)
                    .stockSyncEnabled(true)
                    .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_LMS_UI)
                    .korobyteSyncEnabled(true)
                    .autoSwitchStockSyncEnabled(true)
            ).build()));
    }

    protected void verifyGetPartner(long partnerId) {
        verify(lmsClient).getPartner(partnerId);
    }
}
