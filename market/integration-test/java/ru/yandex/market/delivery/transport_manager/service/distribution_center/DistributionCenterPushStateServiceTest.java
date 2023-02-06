package ru.yandex.market.delivery.transport_manager.service.distribution_center;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitType;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.DistributionCenterStateDto;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.ResourceIdDto;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.Box;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.CenterType;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.Pallet;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.UnitCargoType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.DistributionCenterUnitMapper;

public class DistributionCenterPushStateServiceTest extends AbstractContextualTest {
    @Autowired
    private DistributionCenterPushStateService service;
    @Autowired
    private DistributionCenterUnitMapper unitMapper;

    private static final OffsetDateTime INBOUND_DATETIME = OffsetDateTime.of(2021, 4, 29, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final Map<String, String> BOX_TO_PALLETS = Map.of(
        "BOX001", "PALLET010",
        "BOX002", "PALLET010",
        "BOX003", "PALLET010",
        "BOX004", "PALLET020"
    );

    private static final DistributionCenterStateDto STATE_DTO = new DistributionCenterStateDto().setPallets(
        List.of(
            mockPallet("PALLET010", 2L, List.of(mockBox("BOX001", 2L), mockBox("BOX002", 2L), mockBox("BOX003", 2L))),
            mockPallet("PALLET020", 3L, List.of(mockBox("BOX004", 3L))),
            mockPalletOldFormat("PALLET030", 2L, Collections.emptyList()),
            new Pallet(
                "ANOMALY_PALLET",
                null,
                null,
                INBOUND_DATETIME,
                Collections.emptyList(),
                null,
                UnitCargoType.XDOCK
            )
        )
    );

    @Test
    @ExpectedDatabase(
        value = "/service/distribution_center_unit/after/add_all_units.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addAllUnits() {
        service.updateState(1L, STATE_DTO);
        checkBoxesNesting();
    }

    @Test
    @DatabaseSetup("/service/distribution_center_unit/before/remove_not_actual_units.xml")
    @ExpectedDatabase(
        value = "/service/distribution_center_unit/after/remove_not_actual_units.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void removeNotActualUnits() {
        service.updateState(1L, STATE_DTO);
    }

    @Test
    @DatabaseSetup("/service/distribution_center_unit/before/add_only_new_units.xml")
    @ExpectedDatabase(
        value = "/service/distribution_center_unit/after/add_only_new_units.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addOnlyNewUnits() {
        service.updateState(1L, STATE_DTO);
        checkBoxesNesting();
    }

    @Test
    @DatabaseSetup("/service/distribution_center_unit/before/do_not_change_state.xml")
    @ExpectedDatabase(
        value = "/service/distribution_center_unit/after/do_not_change_state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void doNotChangeState() {
        service.updateState(1L, STATE_DTO);
        checkBoxesNesting();
    }

    @Test
    @DatabaseSetup("/service/distribution_center_unit/before/update_full_state.xml")
    @ExpectedDatabase(
        value = "/service/distribution_center_unit/after/update_full_state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateFullState() {
        service.updateState(1L, STATE_DTO);
        checkBoxesNesting();
    }

    @Test
    @DatabaseSetup("/service/distribution_center_unit/before/remove_box_from_frozen_pallet.xml")
    @ExpectedDatabase(
        value = "/service/distribution_center_unit/after/remove_box_from_frozen_pallet.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void removeBoxFromFrozenPallet() {
        service.updateState(1L, STATE_DTO);
        checkBoxesNesting();
    }

    @Test
    @DatabaseSetup("/service/distribution_center_unit/before/add_box_to_frozen_pallet.xml")
    @ExpectedDatabase(
        value = "/service/distribution_center_unit/after/add_box_to_frozen_pallet.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addBoxToFrozenPallet() {
        service.updateState(1L, STATE_DTO);
        checkBoxesNesting();
    }

    @Test
    @DatabaseSetup("/service/distribution_center_unit/before/move_box_from_frozen_to_unfrozen_pallet.xml")
    @ExpectedDatabase(
        value = "/service/distribution_center_unit/after/move_box_from_frozen_to_unfrozen_pallet.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void moveBoxFromFrozenToUnfrozenPallet() {
        service.updateState(1L, STATE_DTO);
        checkBoxesNesting();
    }

    private void checkBoxesNesting() {
        Set<String> allDcUnitIds = unitMapper.getAllDcUnitIdsForOutboundPoint(1L);
        List<ResourceIdDto> mappingIds = unitMapper.getUnitIdsForOutboundPoint(1L, allDcUnitIds);

        List<DistributionCenterUnit> boxes = mappingIds.stream()
            .map(ResourceIdDto::getId)
            .map(id -> unitMapper.getById(id))
            .filter(unit -> unit.getType() == DistributionCenterUnitType.BOX)
            .collect(Collectors.toList());

        softly.assertThat(boxes.stream().map(DistributionCenterUnit::getDcUnitId))
            .containsAll(BOX_TO_PALLETS.keySet());

        boxes.forEach(box -> {
                softly.assertThat(box.getParentId()).isNotNull();
                String parentDcUnitId = unitMapper.getById(box.getParentId()).getDcUnitId();
                softly.assertThat(parentDcUnitId).isEqualTo(BOX_TO_PALLETS.get(box.getDcUnitId()));
            }
        );
    }

    static Pallet mockPallet(
        String id,
        Long targetPointId,
        List<Box> boxes
    ) {
        return new Pallet(
            id,
            targetPointId,
            "inbound" + targetPointId,
            INBOUND_DATETIME,
            boxes,
            CenterType.DISTRIBUTION_CENTER,
            UnitCargoType.XDOCK
        );
    }

    static Pallet mockPalletOldFormat(
        String id,
        Long targetPointId,
        List<Box> boxes
    ) {
        return new Pallet(
            id,
            targetPointId,
            "inbound" + targetPointId,
            INBOUND_DATETIME,
            boxes,
            null,
            UnitCargoType.XDOCK
        );
    }

    static Box mockBox(String id, Long targetPointId) {
        return new Box(
            id,
            "inbound" + targetPointId,
            INBOUND_DATETIME
        );
    }
}
