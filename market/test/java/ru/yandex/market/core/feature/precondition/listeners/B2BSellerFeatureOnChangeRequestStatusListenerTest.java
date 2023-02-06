package ru.yandex.market.core.feature.precondition.listeners;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.yam.service.PrepayRequestStatusListener;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.mbi.util.Changed;
import ru.yandex.market.mbi.util.Created;

@DbUnitDataSet(before = "B2BSellerFeatureOnChangeRequestStatusListenerTest.before.csv")
public class B2BSellerFeatureOnChangeRequestStatusListenerTest extends FunctionalTest {

    @Autowired
    private B2BSellerFeatureOnChangeRequestStatusListener b2BSellerFeatureOnChangeRequestStatusListener;

    @Autowired
    private Clock clock;

    @BeforeEach
    void before() {
        Mockito.when(clock.instant()).thenReturn(Instant.now());
    }

    @Test
    @DbUnitDataSet(after = "testCompletedStatusB2BSeller.after.csv")
    void testCompleted() {
        b2BSellerFeatureOnChangeRequestStatusListener.processStatusChanges(
                statusChanged(101, PartnerApplicationStatus.COMPLETED),
                10
        );
    }

    private Changed<PrepayRequestStatusListener.StatusInfo> statusChanged(
            long datasourceId,
            PartnerApplicationStatus newStatus
    ) {
        return new Created<>(
                PrepayRequestStatusListener.StatusInfo.builder()
                        .setRequestId(101L)
                        .setDatasourceIds(Collections.singletonList(datasourceId))
                        .setStatus(newStatus)
                        .setPrepayType(PrepayType.YANDEX_MARKET)
                        .setContactEmail("a@b.ru")
                        .build());
    }
}
