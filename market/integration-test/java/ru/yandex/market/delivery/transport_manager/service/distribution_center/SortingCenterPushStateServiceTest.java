package ru.yandex.market.delivery.transport_manager.service.distribution_center;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitType;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.DistributionCenterStateDto;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.ResourceIdDto;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.Bag;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.CenterType;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.ReturnBox;
import ru.yandex.market.delivery.transport_manager.repository.mappers.DistributionCenterUnitMapper;

import static ru.yandex.market.delivery.transport_manager.service.distribution_center.DistributionCenterPushStateServiceTest.mockBox;
import static ru.yandex.market.delivery.transport_manager.service.distribution_center.DistributionCenterPushStateServiceTest.mockPallet;
import static ru.yandex.market.delivery.transport_manager.service.distribution_center.DistributionCenterPushStateServiceTest.mockPalletOldFormat;

public class SortingCenterPushStateServiceTest extends AbstractContextualTest {
    @Autowired
    private DistributionCenterPushStateService service;
    @Autowired
    private DistributionCenterUnitMapper unitMapper;

    private static final Map<String, String> BOX_TO_BAGS = Map.of(
        "place1.order01", "BAG010",
        "place2.order01", "BAG010",
        "order02", "BAG010",
        "order04", "BAG020"
    );

    private static final DistributionCenterStateDto STATE_DTO = new DistributionCenterStateDto().setBags(
        List.of(
            mockBag(
                "BAG010",
                2L,
                List.of(
                    mockReturnBox("order01", "place1"),
                    mockReturnBox("order01", "place2"),
                    mockReturnBox("order02", null)
                )
            ),
            mockBag("BAG020", 3L, List.of(mockReturnBox("order04", null))),
            mockBag("BAG030", 2L, Collections.emptyList())
        )
    );

    private static final DistributionCenterStateDto DC_STATE_DTO = new DistributionCenterStateDto().setPallets(
        List.of(
            mockPallet("PALLET010", 2L, List.of(mockBox("BOX001", 2L), mockBox("BOX002", 2L), mockBox("BOX003", 2L))),
            mockPallet("PALLET020", 3L, List.of(mockBox("BOX004", 3L))),
            mockPalletOldFormat("PALLET030", 2L, Collections.emptyList())
        )
    );

    @Test
    @ExpectedDatabase(
        value = "/service/sorting_center_unit/after/add_all_units.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addAllUnits() {
        service.updateState(10L, STATE_DTO);
        checkBoxesNesting();
    }

    @Test
    @ExpectedDatabase(
        value = "/service/sorting_center_unit/after/do_not_change_xdoc_state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void doNotChangeStateOfXdoc() {
        service.updateState(1L, DC_STATE_DTO);
        service.updateState(10L, STATE_DTO);
    }

    private void checkBoxesNesting() {
        Set<String> allDcUnitIds = unitMapper.getAllDcUnitIdsForOutboundPoint(10L);
        List<ResourceIdDto> mappingIds = unitMapper.getUnitIdsForOutboundPoint(10L, allDcUnitIds);

        List<DistributionCenterUnit> boxes = mappingIds.stream()
            .map(ResourceIdDto::getId)
            .map(id -> unitMapper.getById(id))
            .filter(unit -> unit.getType() == DistributionCenterUnitType.BOX)
            .collect(Collectors.toList());

        softly.assertThat(boxes.stream().map(DistributionCenterUnit::getDcUnitId))
            .containsAll(BOX_TO_BAGS.keySet());

        boxes.forEach(box -> {
                softly.assertThat(box.getParentId()).isNotNull();
                String parentDcUnitId = unitMapper.getById(box.getParentId()).getDcUnitId();
                softly.assertThat(parentDcUnitId).isEqualTo(BOX_TO_BAGS.get(box.getDcUnitId()));
            }
        );
    }

    private static Bag mockBag(
        String id,
        Long targetPointId,
        List<ReturnBox> boxes
    ) {
        return new Bag(
            id,
            targetPointId,
            // для мешков СЦ эти поля на данный момент неприменимы
            null,
            null,
            boxes,
            CenterType.SORTING_CENTER,
            String.format("Cell %s", id)
        );
    }

    private static ReturnBox mockReturnBox(String orderId, String placeId) {
        return new ReturnBox(
            orderId,
            placeId
        );
    }
}
