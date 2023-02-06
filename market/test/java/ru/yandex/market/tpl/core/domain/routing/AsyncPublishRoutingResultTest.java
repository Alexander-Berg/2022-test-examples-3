package ru.yandex.market.tpl.core.domain.routing;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogDao;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingResultWithShiftDate;
import ru.yandex.market.tpl.core.domain.routing.partner.PartnerRoutingInfo;
import ru.yandex.market.tpl.core.domain.routing.partner.PartnerRoutingInfoRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResultStatus;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.service.routing.PartnerRoutingService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class AsyncPublishRoutingResultTest extends TplAbstractTest {

    private static final String PROCESSING_ID = "processingId";

    private static final long ANOTHER_SORTING_CENTER_ID = 47819L;
    private final LocalDate date = LocalDate.parse("2023-11-20");

    private final RoutingApiDataHelper helper = new RoutingApiDataHelper();

    private final TestUserHelper testUserHelper;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final OrderGenerateService orderGenerateService;
    private final SortingCenterService sortingCenterService;

    private final ShiftManager shiftManager;
    private final PartnerRoutingService partnerRoutingService;

    @MockBean
    private RoutingLogDao routingLogDao;
    @MockBean
    private PartnerRoutingInfoRepository partnerRoutingInfoRepository;

    private RoutingResultWithShiftDate mockRoutingResult;
    private PartnerRoutingInfo mockPartnerRoutingInfo;

    @BeforeEach
    public void prepareShift() {
        reset(routingLogDao);

        long sortingCenterId = ANOTHER_SORTING_CENTER_ID;
        long uid = sortingCenterId + 123L;
        var courier = testUserHelper.findOrCreateUserForSc(uid, date, sortingCenterId);

        List<Shift> shifts = shiftManager.assignShiftsForDate(date, sortingCenterId);

        Shift shift = shiftManager.findOrThrow(date, sortingCenterId);

        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(OrderFlowStatus.CREATED)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .build())
                .deliveryServiceId(sortingCenterService.findDsForSortingCenter(
                        shift.getSortingCenter().getId()).get(0).getId())
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .deliveryDate(date)
                .build());

        RoutingRequest routingRequest = helper.getRoutingRequest(courier.getId(), date, 0);
        RoutingResult routingResult = helper.mockResult(routingRequest, false);

        mockRoutingResult = new RoutingResultWithShiftDate(routingResult, date,
                ANOTHER_SORTING_CENTER_ID, null);

        mockPartnerRoutingInfo = new PartnerRoutingInfo();
        mockPartnerRoutingInfo.setRoutingDate(date);
        mockPartnerRoutingInfo.setSortingCenterId(ANOTHER_SORTING_CENTER_ID);
        mockPartnerRoutingInfo.setMock(RoutingMockType.MANUAL);
        mockPartnerRoutingInfo.setOrdersCount(100);
        mockPartnerRoutingInfo.setMultiOrdersCount(100);
        mockPartnerRoutingInfo.setRoutesCount(7);
        mockPartnerRoutingInfo.setDroppedOrdersCount(0);
        mockPartnerRoutingInfo.setFailedTimeShiftsDuration(0);
        mockPartnerRoutingInfo.setLatenessRiskLocationsCount(0);
        mockPartnerRoutingInfo.setLateShiftsCount(0);
    }

    @Test
    public void shouldCreateTaskForPublishRoutingResult() {
        when(routingLogDao.findResultByProcessingId(PROCESSING_ID))
                .thenReturn(Optional.of(mockRoutingResult));
        when(partnerRoutingInfoRepository.findByRoutingId(PROCESSING_ID))
                .thenReturn(Optional.of(mockPartnerRoutingInfo));

        partnerRoutingService.publishRoutingAsync(PROCESSING_ID);

        dbQueueTestUtil.assertQueueHasSingleEvent(QueueType.PUBLISH_ROUTING_RESULT, PROCESSING_ID);

        verify(routingLogDao).updatePublishingStatus(PROCESSING_ID, RoutingResultStatus.IN_PROGRESS);
    }
}
