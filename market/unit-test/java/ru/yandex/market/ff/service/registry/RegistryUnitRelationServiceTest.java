package ru.yandex.market.ff.service.registry;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ru.yandex.market.ff.client.enums.RegistryFlowType;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.RelatedUnitIds;
import ru.yandex.market.ff.model.dto.registry.UnitCount;
import ru.yandex.market.ff.model.dto.registry.UnitCountsInfo;
import ru.yandex.market.ff.model.dto.registry.UnitMeta;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.registry.RegistryEntity;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.ff.model.dto.registry.RegistryUnitId.of;

@ExtendWith(MockitoExtension.class)
class RegistryUnitRelationServiceTest {
    private RegistryUnitRelationService relationService;

    @BeforeEach
    void Init() {
        relationService = new RegistryUnitRelationService();
    }

    @Test
    public void checkLinkBoxAndPallets() {
        var registryEntity = getValidEntity();

        relationService.linkBoxWithParentPallet(registryEntity.getRegistryUnits());

        var boxActual = registryEntity.getRegistryUnits()
                .stream()
                .filter(unit -> unit.getType() == RegistryUnitType.BOX)
                .findFirst()
                .get();

        var expectedRegistry = getValidEntityWithLinkedParentPallet();
        var boxExpected =  expectedRegistry.getRegistryUnits()
                .stream()
                .filter(unit -> unit.getType() == RegistryUnitType.BOX)
                .findFirst()
                .get();

        assertEquals(boxExpected.getParentIds(), boxActual.getParentIds());
    }

    @Test
    public void linkBoxAndPalletsShouldNotAffectExistingRelations() {
        var registryEntity = getValidEntityWithExistingParent();
        relationService.linkBoxWithParentPallet(Objects.requireNonNull(registryEntity.getRegistryUnits()));

        var boxActual = registryEntity.getRegistryUnits()
                .stream()
                .filter(unit -> unit.getType() == RegistryUnitType.BOX)
                .findFirst()
                .get();

        var boxNotExpected = getBoxWithSecondParentPallet();

        assertEquals(boxNotExpected.getParentIds(), boxActual.getParentIds());
    }

    private RegistryEntity getValidEntity() {
        RegistryEntity registryEntity = new RegistryEntity();
        registryEntity.setId(12L);
        registryEntity.setType(RegistryFlowType.PLANNED_RETURNS);

        RegistryUnitEntity registryUnitEntityPallet = getFirstPallet();
        RegistryUnitEntity registryUnitEntityBox = getBox();
        registryEntity.setRegistryUnits(List.of(
                registryUnitEntityBox,
                registryUnitEntityPallet
        ));
        return registryEntity;
    }

    private RegistryEntity getValidEntityWithLinkedParentPallet() {
        RegistryEntity registryEntity = new RegistryEntity();
        registryEntity.setId(12L);
        registryEntity.setType(RegistryFlowType.PLANNED_RETURNS);

        RegistryUnitEntity registryUnitEntityPallet = getFirstPallet();
        RegistryUnitEntity registryUnitEntityBox = getBoxWithFirstParentPallet();
        registryEntity.setRegistryUnits(List.of(
                registryUnitEntityBox,
                registryUnitEntityPallet
        ));
        return registryEntity;
    }


    private RegistryEntity getValidEntityWithExistingParent() {
        RegistryEntity registryEntity = new RegistryEntity();
        registryEntity.setId(12L);
        registryEntity.setType(RegistryFlowType.PLANNED_RETURNS);

        registryEntity.setRegistryUnits(List.of(
                getBoxWithSecondParentPallet(),
                getSecondPallet(),
                getFirstPallet()
        ));
        return registryEntity;
    }


    private RegistryUnitEntity getBox() {
        RegistryUnitEntity registryUnitEntityBox = RegistryUnitEntity.builder()
                .type(RegistryUnitType.BOX)
                .meta(UnitMeta.builder().description("Some box").build())
                .parentIds(Collections.emptyList())
                .id(110L)
                .registryId(1L)
                .identifiers(new RegistryUnitId(Set.of(
                        new UnitPartialId(RegistryUnitIdType.BOX_ID, "VOZRAT"),
                        new UnitPartialId(RegistryUnitIdType.ORDER_ID, "ORDER_RETURN_1"),
                        new UnitPartialId(RegistryUnitIdType.PALLET_ID, "PALLET"))))
                .unitCountsInfo(UnitCountsInfo.of(
                        UnitCount.of(UnitCountType.FIT, 1),
                        null
                ))
                .build();
        return registryUnitEntityBox;
    }

