package ru.yandex.market.tpl.core.domain.region;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.market.tpl.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kukabara
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
class RegionServiceTest {

    private final RegionService regionService;

    @Test
    void testGetRegion() {
        RegionTree regionTree = regionService.getRegionTree();
        assertThat(regionTree.getRegion(213).getName()).isEqualTo("Москва");
    }
}
