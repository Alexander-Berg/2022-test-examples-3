package ru.yandex.market.delivery.transport_manager.service.distribution_center;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.DistributionCenterUnitDto;
import ru.yandex.market.delivery.transport_manager.domain.filter.DistributionCenterUnitSearchFilter;
import ru.yandex.market.delivery.transport_manager.service.distribution_center.qa.DistributionCenterSearchService;

public class DistributionCenterSearchServiceTest extends AbstractContextualTest {

    @Autowired
    private DistributionCenterSearchService distributionCenterSearchService;

    @Test
    @DatabaseSetup("/repository/distribution_unit_center/distribution_center_units.xml")
    void testSearching() {
        DistributionCenterUnitSearchFilter filter = new DistributionCenterUnitSearchFilter();
        filter.setDcUnitIds(List.of("10"));
        filter.setInboundExternalId(null);

        List<DistributionCenterUnitDto> distributionCenterUnitDtos =
                distributionCenterSearchService.searchDistributionUnits(filter);

        softly.assertThat(distributionCenterUnitDtos.size()).isEqualTo(1);
        softly.assertThat(distributionCenterUnitDtos.get(0).getDcUnitId()).isEqualTo("10");
        softly.assertThat(distributionCenterUnitDtos.get(0).getInboundExternalId()).isEqualTo("1");
    }
}
