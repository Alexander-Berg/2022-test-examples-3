package ru.yandex.market.ff.service.registry;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.dto.RegistryUnitCountDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitDTOContainer;
import ru.yandex.market.ff.client.dto.RegistryUnitIdDTO;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.model.bo.RegistryUnitsFilter;
import ru.yandex.market.ff.model.dto.KorobyteDto;
import ru.yandex.market.ff.model.dto.registry.RegistryUnit;
import ru.yandex.market.ff.model.dto.registry.RelatedUnitIds;
import ru.yandex.market.ff.model.dto.registry.UnitCount;
import ru.yandex.market.ff.model.dto.registry.UnitCountsInfo;
import ru.yandex.market.ff.model.dto.registry.UnitInfo;
import ru.yandex.market.ff.model.dto.registry.UnitMeta;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static ru.yandex.market.ff.model.dto.registry.RegistryUnitId.of;

class RegistryUnitServiceTest extends IntegrationTest {

    @Autowired
    private RegistryUnitService registryUnitService;

    /**
     * Тест на создание грузомест.
     */
    @Test
    @DatabaseSetup(value = "classpath:service/registry-unit/empty_registry.xml")
    @ExpectedDatabase(value = "classpath:service/registry-unit/1/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void onValidSaving() {
        var registryId = 1L;

        var palletId = of(RegistryUnitIdType.PALLET_ID, "PL0001");
        RegistryUnit pallet = RegistryUnit.palletBuilder()
                .registryId(registryId)
                .unitInfo(UnitInfo.builder()
                        .unitId(palletId)
                        .unitCountsInfo(UnitCountsInfo.of(UnitCount.of(UnitCountType.FIT, 1)))
                        .build()
                )
                .unitMeta(UnitMeta.builder().description("Some pallet").build())
                .build();

        var boxId = of(RegistryUnitIdType.BOX_ID, "P001", RegistryUnitIdType.ORDER_ID, "12345");
        RegistryUnit box = RegistryUnit.boxBuilder()
                .registryId(registryId)
                .unitInfo(UnitInfo.builder()
                        .unitId(boxId)
                        .parentUnitId(palletId)
                        .unitCountsInfo(UnitCountsInfo.of(UnitCount.of(UnitCountType.FIT, 1)))
                        .build()
                )
                .unitMeta(UnitMeta.builder()
                        .description("some box")
                        .korobyte(KorobyteDto.builder()
                                .height(BigDecimal.TEN)
                                .length(BigDecimal.ONE)
                                .width(BigDecimal.ONE)
                                .build()).build()
                ).build();

        var itemId = of(RegistryUnitIdType.SHOP_SKU, "sku123", RegistryUnitIdType.VENDOR_ID, "47831");
        RegistryUnit item = RegistryUnit.itemBuilder()
                .registryId(registryId)
                .unitMeta(UnitMeta.builder().boxCount(10).hasExpirationDate(true).marketSku(123L).build())
                .unitInfo(UnitInfo.builder()
                        .unitId(itemId)
                        .parentUnitId(boxId)
                        .unitCountsInfo(UnitCountsInfo.builder()
                                .unitCount(
                                        UnitCount.builder()
                                                .type(UnitCountType.FIT)
                                                .count(10)
                                                .relatedUnitId(RelatedUnitIds.asChildFree(boxId, palletId))
                                                .build()
                                )
                                .unitCount(UnitCount.builder()
                                        .type(UnitCountType.DEFECT)
                                        .count(5)
                                        .relatedUnitId(RelatedUnitIds.builder()
                                                .unitId(of(
                                                        RegistryUnitIdType.CIS, "CIS0001",
                                                        RegistryUnitIdType.IMEI, "123")
                                                )
                                                .unitId(of(RegistryUnitIdType.CIS, "CIS0002"))
                                                .unitId(of(RegistryUnitIdType.SERIAL_NUMBER, "123132"))
                                                .unitId(of(RegistryUnitIdType.IMEI, "5555"))
                                                .unitId(of(RegistryUnitIdType.IMEI, "6666"))
                                                .unitId(of(RegistryUnitIdType.VIRTUAL_ID, "0100500"))
                                                .parentUnitId(palletId)
                                                .build()).build()
                                )
                                .build()
                        ).build()
                )
                .build();

        registryUnitService.create(pallet);
        registryUnitService.create(box);
        registryUnitService.create(item);
    }

