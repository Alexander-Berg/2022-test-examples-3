package ru.yandex.market.delivery.transport_manager.service.register;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.repository.register.RegisterFactory;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class RegisterServiceTest extends AbstractContextualTest {
    @Autowired
    RegisterService registerService;

    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml"
    })
    @ExpectedDatabase(
        value = "/repository/register/after/register_new_with_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createPlan() {
        registerService.createEmptyRegisterWithComment(1L, RegisterType.PLAN, 10L, null);
    }

    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml"
    })
    @ExpectedDatabase(
        value = "/repository/register/after/register_new_with_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void create() {
        registerService.create(List.of(
            new Register()
                .setType(RegisterType.PLAN)
                .setStatus(RegisterStatus.NEW)
                .setPartnerId(10L)
        ));
    }

    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml"
    })
    @ExpectedDatabase(
        value = "/repository/register/xdoc_fact_register.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createRegisterWithRestrictedData() {
        registerService.create(List.of(RegisterFactory.newRegisterFactXDocWithRestrictedData()));
    }

    @DatabaseSetup("/repository/register/single_register_with_deps.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/register_status_after_update.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void updateStatusAndDate() {
        registerService.updateStatusAndDate(1L, RegisterStatus.DO_NOT_NEED_TO_SEND, null);
    }

    @DatabaseSetup("/repository/register/two_registers_to_close.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/registers_close_for_plan.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register/after/registers_close_for_plan_history.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void closePlanFor() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 21, 10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
        registerService.markPlansNotForSending(
            new Transportation()
                .setInboundUnit(new TransportationUnit().setId(3L))
                .setOutboundUnit(new TransportationUnit().setId(2L))
        );
    }

    @DatabaseSetup(value = {
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml",
    }, type = DatabaseOperation.CLEAN_INSERT)
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_copy.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_relation_copy.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void copyUnits() {
        registerService.copyUnits(2L, 1L);
    }

    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml"
    })
    @DatabaseSetup(value = {
        "/repository/register/single_register.xml",
        "/repository/register_unit/register_unit.xml",
    }, type = DatabaseOperation.CLEAN_INSERT)
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_copy.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_relation_copy.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register/created_register.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void createRegisterCopy() {
        registerService.createRegisterCopy(1L, 2L);
    }

    @DatabaseSetup(value = {
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml",
    }, type = DatabaseOperation.CLEAN_INSERT)
    @ExpectedDatabase(
        value = "/repository/register_unit/after/insert_with_units_and_relations.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void insertWithUnitsAndRelations() {
        Register register = new Register()
            .setId(2L)
            .setType(RegisterType.FACT)
            .setStatus(RegisterStatus.NEW)
            .setPallets(
                List.of(new RegisterUnit()
                    .setId(1L)
                    .setType(UnitType.PALLET)
                )
            )
            .setItems(
                List.of(new RegisterUnit()
                    .setId(2L)
                    .setParentIds(Set.of(1L))
                    .setType(UnitType.ITEM)
                    .setBarcode("12345")
                )
            );

        registerService.insertWithUnitsAndRelations(register);
    }

    @Test
    @DatabaseSetup("/repository/register/registers_with_contractor.xml")
    void testGetRealSupplier() {
        softly.assertThat(registerService.getRegisterRealSupplierId(10L)).isEqualTo(Optional.of("c1"));
        softly.assertThat(registerService.getRegisterRealSupplierId(20L)).isEqualTo(Optional.empty());
        softly.assertThat(registerService.getRegisterRealSupplierId(30L)).isEqualTo(Optional.of("c3"));
    }
}
