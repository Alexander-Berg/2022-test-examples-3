package ru.yandex.market.delivery.transport_manager.repository.register;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.IdAndCount;
import ru.yandex.market.delivery.transport_manager.domain.entity.dto.RegisterOrdersCountDto;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnitRelation;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.TagCode;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterUnitMapper;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class RegisterUnitMapperTest extends AbstractContextualTest {
    @Autowired
    private RegisterUnitMapper registerUnitMapper;

    // todo: make not static  final or don't modify in tests!!!
    private static final RegisterUnit MINIMAL_REGISTER_UNIT = RegisterFactory.newMinimalRegisterUnit();
    private static final RegisterUnit REGISTER_UNIT = RegisterFactory.newRegisterUnit();
    private static final List<RegisterOrdersCountDto> ITEMS_COUNT_DTOS = RegisterFactory.newItemsCountDtos();

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/register_dependencies.xml",
        "/repository/register/related_transportation.xml"
    })
    void getById() {
        RegisterUnit registerUnit1 = registerUnitMapper.getById(MINIMAL_REGISTER_UNIT.getId());
        RegisterUnit registerUnit2 = registerUnitMapper.getById(REGISTER_UNIT.getId());
        assertThatModelEquals(MINIMAL_REGISTER_UNIT, registerUnit1);
        assertThatModelEquals(REGISTER_UNIT, registerUnit2);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/register_dependencies.xml",
        "/repository/register/related_transportation.xml"
    })
    void getByIds() {
        softly
            .assertThat(registerUnitMapper.getByIds(List.of(
                MINIMAL_REGISTER_UNIT.getId(),
                REGISTER_UNIT.getId()
            )))
            .containsExactlyInAnyOrder(
                MINIMAL_REGISTER_UNIT,
                REGISTER_UNIT
            );
    }

    @Test
    @DatabaseSetup("/repository/register/register.xml")
    @ExpectedDatabase(
        value = "/repository/register_unit/register_unit_null_task.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void persist() {
        registerUnitMapper.persistAll(List.of(MINIMAL_REGISTER_UNIT, REGISTER_UNIT));
    }

    @Test
    @DatabaseSetup("/repository/register/register.xml")
    @ExpectedDatabase(value = "/repository/register_unit/after/empty.xml", assertionMode = NON_STRICT_UNORDERED)
    void persistAllZeroSize() {
        registerUnitMapper.persistAll(Collections.emptyList());
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml"
    })
    @DatabaseSetup(
        value = "/repository/register_unit/register_unit_multiple_parents.xml",
        type = DatabaseOperation.INSERT
    )
    void getParentIds() {
        RegisterUnit registerUnit2 = registerUnitMapper.getById(REGISTER_UNIT.getId());
        assertThatModelEquals(REGISTER_UNIT.setParentIds(Set.of(1L, 3L, 4L)), registerUnit2);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_copy_one.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void copyOne() {
        final Long newId = registerUnitMapper.copyOne(2L, 1L);
        softly.assertThat(newId).isEqualTo(3L);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml",
    })
    void getIdsByRegisterId() {
        softly.assertThat(registerUnitMapper.getIdsByRegisterId(2L)).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml",
    })
    void getIdsByRegisterIdAndType() {
        softly.assertThat(registerUnitMapper.getIdsByRegisterIdAndType(2L, UnitType.ITEM)).containsExactly(2L);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit_count_type.xml",
    })
    void getIdsByRegisterIdAndTypeExceptCountTypes() {
        softly.assertThat(registerUnitMapper.getIdsByRegisterIdAndTypeExceptCountTypes(2L, UnitType.ITEM,
            Set.of(CountType.DEFECT, CountType.EXPIRED)
        )).containsExactly(2L, 3L, 4L);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit_count_type.xml",
    })
    void getIdsByRegisterIdAndTypeForAllCountTypes() {
        softly.assertThat(registerUnitMapper.getIdsByRegisterIdAndTypeExceptCountTypes(2L, UnitType.ITEM,
            Set.of()
        )).containsExactly(2L, 3L, 4L, 5L);
    }

    @Test
    @DatabaseSetup("/repository/register/denied_register.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/after_deny.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void moveToDeniedRegister() {
        registerUnitMapper.moveToDeniedRegister(2L, 1L, "КГТ не поддерживается складом 2");
    }

    @Test
    @DatabaseSetup("/repository/register/denied_register.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/after_set_deny_reason.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void setDenyReason() {
        registerUnitMapper.setDenyReason(1L, "КГТ не поддерживается складом 2");
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml"
    })
    void getAllRelationsForRegister() {
        softly
            .assertThat(registerUnitMapper.getAllRelationsForRegister(2L))
            .containsExactly(new RegisterUnitRelation().setId(2L).setParentId(1L));
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml"
    })
    void getAllRelationsEmptyForRegister() {
        softly
            .assertThat(registerUnitMapper.getAllRelationsForRegister(1L))
            .isEmpty();
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml"
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_1_removed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void remove() {
        registerUnitMapper.remove(1L);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml"
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_2_removed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void removeParent() {
        registerUnitMapper.remove(2L);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml"
    })
    @ExpectedDatabase(value = "/repository/register_unit/register_unit.xml", assertionMode = NON_STRICT_UNORDERED)
    void removeMissing() {
        registerUnitMapper.remove(3L);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml"
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_counts_updated.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateCounts() {
        registerUnitMapper.updateCounts(2L, List.of(
            new UnitCount().setCountType(CountType.DEFECT).setQuantity(103)
        ));
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit_count_orders.xml"
    })
    void ordersCount() {
        List<RegisterOrdersCountDto> ordersCountDtos = registerUnitMapper.ordersCount(List.of(1L, 2L));
        assertThatModelEquals(ITEMS_COUNT_DTOS, ordersCountDtos);
    }

    @Test
    @DatabaseSetup("/repository/register/register.xml")
    void testPersistUnit() {
        registerUnitMapper.persist(REGISTER_UNIT.setId(null));
        Assertions.assertEquals(1L, REGISTER_UNIT.getId());
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml"
    })
    void getByRegisterIdAndType() {
        softly
            .assertThat(registerUnitMapper.getByRegisterIdAndType(2, UnitType.ITEM))
            .containsExactlyInAnyOrder(RegisterFactory.newRegisterUnit());
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml"
    })
    void getByRegisterIdAndTypeEmpty() {
        softly
            .assertThat(registerUnitMapper.getByRegisterIdAndType(2, UnitType.BOX))
            .isEmpty();
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit.xml"
    })
    void getByRegisterIdAndTypeMultiple() {
        softly
            .assertThat(registerUnitMapper.getByRegisterIdAndType(2, UnitType.ITEM, UnitType.PALLET))
            .containsExactlyInAnyOrder(
                MINIMAL_REGISTER_UNIT,
                REGISTER_UNIT
            );
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit_count_orders.xml"
    })
    void countsByRegisterIdsAndTypePallet() {
        List<IdAndCount> count = registerUnitMapper.countsByRegisterIdsAndType(Set.of(1L), UnitType.PALLET);
        softly.assertThat(count).containsExactlyInAnyOrder(new IdAndCount(1L, 1));
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit_count_orders.xml"
    })
    void countsByRegisterIdsAndTypeWrongRegister() {
        List<IdAndCount> count = registerUnitMapper.countsByRegisterIdsAndType(Set.of(2L), UnitType.PALLET);
        softly.assertThat(count).isEmpty();
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit_count_orders.xml"
    })
    void countsByRegisterIdsAndTypeItem() {
        List<IdAndCount> count = registerUnitMapper.countsByRegisterIdsAndType(Set.of(2L), UnitType.ITEM);
        softly.assertThat(count).containsExactlyInAnyOrder(new IdAndCount(2L, 3));
    }

    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/register_dependencies.xml",
        "/repository/register/related_transportation.xml"
    })
    @Test
    void countsByTransportationUnitIdsRegisterTypeAndItemTypePlan() {
        softly.assertThat(registerUnitMapper.countsByTransportationUnitIdsRegisterTypeAndItemType(
                List.of(2L, 3L, 5L, 6L),
                RegisterType.PLAN,
                UnitType.PALLET
            ))
            .isEqualTo(List.of(
                new IdAndCount(3L, 2)
            ));
    }

    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/register_dependencies.xml",
        "/repository/register/related_transportation.xml"
    })
    @Test
    void countsByTransportationUnitIdsRegisterTypeAndItemTypeFact() {
        softly.assertThat(registerUnitMapper.countsByTransportationUnitIdsRegisterTypeAndItemType(
                List.of(2L, 3L, 5L, 6L),
                RegisterType.FACT,
                UnitType.PALLET
            ))
            .isEqualTo(Collections.emptyList());
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
        "/repository/tag/axapta_movement_order.xml",
        "/repository/transportation/xdoc_to_ff_transportations.xml",
        "/repository/transportation/xdoc_to_dc_transportations.xml",
    })
    void getBarcodesWithExistingTagCode() {
        List<String> barcodesWithExistingTagCode = registerUnitMapper.getBarcodesWithExistingTagCode(
            List.of("abc", "abc1", "abc2"),
            TagCode.AXAPTA_MOVEMENT_ORDER_ID
        );

        softly.assertThat(barcodesWithExistingTagCode).containsExactlyInAnyOrder(
            "abc1",
            "abc2"
        );
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/tag/xdoc_transport_plan.xml",
        "/repository/transportation/xdoc_to_ff_transportations.xml",
        "/repository/transportation/xdoc_to_dc_transportations.xml",
    })
    void getBarcodesWithExistingTagCodeNoCodes() {
        List<String> barcodesWithExistingTagCode = registerUnitMapper.getBarcodesWithExistingTagCode(
            List.of("abc", "abc1", "abc2"),
            TagCode.AXAPTA_MOVEMENT_ORDER_ID
        );

        softly.assertThat(barcodesWithExistingTagCode).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("getByTransportationUnitIdsCases")
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register/register_dependencies.xml",
        "/repository/register/related_transportation.xml"
    })
    void getByTransportationUnitIds(RegisterType registerType, UnitType unitType, Long[] expectedUnitIds) {
        softly
            .assertThat(
                registerUnitMapper.getByTransportationUnitIds(
                    List.of(2L, 3L),
                    registerType,
                    unitType
                )
            )
            .extracting(RegisterUnit::getId)
            .containsExactlyInAnyOrder(expectedUnitIds);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit_count_orders.xml"
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/after/after_marked_sent.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void markSent() {
        registerUnitMapper.markAsSent("task1", 2L, Set.of("456"));
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/after/after_marked_sent.xml"
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/register_unit_count_orders.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void markUnsent() {
        registerUnitMapper.markUnsent("task1", 2L);
    }

    @Test
    @DatabaseSetup({
        "/repository/register/register_dependencies_only_defect.xml"
    })
    void testGetFirstItemAsStock() {
        String firstItemStockType = registerUnitMapper.getFirstStockTypeInPlanRegister(1L);
        softly.assertThat(firstItemStockType).isEqualTo("DEFECT");
    }

    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit_count_orders.xml",
    })
    @Test
    void getIdsByRegisterIdAndBarcode() {
        softly
            .assertThat(registerUnitMapper.getIdsByRegisterIdAndBarcode(2L, List.of("456", "678")))
            .containsExactlyInAnyOrder(4L, 5L, 6L);
    }

    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit_count_type.xml"
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/after/remove_by_ids_with_children_cascade.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void removeByIdsWithChildrenCascade() {
        registerUnitMapper.removeByIdsWithChildrenCascade(2L,   List.of(1L));
    }

    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit_count_type.xml"
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/after/remove_by_ids_with_children_no_relation.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @Test
    void removeByIdsWithChildrenNoRelation() {
        registerUnitMapper.removeByIdsWithChildrenCascade(2L, List.of(3L));
    }

    @DatabaseSetup({
        "/repository/register/register.xml",
        "/repository/register_unit/register_unit_count_type.xml"
    })
    @Test
    void isEmpty() {
        softly.assertThat(registerUnitMapper.isEmpty(1L)).isTrue();
        softly.assertThat(registerUnitMapper.isEmpty(2L)).isFalse();
    }
    static Stream<Arguments> getByTransportationUnitIdsCases() {
        return Stream.of(
            Arguments.of(RegisterType.PLAN, UnitType.PALLET, new Long[]{1L, 4L}),
            Arguments.of(RegisterType.PLAN, UnitType.BOX, new Long[]{5L, 6L}),
            Arguments.of(RegisterType.PLAN, UnitType.ITEM, new Long[]{2L}),
            Arguments.of(RegisterType.FACT, UnitType.PALLET, new Long[]{}),
            Arguments.of(RegisterType.FACT, UnitType.BOX, new Long[]{}),
            Arguments.of(RegisterType.FACT, UnitType.ITEM, new Long[]{})
        );
    }
}
