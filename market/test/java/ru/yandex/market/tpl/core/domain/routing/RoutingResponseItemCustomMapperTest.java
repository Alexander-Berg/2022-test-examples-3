package ru.yandex.market.tpl.core.domain.routing;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.google.common.base.Joiner;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestCollector;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResponseItem;
import ru.yandex.market.tpl.core.external.routing.vrp.model.OrderLocation;
import ru.yandex.market.tpl.core.service.order.collector.OrderRoutableCollector;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class RoutingResponseItemCustomMapperTest extends TplAbstractTest {

    private final RoutingResponseItemCustomMapper customMapper;
    private final OrderRoutableCollector orderCollector;
    private final SpecialRequestCollector specialRequestCollector;
    private final Clock clock;

    @Test
    void mapRoutableGroupItemTest() {
        var orderIds = List.of(11L, 33L, 55L, 77L);
        var logisticRequestIds = List.of(22L, 44L, 66L);

        // mock response
        var orderLocation = new OrderLocation();
        orderLocation.setId(makeId(orderIds, logisticRequestIds, false));
        var requestItem = mock(RoutingRequestItem.class);
        when(requestItem.getType()).thenReturn(RoutingRequestItemType.LOCKER);
        when(requestItem.getSubTaskCount()).thenReturn(7);
        var now = Instant.now(clock);

        // test mapping
        var mappedResponse = customMapper.mapItem(orderLocation, requestItem, now);
        assertThat(mappedResponse.getExpectedFinishTime()).isEqualTo(now);
        checkSubtasks(orderIds, logisticRequestIds, mappedResponse);

        // test another ids order
        orderLocation.setId(makeId(orderIds, logisticRequestIds, true));
        mappedResponse = customMapper.mapItem(orderLocation, requestItem, now);
        checkSubtasks(orderIds, logisticRequestIds, mappedResponse);
    }

    @Test
    void isApplicableTest() {
        var orderLocation = new OrderLocation();
        orderLocation.setId(RoutableGroupPackager.GROUP_PREFIX + "1234");
        assertThat(customMapper.isApplicable(orderLocation)).isTrue();

        orderLocation.setId("1234");
        assertThat(customMapper.isApplicable(orderLocation)).isFalse();

        orderLocation.setId("m_1234_234");
        assertThat(customMapper.isApplicable(orderLocation)).isFalse();
    }

    private void checkSubtasks(List<Long> orderIds, List<Long> logisticRequestIds, RoutingResponseItem response) {
        // subtask ids
        assertThat(response.getSubTaskIds()).hasSize(7);
        assertThat(response.getSubTaskIds()).containsAll(orderIds);
        assertThat(response.getSubTaskIds()).containsAll(logisticRequestIds);

        // subtask logistic request flag
        assertThat(response.getSubTasks()).hasSize(7);
        for (var subtask : response.getSubTasks()) {
            assertThat(subtask.getType()).isEqualTo(RoutingRequestItemType.LOCKER);
            if (logisticRequestIds.contains(subtask.getId())) {
                assertThat(subtask.isLogisticRequest()).isTrue();
            } else {
                assertThat(subtask.isLogisticRequest()).isFalse();
            }
        }
    }

    private String makeId(List<Long> orderIds, List<Long> logisticRequestIds, boolean inverseOrder) {
        var orderTaskId = orderCollector.getEntityPrefix() + RoutableGroupPackager.MULTI_ITEM_ID_DELIMITER +
                Joiner.on(RoutableGroupPackager.MULTI_ITEM_ID_DELIMITER).join(orderIds);
        var lrTaskId = specialRequestCollector.getEntityPrefix() + RoutableGroupPackager.MULTI_ITEM_ID_DELIMITER +
                Joiner.on(RoutableGroupPackager.MULTI_ITEM_ID_DELIMITER).join(logisticRequestIds);

        return RoutableGroupPackager.GROUP_PREFIX +
                (inverseOrder ? orderTaskId : lrTaskId) +
                RoutableGroupPackager.MULTI_ITEM_ID_DELIMITER +
                (inverseOrder ? lrTaskId : orderTaskId);
    }

}
