package ru.yandex.market.deliverycalculator.storage.util;

import java.time.Instant;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import ru.yandex.market.deliverycalculator.model.DeliveryTariffProgramType;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffSource;
import ru.yandex.market.deliverycalculator.model.Region;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffProgram;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffType;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffUpdatedInfo;
import ru.yandex.market.deliverycalculator.storage.model.RegionEntity;
import ru.yandex.market.deliverycalculator.storage.model.yadelivery.LocationCargoTypeData;
import ru.yandex.market.deliverycalculator.storage.model.yadelivery.YaDeliveryProgramEntity;
import ru.yandex.market.deliverycalculator.storage.model.yadelivery.YaDeliveryTariffInfoEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Тест для {@link EntityMapper}.
 */
class EntityMapperTest {

    /**
     * Тест для {@link EntityMapper#mapRegion(RegionEntity)}.
     * Случай: успешный маппинг
     */
    @Test
    void testMapToModel_successfulMapping() {
        RegionEntity entity = new RegionEntity();
        entity.setId(1);
        entity.setName("Россиюшка");
        entity.setType(1);
        entity.setParent(new RegionEntity());
        entity.getParent().setId(0);

        Region region = EntityMapper.mapRegion(entity);

        assertNotNull(region);
        assertThat(region).isEqualToIgnoringGivenFields(entity, "parentRegionId");
        assertEquals(entity.getParent().getId(), region.getParentRegionId());
    }

    /**
     * Тест для {@link EntityMapper#mapRegion(RegionEntity)}.
     * Случай: успешный маппинг
     */
    @Test
    void testMapToModel_mappingNull() {
        assertNull(EntityMapper.mapRegion(null));
    }

    @Test
    void testMapTarrif_successfulMapping() {
        YaDeliveryTariffInfoEntity entity = new YaDeliveryTariffInfoEntity();
        entity.setId(2L);
        entity.setContentUrl("http://mds");
        entity.setUpdateTime(Instant.now());
        entity.setForShop(false);
        entity.setForCustomer(false);
        entity.setCargoTypeHash(10101);
        entity.setSourceType(DeliveryTariffSource.YADO);
        entity.setType(YaDeliveryTariffType.COURIER);
        entity.setTariffCargoTypesBlacklist(Sets.newSet(20, 30, 40));
        entity.setLocationCargoTypesBlacklist(Sets.newSet(
                new LocationCargoTypeData(1, 40),
                new LocationCargoTypeData(2, 50)
        ));
        entity.setRulesCurrency("RUR");
        entity.setCurrency("$");
        entity.setCarrierId(35);
        entity.setM3Weight(50.1);
        entity.setRulesCount(44);
        entity.setOriginalRulesCount(65);
        entity.addProgram(new YaDeliveryProgramEntity(entity, DeliveryTariffProgramType.DAAS));
        entity.addProgram(new YaDeliveryProgramEntity(entity, DeliveryTariffProgramType.MARKET_DELIVERY));

        YaDeliveryTariffUpdatedInfo tariffUpdatedInfo = EntityMapper.mapTariff(entity);
        assertThat(tariffUpdatedInfo).isEqualToIgnoringGivenFields(entity,
                "locationCargoTypesBlacklist", "programs");
        assertEquals(tariffUpdatedInfo.getLocationCargoTypesBlacklist().stream()
                        .map(cargoType -> new LocationCargoTypeData(cargoType.getLocationId(),
                                cargoType.getCargoType())).collect(Collectors.toSet()),
                entity.getLocationCargoTypesBlacklist());
        assertEquals(tariffUpdatedInfo.getPrograms().stream()
                        .map(YaDeliveryTariffProgram::getProgram).collect(Collectors.toSet()),
                entity.getPrograms().stream().map(YaDeliveryProgramEntity::getProgram).collect(Collectors.toSet()));
    }
}
