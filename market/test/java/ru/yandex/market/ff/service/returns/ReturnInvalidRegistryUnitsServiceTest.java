package ru.yandex.market.ff.service.returns;

import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitEntity;
import ru.yandex.market.ff.service.registry.RegistryUnitService;
import ru.yandex.market.ff.service.registry.UnitToEntityContainer;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

class ReturnInvalidRegistryUnitsServiceTest extends IntegrationTest {
    @Autowired
    private RegistryUnitService registryUnitService;
    @Autowired
    private DefaultReturnInvalidRegistryUnitsServiceImpl returnInvalidRegistryUnitsService;

    @Test
    @DatabaseSetup("classpath:service/returns/invalid-registry-units/only-box/before.xml")
    @ExpectedDatabase(value = "classpath:service/returns/invalid-registry-units/only-box/after.xml",
            assertionMode = NON_STRICT)
    public void onlyBoxShouldBeFiltered() {
        List<RegistryUnitEntity> registryUnitEntities =
                registryUnitService.findAllByRegistryId(1L).stream().map(UnitToEntityContainer::getEntity)
                        .collect(Collectors.toList());

        returnInvalidRegistryUnitsService.filterAndSaveInvalidUnits(registryUnitEntities, createRequestSubTypeEntity());
    }

    @Test
    @DatabaseSetup("classpath:service/returns/invalid-registry-units/only-order-id-return-id//before.xml")
    @ExpectedDatabase(value = "classpath:service/returns/invalid-registry-units/only-order-id-return-id/after.xml",
            assertionMode = NON_STRICT)
    public void onlyMissingReturnIdAndOrderIdShouldBeFiltered() {
        List<RegistryUnitEntity> registryUnitEntities =
                registryUnitService.findAllByRegistryId(1L).stream().map(UnitToEntityContainer::getEntity)
                        .collect(Collectors.toList());

        returnInvalidRegistryUnitsService.filterAndSaveInvalidUnits(registryUnitEntities, createRequestSubTypeEntity());
    }

    @Test
    @DatabaseSetup("classpath:service/returns/invalid-registry-units/no-filtering/before.xml")
    @ExpectedDatabase(value = "classpath:service/returns/invalid-registry-units/no-filtering/after.xml",
            assertionMode = NON_STRICT)
    public void nothingIsFiltered() {
        List<RegistryUnitEntity> registryUnitEntities =
                registryUnitService.findAllByRegistryId(1L).stream().map(UnitToEntityContainer::getEntity)
                        .collect(Collectors.toList());

        returnInvalidRegistryUnitsService.filterAndSaveInvalidUnits(registryUnitEntities, createRequestSubTypeEntity());
    }

    private RequestSubTypeEntity createRequestSubTypeEntity() {
        RequestSubTypeEntity requestSubTypeEntity = new RequestSubTypeEntity();
        requestSubTypeEntity.setRequiredRegistryUnitIdTypesForBoxOnPreValidation("ORDER_ID,ORDER_RETURN_ID");
        requestSubTypeEntity.setSaveUnknownBoxAsNotAcceptable(false);
        return requestSubTypeEntity;
    }
}
