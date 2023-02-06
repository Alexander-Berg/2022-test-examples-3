package ru.yandex.market.delivery.transport_manager.service.xdoc;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnit;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;
import ru.yandex.market.delivery.transport_manager.service.xdoc.data.MetaSupplyGroup;
import ru.yandex.market.delivery.transport_manager.service.xdoc.data.PalletWithSupplies;
import ru.yandex.market.delivery.transport_manager.service.xdoc.data.SupplyGroup;
import ru.yandex.market.delivery.transport_manager.util.TimeUtil;

class XDocPlanOutboundSorterTest {
    public static final ZonedDateTime NOW = ZonedDateTime.of(2021, 12, 5, 10, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET);
    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    TmPropertyService propertyService = Mockito.mock(TmPropertyService.class);

    @Test
    void sort() {
        TestableClock clock = new TestableClock();
        clock.setFixed(NOW.toInstant(), TimeUtil.DEFAULT_ZONE_OFFSET);
        XDocPlanOutboundSorter sorter = new XDocPlanOutboundSorter(
            clock,
            propertyService
        );

        MetaSupplyGroup preferredOverdueMinSupTime = new MetaSupplyGroup()
            .add(supplyGroup("1", NOW.minusDays(10).toInstant(), 1));
        MetaSupplyGroup preferredOverdueMaxSupTime = new MetaSupplyGroup()
            .add(supplyGroup("2", NOW.minusDays(7).toInstant(), 5));
        MetaSupplyGroup preferredNotOverdueMaxPalletCount = new MetaSupplyGroup()
            .add(supplyGroup("3", NOW.minusDays(2).toInstant(), 6));
        MetaSupplyGroup preferredNotOverdueMinPalletCount = new MetaSupplyGroup()
            .add(supplyGroup("4", NOW.minusDays(3).toInstant(), 2));
        MetaSupplyGroup notPreferredOverdueMinSupTime = new MetaSupplyGroup()
            .add(supplyGroup("5", NOW.minusDays(10).toInstant(), 1));
        MetaSupplyGroup notPreferredOverdueMaxSupTime = new MetaSupplyGroup()
            .add(supplyGroup("6", NOW.minusDays(7).toInstant(), 5));
        MetaSupplyGroup notPreferredNotOverdueMaxPalletCount = new MetaSupplyGroup()
            .add(supplyGroup("7", NOW.minusDays(2).toInstant(), 6));
        MetaSupplyGroup notPreferredNotOverdueMinPalletCount = new MetaSupplyGroup()
            .add(supplyGroup("8", NOW.minusDays(3).toInstant(), 2));

        List<MetaSupplyGroup> supplyMetaGroups = List.of(
            notPreferredOverdueMinSupTime,
            preferredOverdueMaxSupTime,
            preferredOverdueMinSupTime,
            notPreferredNotOverdueMaxPalletCount,
            preferredNotOverdueMinPalletCount,
            notPreferredNotOverdueMinPalletCount,
            notPreferredOverdueMaxSupTime,
            preferredNotOverdueMaxPalletCount
        );

        softly.assertThat(
            sorter.sort(
                supplyMetaGroups,
                0L,
                Set.of("1", "2", "3", "4")
            )
        ).containsExactly(
            preferredOverdueMinSupTime,
            preferredOverdueMaxSupTime,
            preferredNotOverdueMaxPalletCount,
            preferredNotOverdueMinPalletCount,
            notPreferredOverdueMinSupTime,
            notPreferredOverdueMaxSupTime,
            notPreferredNotOverdueMaxPalletCount,
            notPreferredNotOverdueMinPalletCount
        );
    }

    private SupplyGroup supplyGroup(String supplyId, Instant supplyTime, int palletCount) {
        Set<PalletWithSupplies> pallets = IntStream.range(0, palletCount)
            .mapToObj(i -> new PalletWithSupplies()
                .setSupplyIds(Set.of(supplyId))
                .setPallet(new DistributionCenterUnit().setDcUnitId(String.format("%s-%d", supplyId, i)))
            )
            .collect(Collectors.toSet());
        return new SupplyGroup()
            .setSupplyId(supplyId)
            .setSupplyTime(supplyTime)
            .setPallets(pallets);
    }
}
