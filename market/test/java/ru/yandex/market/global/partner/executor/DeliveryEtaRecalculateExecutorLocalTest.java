package ru.yandex.market.global.partner.executor;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.global.db.jooq.tables.pojos.DeliveryRegion;
import ru.yandex.market.global.partner.BaseLocalTest;
import ru.yandex.market.global.partner.config.PartnerTmsTasksConfig;
import ru.yandex.market.global.partner.domain.delivery.region.DeliveryRegionCommandService;
import ru.yandex.market.global.partner.domain.delivery.region.DeliveryRegionQueryService;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@Import(PartnerTmsTasksConfig.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DeliveryEtaRecalculateExecutorLocalTest extends BaseLocalTest {

    private final DeliveryEtaRecalculateExecutor executor;
    private final DeliveryRegionQueryService deliveryRegionQueryService;
    private final DeliveryRegionCommandService deliveryRegionCommandService;

    @Test
    void testEstimate() {
        deliveryRegionCommandService.updateEtaMinutes(1, 0);

        DeliveryRegion before = deliveryRegionQueryService.get(1);
        executor.doRealJob(null);
        DeliveryRegion after = deliveryRegionQueryService.get(1);

        assertThat(before.getEtaMinutes()).isZero();
        assertThat(after.getEtaMinutes()).isNotZero();
    }

}
