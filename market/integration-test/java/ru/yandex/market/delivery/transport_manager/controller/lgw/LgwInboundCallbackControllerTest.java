package ru.yandex.market.delivery.transport_manager.controller.lgw;

import java.util.Objects;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationUnitMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterUnitMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DatabaseSetup("/repository/facade/interactor/transportation_with_deps.xml")
class LgwInboundCallbackControllerTest extends AbstractContextualTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransportationUnitMapper unitMapper;

    @Autowired
    private RegisterMapper registerMapper;

    @Autowired
    private RegisterUnitMapper registerUnitMapper;


    @Test
    @SneakyThrows
    void putInboundSuccess() {
        mockMvc.perform(
            put("/lgw/inbound/TMU3/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/lgw/unit/unit_success.json"))
        ).andExpect(MockMvcResultMatchers.status().isOk());

        TransportationUnit unit = Objects.requireNonNull(unitMapper.getById(3L));
        Assertions.assertEquals(TransportationUnitStatus.ACCEPTED, unit.getStatus());
        Assertions.assertEquals("EXT_UNIT_ID_1", unit.getExternalId());
    }

    @Test
    @SneakyThrows
    void putInboundError() {
        mockMvc.perform(
            put("/lgw/inbound/TMU3/error")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/lgw/unit/unit_error.json"))
        ).andExpect(MockMvcResultMatchers.status().isOk());

        TransportationUnit unit = Objects.requireNonNull(unitMapper.getById(3L));
        Assertions.assertEquals(TransportationUnitStatus.ERROR, unit.getStatus());
    }

    @Test
    @SneakyThrows
    @DatabaseSetup({
        "/repository/facade/interactor/register.xml",
        "/repository/facade/interactor/register_for_inbound.xml"
    })
    void putRegistrySuccess() {
        mockMvc.perform(
            put("/lgw/inbound/registry/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/lgw/unit/registry_success.json"))
        ).andExpect(MockMvcResultMatchers.status().isOk());

        var register = Objects.requireNonNull(registerMapper.getById(1L));
        Assertions.assertEquals(register.getExternalId(), "NEW_EXT_ID_1");
        Assertions.assertEquals(register.getStatus(), RegisterStatus.ACCEPTED);
        Assertions.assertEquals(
            Objects.requireNonNull(unitMapper.getById(3L)).getStatus(),
            TransportationUnitStatus.PLAN_REGISTRY_ACCEPTED
        );
    }

    @Test
    @SneakyThrows
    @DatabaseSetup({
        "/repository/facade/interactor/register.xml",
        "/repository/facade/interactor/register_for_inbound.xml"
    })
    @DatabaseSetup(value = "/repository/facade/interactor/update_status.xml", type = DatabaseOperation.UPDATE)
    void putRegistryError() {
        mockMvc.perform(
            put("/lgw/inbound/registry/error")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/lgw/unit/registry_error.json"))
        ).andExpect(MockMvcResultMatchers.status().isOk());
        Assertions.assertEquals(Objects.requireNonNull(registerMapper.getById(1L)).getStatus(), RegisterStatus.ERROR);
        Assertions.assertEquals(
            Objects.requireNonNull(unitMapper.getById(3L)).getStatus(), TransportationUnitStatus.ERROR
        );
    }

    @Test
    @SneakyThrows
    @DatabaseSetup("/repository/register/setup/empty.xml")
    @ExpectedDatabase(
        value = "/repository/register_unit/after/min_get_inbound.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup(
        value = "/repository/facade/interactor/update/transportation_1_sent.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Успешный колбек от LGW на getInbound. Проверка минимального набора входных данных")
    void getInboundMinSuccess() {
        mockMvc.perform(
            put("/lgw/inbound/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/lgw/unit/get/min_inbound_success.json"))
        ).andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @SneakyThrows
    @DatabaseSetup("/repository/register/setup/empty.xml")
    @DatabaseSetup(
        value = "/repository/facade/interactor/update/transportation_1_sent.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/repository/register_unit/after/get_inbound_with_restricted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешный колбек от LGW на getInbound. Проверка заполнения restrictedData")
    void getInboundWithRestrictedSuccess() {
        mockMvc.perform(
            put("/lgw/inbound/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/lgw/unit/get/inbound_with_restricted.json"))
        ).andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @SneakyThrows
    void getInboundError() {
        mockMvc.perform(
            put("/lgw/inbound/get/error")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/lgw/unit/get/get_inbound_error.json"))
        ).andExpect(MockMvcResultMatchers.status().isOk());

        Assertions.assertEquals(
            Objects.requireNonNull(unitMapper.getById(3L)).getStatus(),
            TransportationUnitStatus.ERROR
        );
    }

    @Test
    @SneakyThrows
    @DatabaseSetup("/repository/facade/interactor/transportation_unit_for_cancellation.xml")
    void cancelUnitSuccess() {
        mockMvc.perform(
            put("/lgw/inbound/cancel/TMU30/success")
        ).andExpect(MockMvcResultMatchers.status().isOk());

        TransportationUnit unit = Objects.requireNonNull(unitMapper.getById(30L));
        Assertions.assertEquals(TransportationUnitStatus.CANCELLED, unit.getStatus());
    }

    @Test
    @SneakyThrows
    @DatabaseSetup("/repository/facade/interactor/transportation_unit_for_cancellation.xml")
    void cancelUnitError() {
        mockMvc.perform(
            put("/lgw/inbound/cancel/TMU30/error")
        ).andExpect(MockMvcResultMatchers.status().isOk());

        TransportationUnit unit = Objects.requireNonNull(unitMapper.getById(30L));
        Assertions.assertEquals(TransportationUnitStatus.ERROR, unit.getStatus());
    }

    @Test
    @SneakyThrows
    @DatabaseSetup("/repository/register/setup/empty.xml")
    @ExpectedDatabase(
        value = "/repository/register_unit/after/min_get_inbound.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup(
        value = "/repository/facade/interactor/update/transportation_1_sent.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Успешный колбек от LGW на getInbound. Проверка на заполненность ВСЕХ полей")
    void getInboundMaxSuccess() {
        mockMvc.perform(
            put("/lgw/inbound/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/lgw/unit/get/max_inbound_success.json"))
        ).andExpect(MockMvcResultMatchers.status().isOk());

        softly.assertThat(registerMapper.getById(1L))
            .hasNoNullFieldsOrPropertiesExcept("ffwfId", "shipmentManagerName");
        softly.assertThat(registerUnitMapper.getAllRelationsForRegister(1L)).isNotEmpty();
        registerUnitMapper.getByIds(Set.of(1L, 2L, 3L)).forEach(unit -> {
            softly.assertThat(unit).hasNoNullFieldsOrPropertiesExcept("unitMeta", "denyReason", "lgwTask");
            if (unit.getType() == UnitType.ITEM) {
                softly.assertThat(unit.getUnitMeta()).hasNoNullFieldsOrProperties();
            }
        });
    }
}