    /**
     * Тест на поиск грузомест по id реестра.
     */
    @Test
    @DatabaseSetup(value = "classpath:service/registry-unit/2/before.xml")
    void onFindingUnitsByRegistryId() {
        var registryId = 1L;
        List<UnitToEntityContainer> registryUnits = registryUnitService.findAllByRegistryId(registryId);

        assertions.assertThat(registryUnits.size()).isEqualTo(2);

        RegistryUnit pallet = registryUnits.get(0).getDto();
        assertions.assertThat(pallet.getRegistryId()).isEqualTo(registryId);
        var palletId = of(RegistryUnitIdType.PALLET_ID, "PL0001");
        assertions.assertThat(pallet.getUnitInfo().getUnitId()).isEqualTo(palletId);
        assertions.assertThat(pallet.getType()).isEqualTo(RegistryUnitType.PALLET);
        assertions.assertThat(pallet.getUnitInfo().getUnitCountsInfo().getUnitCounts())
                .isEqualTo(List.of(new UnitCount(1, UnitCountType.FIT, null, null, null)));
        assertions.assertThat(pallet.getUnitInfo().getParentUnitIds()).isEmpty();

        RegistryUnit box = registryUnits.get(1).getDto();
        assertions.assertThat(box.getRegistryId()).isEqualTo(registryId);
        assertions.assertThat(box.getUnitInfo().getParentUnitIds()).isEqualTo(List.of(palletId));
        assertions.assertThat(box.getUnitInfo().getUnitId())
                .isEqualTo(of(RegistryUnitIdType.BOX_ID, "P00001"));
        assertions.assertThat(box.getType()).isEqualTo(RegistryUnitType.BOX);
        assertions.assertThat(box.getUnitInfo().getUnitCountsInfo().getUnitCounts())
                .isEqualTo(List.of(new UnitCount(1, UnitCountType.FIT, null, null, null)));
    }

    /**
     * Тест на happy-path апдейт грузоместа.
     */
    @Test
    @DatabaseSetup(value = "classpath:service/registry-unit/3/before.xml")
    @ExpectedDatabase(value = "classpath:service/registry-unit/3/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void onValidUpdating() {
        var registryId = 1L;
        UnitToEntityContainer unitToEntityContainer = registryUnitService.findAllByRegistryId(registryId).get(0);

        var dto = unitToEntityContainer.getDto();
        var entity = unitToEntityContainer.getEntity();

        dto.getUnitMeta().setBoxCount(1);
        dto.getUnitMeta().setMarketSku(123L);
        dto.getUnitMeta().setHasExpirationDate(null);

        dto.getUnitInfo().getUnitCountsInfo().getUnitCounts().add(UnitCount.of(UnitCountType.DEFECT, 5));

        registryUnitService.update(entity, dto);
    }

    /**
     * Проверяем первую страницу.
     */
    @Test
    @DatabaseSetup(value = "classpath:service/registry-unit/5/before.xml")
    void getRegistryUnitsFirstPage() {
        var registryId = 1L;
        RegistryUnitsFilter filter = new RegistryUnitsFilter();
        filter.setRegistryIds(Collections.singleton(registryId));
        long requestId = 1L;
        RegistryUnitDTOContainer page0 = registryUnitService
                .findAllByRequestIdAndFilter(requestId, filter, new PageRequest(0, 2));
        assertions.assertThat(page0.getTotalElements()).as("Total elements").isEqualTo(3);
        assertions.assertThat(page0.getTotalPages()).as("Total pages").isEqualTo(2);
        assertions.assertThat(page0.getPageNumber()).as("Page number").isEqualTo(0);
        assertions.assertThat(page0.getSize()).as("Size").isEqualTo(2);

        RegistryUnitDTO unit1 = page0.getUnits().get(0);
        assertions.assertThat(unit1.getRegistryId()).isEqualTo(registryId);
        assertions.assertThat(unit1.getUnitInfo().getUnitId())
                .isEqualTo(RegistryUnitIdDTO.of(RegistryUnitIdType.PALLET_ID, "PL0001"));
        assertions.assertThat(unit1.getType()).isEqualTo(RegistryUnitType.PALLET);
        assertions.assertThat(unit1.getUnitInfo().getUnitCountsInfo().getUnitCounts())
                .containsExactly(new RegistryUnitCountDTO(UnitCountType.FIT, 1, null, null));
        assertions.assertThat(unit1.getUnitInfo().getParentUnitIds()).isEmpty();

        RegistryUnitDTO unit2 = page0.getUnits().get(1);
        assertions.assertThat(unit2.getRegistryId()).isEqualTo(registryId);
        assertions.assertThat(unit2.getUnitInfo().getUnitId())
                .isEqualTo(RegistryUnitIdDTO.of(RegistryUnitIdType.BOX_ID, "P00001"));
        assertions.assertThat(unit2.getType()).isEqualTo(RegistryUnitType.BOX);
        assertions.assertThat(unit2.getUnitInfo().getUnitCountsInfo().getUnitCounts())
                .containsExactly(new RegistryUnitCountDTO(UnitCountType.FIT, 1, null, null));
        assertions.assertThat(unit2.getUnitInfo().getParentUnitIds()).hasSize(1);
        assertions.assertThat(unit2.getUnitInfo().getParentUnitIds().get(0))
                .isEqualTo(RegistryUnitIdDTO.of(RegistryUnitIdType.PALLET_ID, "PL0001"));
    }

