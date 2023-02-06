package ru.yandex.market.tpl.core.domain.droppeditem;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingResultWithShiftDate;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class DroppedItemServiceTest extends TplAbstractTest {

    private final DroppedItemService droppedItemService;
    private final DroppedItemRepository droppedItemRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TestUserHelper testUserHelper;
    private final TestableClock clock;
    private final OrderGenerateService orderGenerateService;


    private Shift shift;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.DROPPED_ORDERS_SAVE_ENABLED, Boolean.TRUE);

        LocalDate shiftDate = LocalDate.now(clock);
        shift = testUserHelper.findOrCreateOpenShiftForSc(shiftDate, SortingCenter.DEFAULT_SC_ID);
    }

    @Test
    void saveDroppedItems() {
        List<DroppedItem> before = droppedItemRepository.findAll();
        assertThat(before).isEmpty();

        droppedItemService.saveDroppedItems(mockRoutingResultWithDroppedItems());

        List<DroppedItem> all = droppedItemRepository.findAll();
        assertThat(all).hasSize(4);

        assertThat(all)
                .extracting(DroppedItem::getShiftId)
                .containsOnly(shift.getId());

        assertThat(all)
                .extracting(DroppedItem::getExternalId)
                .containsExactlyInAnyOrder("ext123", "ext456", "ext789", "ТММ987");
    }

    private RoutingResultWithShiftDate mockRoutingResultWithDroppedItems() {
        // точки, дропнутые маршрутизацией
        Map<String, RoutingRequestItem> droppedItems = new HashMap<>();
        droppedItems.put("m_123_456", RoutingRequestItem.builder().ref("ext123_ext456").build());
        droppedItems.put("789", RoutingRequestItem.builder().ref("ext789").build());
        droppedItems.put("987", RoutingRequestItem.builder().ref("ТММ987").build());

        RoutingResult routingResult = RoutingResult.builder()
                .droppedItems(droppedItems)
                .build();

        return RoutingResultWithShiftDate.builder()
                .routingResult(routingResult)
                .shiftDate(shift.getShiftDate())
                .sortingCenterId(shift.getSortingCenter().getId())
                .build();
    }

    @Test
    void saveFarOrderItems() {
        List<DroppedItem> before = droppedItemRepository.findAll();
        assertThat(before).isEmpty();

        droppedItemService.saveDroppedItems(mockRoutingResultWithFarOrderItems());

        List<DroppedItem> all = droppedItemRepository.findAll();
        assertThat(all).hasSize(2);

        assertThat(all)
                .extracting(DroppedItem::getShiftId)
                .containsOnly(shift.getId());

        assertThat(all)
                .extracting(DroppedItem::getExternalId)
                .containsExactlyInAnyOrder("ext987", "ext654");
    }

    private RoutingResultWithShiftDate mockRoutingResultWithFarOrderItems() {
        // точки, отброшенные на этапе формирования запроса в маршрутизацию
        Order ext987 = orderGenerateService.createOrder("ext987");
        Order ext654 = orderGenerateService.createOrder("ext654");

        RoutingRequest routingRequest = RoutingRequest.builder()
                .farOrderIds(Set.of(ext987.getId(), ext654.getId()))
                .build();

        RoutingResult routingResult = RoutingResult.builder()
                .routingRequest(routingRequest)
                .build();

        return RoutingResultWithShiftDate.builder()
                .routingResult(routingResult)
                .shiftDate(shift.getShiftDate())
                .sortingCenterId(shift.getSortingCenter().getId())
                .build();
    }

    @Test
    void updateDroppedItems() {
        List<DroppedItem> before = droppedItemRepository.findAll();
        assertThat(before).isEmpty();

        droppedItemService.saveDroppedItems(mockRoutingResultWithDroppedItems());

        List<DroppedItem> all = droppedItemRepository.findAll();
        assertThat(all).hasSize(4);

        assertThat(all)
                .extracting(DroppedItem::getShiftId)
                .containsOnly(shift.getId());

        assertThat(all)
                .extracting(DroppedItem::getExternalId)
                .containsExactlyInAnyOrder("ext123", "ext456", "ext789", "ТММ987");

        droppedItemService.saveDroppedItems(mockRoutingResultWithFarOrderItems());

        List<DroppedItem> updated = droppedItemRepository.findAll();
        assertThat(updated).hasSize(2);

        assertThat(updated)
                .extracting(DroppedItem::getShiftId)
                .containsOnly(shift.getId());

        assertThat(updated)
                .extracting(DroppedItem::getExternalId)
                .containsExactlyInAnyOrder("ext987", "ext654");
    }
}
