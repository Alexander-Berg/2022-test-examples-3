package ru.yandex.market.delivery.transport_manager.service.register;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.CenterType;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitType;

class GruzinUnitsToRegisterServiceTest extends AbstractContextualTest {
    @Autowired
    private GruzinUnitsToRegisterService gruzinUnitsToRegisterService;

    @DatabaseSetup("/repository/register/register.xml")
    @ExpectedDatabase(
        value = "/repository/register_unit/register_unit_relations.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void saveRegisterUnits() {
        gruzinUnitsToRegisterService.saveRegisterUnits(
            2L,
            List.of(
                // Паллета с двумя коробками
                new DistributionCenterUnit()
                    .setId(1L)
                    .setType(DistributionCenterUnitType.PALLET)
                    .setLogisticPointFromId(10L)
                    .setLogisticPointToId(20L)
                    .setDcUnitId("PALLET_01")
                    .setCenterType(CenterType.DISTRIBUTION_CENTER),
                new DistributionCenterUnit()
                    .setId(10L)
                    .setType(DistributionCenterUnitType.BOX)
                    .setLogisticPointFromId(10L)
                    .setLogisticPointToId(20L)
                    .setDcUnitId("BOX_01")
                    .setParentId(1L),
                new DistributionCenterUnit()
                    .setId(20L)
                    .setType(DistributionCenterUnitType.BOX)
                    .setLogisticPointFromId(10L)
                    .setLogisticPointToId(20L)
                    .setDcUnitId("BOX_02")
                    .setParentId(1L),
                // Сама по себе коробка
                new DistributionCenterUnit()
                    .setId(30L)
                    .setType(DistributionCenterUnitType.BOX)
                    .setLogisticPointFromId(10L)
                    .setLogisticPointToId(20L)
                    .setDcUnitId("BOX_03")
            )
        );
    }

    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit_relations.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_delete_by_parent_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void removeRegisterUnits() {
        gruzinUnitsToRegisterService.removeRegisterUnits(
            2L,
            List.of(new DistributionCenterUnit().setDcUnitId("PALLET_01"))
        );
    }
}