    /**
     * Проверяем вторую страницу.
     */
    @Test
    @DatabaseSetup(value = "classpath:service/registry-unit/5/before.xml")
    void getRegistryUnitsSecondPage() {
        var registryId = 1L;
        RegistryUnitsFilter filter = new RegistryUnitsFilter();
        filter.setRegistryIds(Collections.singleton(registryId));
        long requestId = 1L;
        RegistryUnitDTOContainer page1 = registryUnitService
                .findAllByRequestIdAndFilter(requestId, filter, new PageRequest(1, 2));
        assertions.assertThat(page1.getTotalElements()).as("Total elements").isEqualTo(3);
        assertions.assertThat(page1.getTotalPages()).as("Total pages").isEqualTo(2);
        assertions.assertThat(page1.getPageNumber()).as("Page number").isEqualTo(1);
        assertions.assertThat(page1.getSize()).as("Size").isEqualTo(1);

        RegistryUnitDTO unit3 = page1.getUnits().get(0);
        assertions.assertThat(unit3.getRegistryId()).isEqualTo(registryId);
        assertions.assertThat(unit3.getUnitInfo().getUnitId())
                .isEqualTo(RegistryUnitIdDTO.of(RegistryUnitIdType.BOX_ID, "P00002"));
        assertions.assertThat(unit3.getType()).isEqualTo(RegistryUnitType.BOX);
        assertions.assertThat(unit3.getUnitInfo().getUnitCountsInfo().getUnitCounts())
                .containsExactly(new RegistryUnitCountDTO(UnitCountType.FIT, 2, null, null));
        assertions.assertThat(unit3.getUnitInfo().getParentUnitIds()).hasSize(1);
        assertions.assertThat(unit3.getUnitInfo().getParentUnitIds().get(0))
                .isEqualTo(RegistryUnitIdDTO.of(RegistryUnitIdType.PALLET_ID, "PL0001"));
    }

    /**
     * Проверяем работу фильтрации по requestId.
     */
    @Test
    @DatabaseSetup(value = "classpath:service/registry-unit/5/before.xml")
    void getRegistryUnitsEmptyResultDueRequestIdDoesntMatch() {

        RegistryUnitsFilter filter = new RegistryUnitsFilter();
        filter.setRegistryIds(Collections.singleton(1L));
        RegistryUnitDTOContainer container = registryUnitService
                .findAllByRequestIdAndFilter(2L, filter, new PageRequest(0, 2));
        assertions.assertThat(container.getTotalElements()).as("Total elements").isEqualTo(0);
        assertions.assertThat(container.getTotalPages()).as("Total pages").isEqualTo(0);
        assertions.assertThat(container.getPageNumber()).as("Page number").isEqualTo(0);
        assertions.assertThat(container.getSize()).as("Size").isEqualTo(0);
    }

    /**
     * Проверяем работу фильтрации по registryId.
     */
    @Test
    @DatabaseSetup(value = "classpath:service/registry-unit/5/before.xml")
    void getRegistryUnitsEmptyResultDueRegistryIdsDoesntMatch() {

        RegistryUnitsFilter filter = new RegistryUnitsFilter();
        filter.setRegistryIds(Collections.singleton(2L));
        RegistryUnitDTOContainer container = registryUnitService
                .findAllByRequestIdAndFilter(1L, filter, new PageRequest(0, 2));
        assertions.assertThat(container.getTotalElements()).as("Total elements").isEqualTo(0);
        assertions.assertThat(container.getTotalPages()).as("Total pages").isEqualTo(0);
        assertions.assertThat(container.getPageNumber()).as("Page number").isEqualTo(0);
        assertions.assertThat(container.getSize()).as("Size").isEqualTo(0);
    }

    /**
     * Проверяем работу фильтрации по типу грузомест.
     */
    @Test
    @DatabaseSetup(value = "classpath:service/registry-unit/5/before.xml")
    void getFilteredOnRegistryUnitType() {

        RegistryUnitsFilter filter = new RegistryUnitsFilter();
        filter.setRegistryIds(Collections.singleton(1L));
        filter.setRegistryUnitTypes(Set.of(RegistryUnitType.BOX));
        RegistryUnitDTOContainer container = registryUnitService
                .findAllByRequestIdAndFilter(1L, filter, new PageRequest(0, 2));
        assertions.assertThat(container.getTotalElements()).as("Total elements").isEqualTo(2);
        assertions.assertThat(container.getTotalPages()).as("Total pages").isEqualTo(1);
        assertions.assertThat(container.getPageNumber()).as("Page number").isEqualTo(0);
        assertions.assertThat(container.getSize()).as("Size").isEqualTo(2);
        assertions.assertThat(container.getUnits()).as("Registry units")
                .allMatch(unitDTO -> unitDTO.getType() == RegistryUnitType.BOX, "Type is BOX");
    }

}
