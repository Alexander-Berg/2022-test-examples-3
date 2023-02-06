package ru.yandex.market.ff.service.registry;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.model.dto.registry.RegistryUnit;
import ru.yandex.market.ff.model.dto.registry.UnitCount;

import static ru.yandex.market.ff.service.implementation.utils.RegistryUnitPredicates.ofType;

class RegistryChainStateServiceImplTest extends IntegrationTest {

    @Autowired
    private RegistryChainStateService registryChainStateService;

    @Test
    @DatabaseSetup(value = "classpath:service/registry-unit/7/before.xml")
    void getRegistryAddInitialSupply() {
        List<RegistryUnit> registryUnits = registryChainStateService.getFinalStateOfRegistryChain(1L);
        assertions.assertThat(registryUnits.size()).isEqualTo(1);
        assertUnitCountByType(registryUnits, 1, UnitCountType.FIT);
    }

    @Test
    @DatabaseSetup(value = "classpath:service/registry-unit/8/before.xml")
    void getRegistryAddSecondarySupply() {
        List<RegistryUnit> registryUnits = registryChainStateService.getFinalStateOfRegistryChain(1L);
        assertions.assertThat(registryUnits.size()).isEqualTo(4);
        assertUnitCountByType(registryUnits, 2, UnitCountType.FIT);
        assertUnitCountByType(registryUnits, 15, UnitCountType.NON_COMPLIENT);

        List<RegistryUnit> anomaliesBeforeSupply = registryChainStateService.getStateOfRegistryChainBefore(2L,
                cUnit -> cUnit.getUnitInfo()
                        .getUnitCountsInfo()
                        .getUnitCounts()
                        .stream()
                        .anyMatch(ofType(UnitCountType.NON_COMPLIENT)));

        assertions.assertThat(anomaliesBeforeSupply.size()).isEqualTo(1);
        assertUnitCountByType(anomaliesBeforeSupply, 10, UnitCountType.NON_COMPLIENT);
    }

    @Test
    @DatabaseSetup(value = "classpath:service/registry-unit/6/before.xml")
    void getRegistryInitialAndSecondarySupply() {
        List<RegistryUnit> registryUnits = registryChainStateService.getFinalStateOfRegistryChain(1L);
        assertions.assertThat(registryUnits.size()).isEqualTo(4);
        assertUnitCountByType(registryUnits, 2, UnitCountType.FIT);
        assertUnitCountByType(registryUnits, 15, UnitCountType.NON_COMPLIENT);
    }

    @Test
    @DatabaseSetup(value = "classpath:service/registry-unit/9/before.xml")
    void getRegistryAnyType() {
        List<RegistryUnit> registryUnits = registryChainStateService.getFinalStateOfRegistryChain(1L);
        assertions.assertThat(registryUnits.size()).isEqualTo(0);
    }

    private void assertUnitCountByType(List<RegistryUnit> registryUnits, int count,
                                       UnitCountType type) {
        int unitCount = registryUnits.stream()
                .flatMap(cUnit -> cUnit.getUnitInfo().getUnitCountsInfo().getUnitCounts().stream())
                .filter(cUnit -> cUnit.getType() == type)
                .mapToInt(UnitCount::getCount).sum();
        assertions.assertThat(unitCount).isEqualTo(count);
    }
}
