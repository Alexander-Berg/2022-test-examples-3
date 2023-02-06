package ru.yandex.market.delivery.transport_manager.service.xdoc;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitType;

class AxaptaMovementOrderPalletFilterTest extends AbstractContextualTest {
    @Autowired
    private AxaptaMovementOrderPalletFilter filter;

    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
        "/repository/tag/axapta_movement_order.xml",
        "/repository/transportation/xdoc_to_ff_transportations.xml",
        "/repository/transportation/xdoc_to_dc_transportations.xml",
    })
    @Test
    void getAvailableForOutbound() {
        List<DistributionCenterUnit> availableForOutbound = filter.getAvailableForOutbound(
            List.of(
                unit(1L, "abc", "aaa"),
                unit(2L, "abc1", "aaa"),
                unit(3L, "abc2", "aaa")
            )
        );

        softly.assertThat(availableForOutbound).containsExactlyInAnyOrder(
            unit(2L, "abc1", "aaa"),
            unit(3L, "abc2", "aaa")
        );
    }

    @Test
    @DatabaseSetup({
        "/repository/distribution_unit_center/transportations_with_mix_pallets.xml",
    })
    void testHappyPathWithDifferentTypesOfPallets() {
        List<DistributionCenterUnit> availableForOutbound = filter.getAvailableForOutbound(
            List.of(
                unit(1L, "PALLET1", "I1"),
                unit(2L, "PALLET2", "I1"),
                unit(3L, "PALLET3", null),
                unit(8L, "PALLET4", null)
            )
        );

        softly.assertThat(availableForOutbound).containsExactlyInAnyOrder(
            unit(1L, "PALLET1", "I1"),
            unit(3L, "PALLET3", null)
        );
    }

    private static DistributionCenterUnit unit(Long id, String barcode, String externalId) {
        return new DistributionCenterUnit()
            .setId(id)
            .setDcUnitId(barcode)
            .setInboundExternalId(externalId)
            .setType(DistributionCenterUnitType.PALLET);
    }
}
