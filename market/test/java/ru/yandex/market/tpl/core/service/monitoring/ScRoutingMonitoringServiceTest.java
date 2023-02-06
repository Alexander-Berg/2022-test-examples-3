package ru.yandex.market.tpl.core.service.monitoring;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.common.util.metric.KeyValueMetric;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogDao;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingResultWithShiftDate;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrdersVerificationLogCommandService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftRepository;
import ru.yandex.market.tpl.core.external.delivery.sc.SortCenterDirectClient;
import ru.yandex.market.tpl.core.external.delivery.sc.dto.ScRoutingResult;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.test.TplMonitoringTestFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScRoutingMonitoringServiceTest {

    public static final long EXISTED_SHIFT_ID = 1L;
    public static final long EXISTED_SHIFT_SC_ID = 10L;
    public static final String EXISTED_SHIFT_SC_TOKEN = "SC_TOKEN";
    public static final LocalDate EXISTED_SHIFT_DATE = LocalDate.of(2021, 5, 29);
    public static final String EXISTED_ORDER_EXTERNAL_ID = "externalOrderId";
    public static final String COURIER_ID = "userUid";
    public static final String MISMATCH_ORDER_ID = "MISMATCH_ORDER_ID";
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ShiftRepository shiftRepository;
    @Mock
    private SortCenterDirectClient sortCenterDirectClient;
    @Mock
    private ConfigurationProviderAdapter configurationProvider;
    @Mock
    private TplLogMonitoringService logMonitoringService;
    @Mock
    private RoutingLogDao routingLogDao;
    @Mock
    private ScOrdersVerificationLogCommandService verificationLogService;
    @InjectMocks
    private ScRoutingMonitoringService routingMonitoringService;
    private final String processingId = "processingId";

    @Test
    void verificationRouting_whenBothNotEmpty_Valid() {
        //given
        doReturn(Collections.singletonList(
                TplMonitoringTestFactory.createTplOrderUserItem(EXISTED_ORDER_EXTERNAL_ID, COURIER_ID)))
                .when(orderRepository).findExpectedOnScOrdersByShiftId(any());

        doReturn(Optional.of(new ScRoutingResult(
                Collections.singletonList(
                        TplMonitoringTestFactory.createScOrderCourierItem(EXISTED_ORDER_EXTERNAL_ID, COURIER_ID)))))
                .when(sortCenterDirectClient).getRoutingResult(EXISTED_SHIFT_DATE, EXISTED_SHIFT_SC_TOKEN);


        Shift mockedShift = createMockedShift();

        doReturn(Optional.of(mockedShift)).when(shiftRepository).findByShiftDateAndSortingCenterId(any(), anyLong());


        //when
        routingMonitoringService.verificationRouting(EXISTED_SHIFT_DATE, EXISTED_SHIFT_ID, processingId);

        //then
        verify(logMonitoringService, never()).increment(eq(KeyValueMetric.SC_VERIFY_ROUTING_RESULT_FAIL),
                any(String.class));
    }

    @Test
    void verificationRouting_whenBothEmpty_Valid() {
        //given
        doReturn(Collections.emptyList()).when(orderRepository).findExpectedOnScOrdersByShiftId(any());

        doReturn(Optional.of(new ScRoutingResult(Collections.emptyList()))).when(sortCenterDirectClient)
                .getRoutingResult(EXISTED_SHIFT_DATE, EXISTED_SHIFT_SC_TOKEN);


        Shift mockedShift = createMockedShift();

        doReturn(Optional.of(mockedShift)).when(shiftRepository).findByShiftDateAndSortingCenterId(any(), anyLong());


        //when
        routingMonitoringService.verificationRouting(EXISTED_SHIFT_DATE, EXISTED_SHIFT_ID, processingId);

        //then
        verify(logMonitoringService, never()).increment(eq(KeyValueMetric.SC_VERIFY_ROUTING_RESULT_FAIL),
                any(String.class));
    }

    @Test
    void verificationRouting_whenInvalidMismatch() {
        //given
        doReturn(Optional.of(0)).when(configurationProvider)
                .getValueAsInteger(eq(ConfigurationProperties.VALID_ROUTING_MONITORING_SC_GAP));

        doReturn(Collections.singletonList(
                TplMonitoringTestFactory.createTplOrderUserItem(MISMATCH_ORDER_ID, COURIER_ID)))
                .when(orderRepository).findExpectedOnScOrdersByShiftId(any());

        doReturn(Optional.of(new ScRoutingResult(
                Collections.singletonList(
                        TplMonitoringTestFactory.createScOrderCourierItem(EXISTED_ORDER_EXTERNAL_ID, COURIER_ID)))))
                .when(sortCenterDirectClient).getRoutingResult(EXISTED_SHIFT_DATE, EXISTED_SHIFT_SC_TOKEN);

        Shift mockedShift = createMockedShift();

        doReturn(Optional.of(mockedShift)).when(shiftRepository).findByShiftDateAndSortingCenterId(any(), anyLong());


        //when
        routingMonitoringService.verificationRouting(EXISTED_SHIFT_DATE, EXISTED_SHIFT_SC_ID, processingId);

        //then
        verify(logMonitoringService, times(1)).increment(eq(KeyValueMetric.SC_VERIFY_ROUTING_RESULT_FAIL),
                any(String.class));
    }

    @Test
    void verificationRouting_whenValidMismatch() {
        //given
        doReturn(Optional.of(3)).when(configurationProvider)
                .getValueAsInteger(eq(ConfigurationProperties.VALID_ROUTING_MONITORING_SC_GAP));

        doReturn(Arrays.asList(
                TplMonitoringTestFactory.createTplOrderUserItem(EXISTED_ORDER_EXTERNAL_ID, COURIER_ID),
                TplMonitoringTestFactory.createTplOrderUserItem("MISMATCH_ORDER_ID2", COURIER_ID),
                TplMonitoringTestFactory.createTplOrderUserItem("MISMATCH_ORDER_ID3", COURIER_ID),
                TplMonitoringTestFactory.createTplOrderUserItem("MISMATCH_ORDER_ID4", COURIER_ID)))
                .when(orderRepository).findExpectedOnScOrdersByShiftId(any());

        doReturn(Optional.of(new ScRoutingResult(
                Collections.singletonList(
                        TplMonitoringTestFactory.createScOrderCourierItem(EXISTED_ORDER_EXTERNAL_ID, COURIER_ID)))))
                .when(sortCenterDirectClient).getRoutingResult(EXISTED_SHIFT_DATE, EXISTED_SHIFT_SC_TOKEN);


        Shift mockedShift = createMockedShift();

        doReturn(Optional.of(mockedShift)).when(shiftRepository).findByShiftDateAndSortingCenterId(any(), anyLong());


        //when
        routingMonitoringService.verificationRouting(EXISTED_SHIFT_DATE, EXISTED_SHIFT_SC_ID, processingId);

        //then
        verify(logMonitoringService, never()).increment(eq(KeyValueMetric.SC_VERIFY_ROUTING_RESULT_FAIL),
                any(String.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void verificationRouting_skipPartial(boolean isSupportSameRoutingProcessingIds) {
        doReturn(true)
                .when(configurationProvider)
                .isBooleanEnabled(eq(ConfigurationProperties.VALID_ROUTING_MONITORING_PARTIAL_DISABLED));
        doReturn(isSupportSameRoutingProcessingIds)
                .when(configurationProvider)
                .isBooleanEnabled(eq(ConfigurationProperties.SUPPORT_SAME_ROUTING_PROCESSING_IDS_ENABLED));

        RoutingResult routingResult = RoutingResult.builder()
                .profileType(RoutingProfileType.PARTIAL)
                .build();
        RoutingResultWithShiftDate routingResultWithShiftDate = RoutingResultWithShiftDate.builder()
                .routingResult(routingResult)
                .build();

        if(isSupportSameRoutingProcessingIds) {
            doReturn(Optional.of(routingResultWithShiftDate))
                    .when(routingLogDao)
                    .findResultByProcessingId(eq(processingId), eq(RoutingProfileType.PARTIAL));
        } else {
            doReturn(Optional.of(routingResultWithShiftDate))
                    .when(routingLogDao)
                    .findResultByProcessingId(eq(processingId));
        }

        routingMonitoringService.verificationRouting(EXISTED_SHIFT_DATE, EXISTED_SHIFT_ID, processingId);

        verify(shiftRepository, never()).findByShiftDateAndSortingCenterId(any(), anyLong());
    }

    private Shift createMockedShift() {
        Shift shift = mock(Shift.class);
        when(shift.getId()).thenReturn(EXISTED_SHIFT_ID);
        SortingCenter sortingCenter = new SortingCenter();
        sortingCenter.setId(EXISTED_SHIFT_SC_ID);
        sortingCenter.setToken(EXISTED_SHIFT_SC_TOKEN);
        when(shift.getSortingCenter()).thenReturn(sortingCenter);
        when(shift.getShiftDate()).thenReturn(EXISTED_SHIFT_DATE);
        return shift;
    }
}