    private RegistryUnitEntity getBoxWithFirstParentPallet() {
        RegistryUnitEntity registryUnitEntityBox = RegistryUnitEntity.builder()
                .type(RegistryUnitType.BOX)
                .meta(UnitMeta.builder().description("Some box").build())
                .id(110L)
                .parentIds(Collections.emptyList())
                .registryId(1L)
                .parentIds(List.of(new RegistryUnitId(Set.of(
                        new UnitPartialId(RegistryUnitIdType.PALLET_ID, "PALLET"))
                )))
                .identifiers(new RegistryUnitId(Set.of(
                        new UnitPartialId(RegistryUnitIdType.BOX_ID, "VOZRAT"),
                        new UnitPartialId(RegistryUnitIdType.ORDER_ID, "ORDER_RETURN_1"))))
                .unitCountsInfo(UnitCountsInfo.of(
                        UnitCount.of(UnitCountType.FIT, 1),
                        null
                ))
                .build();
        return registryUnitEntityBox;
    }

    private RegistryUnitEntity getBoxWithSecondParentPallet() {
        RegistryUnitEntity registryUnitEntityBox = RegistryUnitEntity.builder()
                .type(RegistryUnitType.BOX)
                .meta(UnitMeta.builder().description("Some box").build())
                .id(110L)
                .parentIds(Collections.emptyList())
                .registryId(1L)
                .parentIds(List.of(new RegistryUnitId(Set.of(
                        new UnitPartialId(RegistryUnitIdType.PALLET_ID, "PALLET"),
                        new UnitPartialId(RegistryUnitIdType.ORDER_ID, "ORDER_RETURN_2"))
                )))
                .identifiers(new RegistryUnitId(Set.of(
                        new UnitPartialId(RegistryUnitIdType.BOX_ID, "VOZRAT"),
                        new UnitPartialId(RegistryUnitIdType.ORDER_ID, "ORDER_RETURN_1"))))
                .unitCountsInfo(UnitCountsInfo.of(
                        UnitCount.of(UnitCountType.FIT, 1),
                        null
                ))
                .build();
        return registryUnitEntityBox;
    }

    private RegistryUnitEntity getFirstPallet() {
        RegistryUnitEntity registryUnitEntityPallet = RegistryUnitEntity.builder()
                .type(RegistryUnitType.PALLET)
                .id(10L)
                .parentIds(Collections.emptyList())
                .registryId(1L)
                .meta(UnitMeta.builder().description("Some pallet").build())
                .identifiers(new RegistryUnitId(Set.of(
                        new UnitPartialId(RegistryUnitIdType.PALLET_ID, "PALLET"),
                        new UnitPartialId(RegistryUnitIdType.ORDER_ID, "ORDER_RETURN_1"))))
                .unitCountsInfo(UnitCountsInfo.of(
                        UnitCount.of(UnitCountType.FIT, 1),
                        RelatedUnitIds.asOrphan(of(RegistryUnitIdType.BOX_ID, "VOZRAT"))
                ))
                .build();
        return registryUnitEntityPallet;
    }

    private RegistryUnitEntity getSecondPallet() {
        RegistryUnitEntity registryUnitEntityPallet = RegistryUnitEntity.builder()
                .type(RegistryUnitType.PALLET)
                .id(11L)
                .parentIds(Collections.emptyList())
                .registryId(1L)
                .meta(UnitMeta.builder().description("Some pallet").build())
                .identifiers(new RegistryUnitId(Set.of(
                        new UnitPartialId(RegistryUnitIdType.PALLET_ID, "PALLET"),
                        new UnitPartialId(RegistryUnitIdType.ORDER_ID, "ORDER_RETURN_2"))))
                .unitCountsInfo(UnitCountsInfo.of(
                        UnitCount.of(UnitCountType.FIT, 1),
                        RelatedUnitIds.asOrphan(of(RegistryUnitIdType.BOX_ID, "VOZRAT"))
                ))
                .build();
        return registryUnitEntityPallet;
    }
}
