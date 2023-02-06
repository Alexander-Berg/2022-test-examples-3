package ru.yandex.market.delivery.transport_manager.repository.register;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.IdPair;
import ru.yandex.market.delivery.transport_manager.domain.entity.StatusHolder;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterMeta;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterRelation;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.queue.health.AggregatedTypeRegisterState;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class RegisterMapperTest extends AbstractContextualTest {
    @Autowired
    private RegisterMapper registerMapper;

    private static final Register MINIMAL_REGISTER = RegisterFactory.newMinimalRegister();
    private static final Register FULL_REGISTER = RegisterFactory.newFullRegister();
    private static final Register FULL_REGISTER_WITH_UNITS = RegisterFactory.newFullRegisterWithUnits();
    private static final Register XDOC_FACT_WITH_RESTRICTED = RegisterFactory.newRegisterFactXDocWithRestrictedData();

    @Test
    @ExpectedDatabase(value = "/repository/register/register.xml", assertionMode = NON_STRICT_UNORDERED)
    void persist() {
        registerMapper.persistAll(List.of(MINIMAL_REGISTER, FULL_REGISTER));
    }

    @Test
    @ExpectedDatabase(value = "/repository/register/register.xml", assertionMode = NON_STRICT_UNORDERED)
    void insert() {
        registerMapper.insert(MINIMAL_REGISTER);
        registerMapper.insert(FULL_REGISTER);
    }

    @Test
    @ExpectedDatabase(value = "/repository/register/xdoc_fact_register.xml", assertionMode = NON_STRICT_UNORDERED)
    void persistRegisterWithRestrictedData() {
        registerMapper.persistAll(List.of(XDOC_FACT_WITH_RESTRICTED));
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/register_dependencies.xml",
        "/repository/register/related_transportation.xml"
    })
    void getRegistersForTransportation() {
        List<Register> allRegistersForTransportation = registerMapper.getByTransportationId(1L);
        assertContainsExactlyInAnyOrder(allRegistersForTransportation, MINIMAL_REGISTER, FULL_REGISTER);
    }

    @Test
    @DatabaseSetup("/repository/register/xdoc_fact_transportation.xml")
    void getRegistersWithRestrictedDataForTransportations() {
        List<Register> allRegistersForTransportations = registerMapper.getByTransportationIds(Set.of(1L));
        assertContainsExactlyInAnyOrder(allRegistersForTransportations, XDOC_FACT_WITH_RESTRICTED);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/registers_by_transportations.xml",
        "/repository/register/related_transportations.xml"
    })
    void getRegistersForTransportations() {
        List<Register> byTransportationIds = registerMapper.getByTransportationIds(Set.of(1L, 2L));
        assertContainsExactlyInAnyOrder(
            byTransportationIds,
            MINIMAL_REGISTER,
            RegisterFactory.newRegister(2L, RegisterStatus.NEW),
            RegisterFactory.newRegister(3L, RegisterStatus.PREPARING),
            RegisterFactory.newRegister(4L, RegisterStatus.PREPARING),
            RegisterFactory.newRegister(5L, RegisterStatus.PREPARING)
        );
    }

    @Test
    @DatabaseSetup({
        "/repository/register/registers_by_transportations.xml",
        "/repository/register/related_transportations.xml"
    })
    void getRegisterForTransportatonsEmpty() {
        List<Register> empty = registerMapper.getByTransportationIds(Set.of());
        softly.assertThat(empty).hasSize(0);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/register_dependencies.xml",
        "/repository/register/related_transportation.xml"
    })
    void getRegistersForMovement() {
        List<Register> allRegistersForTransportation = registerMapper.getByMovementId(4L);
        assertContainsExactlyInAnyOrder(allRegistersForTransportation, FULL_REGISTER);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/register_dependencies.xml",
        "/repository/register/related_transportation.xml"
    })
    void getRegistersForTransportationUnit() {
        List<Register> allRegistersForTransportation = registerMapper.getByTransportationUnitId(2L);
        assertContainsExactlyInAnyOrder(allRegistersForTransportation, MINIMAL_REGISTER);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/register_dependencies.xml",
        "/repository/register/related_transportation.xml"
    })
    void getPlanRegisterUnitCountTypes() {
        Set<CountType> countTypes = registerMapper.getPlanRegisterUnitItemCountTypes(3L);
        softly.assertThat(countTypes).containsAll(Set.of(CountType.FIT, CountType.DEFECT));
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/related_transportation.xml",
        "/repository/register/register_unit_without_count_type.xml"
    })
    void getPlanRegisterUnitCountTypesWhenMissingCountType() {
        Set<CountType> countTypes = registerMapper.getPlanRegisterUnitItemCountTypes(3L);
        softly.assertThat(countTypes).containsAll(Set.of(CountType.FIT, CountType.DEFECT));
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/register_dependencies.xml",
        "/repository/register/related_transportation.xml"
    })
    void getRegistersWithUnitsForTransportation() {
        List<Register> allRegistersForTransportation = registerMapper.getByTransportationId(1L);
        List<Long> registerIds = allRegistersForTransportation.stream()
            .map(Register::getId)
            .collect(Collectors.toList());
        List<Register> registerWithUnits = registerMapper.getRegisterWithUnits(registerIds);
        assertContainsExactlyInAnyOrder(registerWithUnits, MINIMAL_REGISTER, FULL_REGISTER_WITH_UNITS);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/register_dependencies.xml",
        "/repository/register/related_transportation.xml"
    })
    void getRegisterMeta() {
        Map<Long, RegisterMeta> registerMeta = registerMapper.getRegisterMetaGrouped(List.of(1L, 2L));
        softly.assertThat(registerMeta.get(1L).getRelation()).isEqualTo(RegisterRelation.OUTBOUND);
        softly.assertThat(registerMeta.get(1L).getTransportationId()).isEqualTo(1L);
        softly.assertThat(registerMeta.get(2L).getRelation()).isEqualTo(RegisterRelation.MOVEMENT);
        softly.assertThat(registerMeta.get(2L).getTransportationId()).isEqualTo(1L);
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation/transportations_with_full_metadata.xml",
        "/repository/transportation/register_meta.xml"
    })
    void deleteTransportationUnitsRegisterRelations() {
        Set<Long> registerIds = registerMapper.deleteTransportationUnitRegisterRelations(Set.of(2L, 4L));
        softly.assertThat(registerIds).containsExactly(1L, 2L);
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation/transportations_with_full_metadata.xml",
        "/repository/transportation/register_meta.xml"
    })
    void deleteMovementRegisterRelations() {
        Set<Long> registerIds = registerMapper.deleteMovementRegisterRelations(Set.of(1L));
        softly.assertThat(registerIds).containsExactly(3L);
    }

    @DatabaseSetup("/repository/register/register.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/register_update_status_and_date.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void updateStatusAndDate() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 21, 10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
        softly
            .assertThat(registerMapper.updateStatusAndDate(2L, RegisterStatus.DO_NOT_NEED_TO_SEND, clock.instant()))
            .containsExactly(
                new StatusHolder<>(2L, "PLAN", RegisterStatus.PREPARING, RegisterStatus.DO_NOT_NEED_TO_SEND)
            );
    }

    @DatabaseSetup("/repository/register/register.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/register_update_status_and_date_null.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void updateStatusAndDateNull() {
        softly
            .assertThat(registerMapper.updateStatusAndDate(2L, RegisterStatus.DO_NOT_NEED_TO_SEND, null))
            .containsExactly(
                new StatusHolder<>(2L, "PLAN", RegisterStatus.PREPARING, RegisterStatus.DO_NOT_NEED_TO_SEND)
            );
    }

    @DatabaseSetup("/repository/register/single_register_with_deps.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/register_after_update.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void updateRegisterStatusAndDateForTransportationUnit() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 21, 10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
        softly
            .assertThat(registerMapper.updateRegisterStatusAndDateForTransportationUnit(
                2L,
                RegisterStatus.DO_NOT_NEED_TO_SEND,
                RegisterType.PLAN,
                clock.instant()
            ))
            .containsExactly(
                new StatusHolder<>(1L, "PLAN", RegisterStatus.PREPARING, RegisterStatus.DO_NOT_NEED_TO_SEND)
            );
    }

    @DatabaseSetup("/repository/register/register.xml")
    @ExpectedDatabase(
        value = "/repository/register/register.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void updateStatusAndDateForUnitSameStatus() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 21, 10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
        softly
            .assertThat(registerMapper.updateRegisterStatusAndDateForTransportationUnit(
                2L,
                RegisterStatus.PREPARING,
                RegisterType.PLAN,
                clock.instant())
            )
            .isEmpty();
    }

    @Test
    @DatabaseSetup("/repository/register/register.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/register_with_ffwf_id.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void setFfwfId() {
        registerMapper.setFfwfId(2L, 123L);
    }

    @Test
    @DatabaseSetup("/repository/register/register_without_external_id.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/register_with_external_id.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void setExternalIdByFfwfId() {
        registerMapper.setExternalIdByFfwfId(3L, "123");
    }

    @Test
    @DatabaseSetup("/repository/register/register_without_external_id.xml")
    void getStatus() {
        softly.assertThat(registerMapper.getStatus(1L)).isEqualTo(RegisterStatus.DRAFT);
        softly.assertThat(registerMapper.getStatus(2L)).isEqualTo(RegisterStatus.PREPARING);
    }

    @Test
    @DatabaseSetup("/repository/register/register.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/register_update_status_and_date_null.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void switchStatusReturningCount() {
        softly
            .assertThat(registerMapper.switchStatusReturningCount(
                2L,
                RegisterStatus.PREPARING,
                RegisterStatus.DO_NOT_NEED_TO_SEND
            ))
            .isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/repository/register/register.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/register_update_status_and_date_null.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void switchStatusReturningCountWithoutPreviousStatusCheck() {
        softly
            .assertThat(registerMapper.switchStatusReturningCount(
                2L,
                null,
                RegisterStatus.DO_NOT_NEED_TO_SEND
            ))
            .isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/repository/register/register.xml")
    @ExpectedDatabase(
        value = "/repository/register/register.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void switchStatusReturningCountWrong() {
        softly
            .assertThat(registerMapper.switchStatusReturningCount(
                2L,
                RegisterStatus.NEW,
                RegisterStatus.DO_NOT_NEED_TO_SEND
            ))
            .isEqualTo(0);
    }

    @Test
    @DatabaseSetup("/repository/register/register.xml")
    @ExpectedDatabase(
        value = "/repository/register/register.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void switchStatusReturningCountMissingId() {
        softly
            .assertThat(registerMapper.switchStatusReturningCount(
                3L,
                RegisterStatus.PREPARING,
                RegisterStatus.DO_NOT_NEED_TO_SEND
            ))
            .isEqualTo(0);
    }

    @Test
    @DatabaseSetup("/repository/register/by_type_and_status.xml")
    void typedRegisterStatuses() {
        clock.setFixed(
            LocalDateTime.of(2021, 7, 5, 14, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
        LocalDateTime now = LocalDateTime.now(clock);
        List<AggregatedTypeRegisterState> typedRegisterStatuses =
            registerMapper.getTypedRegisterStatuses(now.minusDays(7), now);

        softly.assertThat(typedRegisterStatuses).containsExactlyInAnyOrder(
            new AggregatedTypeRegisterState().setAmount(1).setStatus(RegisterStatus.DRAFT).setType(RegisterType.PLAN),
            new AggregatedTypeRegisterState().setAmount(1).setStatus(RegisterStatus.ERROR)
                .setType(RegisterType.FACT_DELIVERED_ORDERS_RETURN),
            new AggregatedTypeRegisterState().setAmount(1).setStatus(RegisterStatus.NEW).setType(RegisterType.FACT)
        );
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/register_dependencies.xml",
        "/repository/register/related_transportation.xml"
    })
    void getByTransportationUnitIdsAndMissingType() {
        softly
            .assertThat(
                registerMapper.getRegisterIdsByTransportationUnitIdsAndType(
                    Set.of(2L, 3L),
                    RegisterType.DENIED
                )
            )
            .isEmpty();
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/register_dependencies.xml",
        "/repository/register/related_transportation.xml"
    })
    void getByTransportationUnitIdsAndTypePlan() {
        softly
            .assertThat(
                registerMapper.getRegisterIdsByTransportationUnitIdsAndType(
                    Set.of(2L, 3L),
                    RegisterType.PLAN
                )
            )
            .containsExactly(
                new IdPair(3L, 2L)
            );
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/register_dependencies.xml",
        "/repository/register/related_transportation.xml"
    })
    void getByTransportationUnitIdsMissingType2() {
        softly
            .assertThat(
                registerMapper.getRegisterIdsByTransportationUnitIdsAndType(
                    Set.of(2L, 4L),
                    RegisterType.PLAN
                )
            )
            .isEmpty();
    }

    @Test
    @DatabaseSetup("/repository/register/single_register_with_deps.xml")
    void containsTest() {
        softly.assertThat(registerMapper.contains(List.of(1L), "B108324521")).isTrue();
        softly.assertThat(registerMapper.contains(List.of(1L), "B108324522")).isFalse();
        softly.assertThat(registerMapper.contains(List.of(), "B108324521")).isFalse();
    }

    @Test
    void checkSetDocumentsReady() {
        registerMapper.insert(FULL_REGISTER);
        registerMapper.setDocumentsReady(1L, true);
        Register register = registerMapper.getById(1L);
        Assertions.assertTrue(register.getDocumentsReady());
    }

    @DatabaseSetup("/repository/register/register.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/register_with_comment.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void setComment() {
        registerMapper.setComment(1L, "Новый комментарий");
    }
}
