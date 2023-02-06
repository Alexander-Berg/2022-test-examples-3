package ru.yandex.market.ff.service.registry;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.client.enums.RegistryFlowType;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.model.dto.registry.RegistryRestrictedData;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitCount;
import ru.yandex.market.ff.model.dto.registry.UnitCountsInfo;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.registry.RegistryEntity;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitEntity;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitInvalidReason;
import ru.yandex.market.ff.service.registry.converter.RegistryUnitToRegistryUnitInvalidConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegistryEditingServiceImplTest extends SoftAssertionSupport {

    private final RegistryUnitToRegistryUnitInvalidConverter registryUnitToRegistryUnitInvalidConverter
        = new RegistryUnitToRegistryUnitInvalidConverter();

    private final RegistryEditingServiceImpl registryEditingService =
        new RegistryEditingServiceImpl(registryUnitToRegistryUnitInvalidConverter);

    @Test
    public void checkModifyToValidRegistryWithInvalidReasons() {
        RegistryEntity registryEntity = createRegistry(
            List.of(createRegistryUnit(createIdentifiers(
                Set.of(createUnitPartialId(RegistryUnitIdType.CIS, "1111"),
                    createUnitPartialId(RegistryUnitIdType.SHOP_SKU, "2222"))), RegistryUnitType.BOX)));

        RegistryModificationResult registryModificationResultActual =
            registryEditingService.modifyToValidRegistry(registryEntity, createRequestSubTypeEntity());

        assertions.assertThat(registryModificationResultActual.getInvalidRegistryUnits().get(0).getReasons()
            .containsAll(List.of(RegistryUnitInvalidReason.MISSING_ORDER_ID,
                RegistryUnitInvalidReason.MISSING_RETURN_ID))).isEqualTo(true);

        assertEquals(registryModificationResultActual.getInvalidRegistryUnits().get(0).getReasons().size(), 2);
        assertEquals(registryModificationResultActual.getValidRegistry().getRegistryUnits().size(), 0);
    }

    @Test
    public void checkModifyToValidRegistryWithoutInvalidReasons() {
        RegistryEntity registryEntity = createRegistry(
            List.of(createRegistryUnit(createIdentifiers(
                Set.of(createUnitPartialId(RegistryUnitIdType.ORDER_ID, "1111"),
                    createUnitPartialId(RegistryUnitIdType.ORDER_RETURN_ID, "2222"))), RegistryUnitType.BOX)));

        RegistryModificationResult registryModificationResultActual =
            registryEditingService.modifyToValidRegistry(registryEntity, createRequestSubTypeEntity());

        assertEquals(registryModificationResultActual.getInvalidRegistryUnits().size(), 0);
        assertEquals(registryModificationResultActual.getValidRegistry().getRegistryUnits().size(), 1);
    }

    @Test
    public void checkModifyToValidRegistryWithItemType() {
        RegistryEntity registryEntity = createRegistry(
            List.of(createRegistryUnit(createIdentifiers(
                Set.of(createUnitPartialId(RegistryUnitIdType.CIS, "1111"),
                    createUnitPartialId(RegistryUnitIdType.SHOP_SKU, "2222"))), RegistryUnitType.ITEM)));

        RegistryModificationResult registryModificationResultActual =
            registryEditingService.modifyToValidRegistry(registryEntity, createRequestSubTypeEntity());

        assertEquals(registryModificationResultActual.getInvalidRegistryUnits().size(), 0);
        assertEquals(registryModificationResultActual.getValidRegistry().getRegistryUnits().size(), 1);
    }

    private RegistryEntity createRegistry(List<RegistryUnitEntity> registryUnitIds) {
        var registry = new RegistryEntity();
        registry.setRegistryUnits(registryUnitIds);
        registry.setId(1L);
        registry.setType(RegistryFlowType.FACT);
        registry.setRequestId(1L);
        registry.setRestrictedData(RegistryRestrictedData.builder().build());
        registry.setUnitCounts(List.of());
        return registry;
    }

    private RegistryUnitId createIdentifiers(Set<UnitPartialId> unitPartialIdSet) {
        return RegistryUnitId.builder()
            .parts(unitPartialIdSet).build();
    }

    private UnitPartialId createUnitPartialId(RegistryUnitIdType type, String value) {
        return UnitPartialId.builder().type(type).value(value).build();
    }

    private RegistryUnitEntity createRegistryUnit(RegistryUnitId identifiers, RegistryUnitType type) {
        var registryUnit = new RegistryUnitEntity();
        registryUnit.setRegistryId(1L);
        registryUnit.setId(1L);
        registryUnit.setChildUnits(List.of(registryUnit));
        registryUnit.setIdentifiers(identifiers);
        registryUnit.setType(type);
        registryUnit.setUnitCountsInfo(UnitCountsInfo.of(UnitCount.of(UnitCountType.FIT, 1)));
        registryUnit.setParentUnits(List.of(registryUnit));
        return registryUnit;
    }

    private RequestSubTypeEntity createRequestSubTypeEntity() {
        RequestSubTypeEntity requestSubTypeEntity = new RequestSubTypeEntity();
        requestSubTypeEntity.setRequiredRegistryUnitIdTypesForBoxOnPreValidation("ORDER_ID,ORDER_RETURN_ID");
        return requestSubTypeEntity;
    }
}
