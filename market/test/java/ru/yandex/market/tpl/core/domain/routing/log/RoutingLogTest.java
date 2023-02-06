package ru.yandex.market.tpl.core.domain.routing.log;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.routing.PartnerRoutingInfoDto;
import ru.yandex.market.tpl.core.external.routing.api.RoutingApiEvent;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpResponse;
import ru.yandex.market.tpl.core.external.routing.vrp.model.SolutionMetrics;
import ru.yandex.market.tpl.core.service.routing.PartnerRoutingService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.util.TplCoreTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class RoutingLogTest extends TplAbstractTest {

    private final RoutingApiDataHelper routingApiDataHelper = new RoutingApiDataHelper();
    private final Clock clock;
    private final PartnerRoutingService partnerRoutingService;
    private final RoutingLogDao routingLogDao;


    @Test
    void shouldSaveShiftDateAndSortingCenterIdOnCreatingRoutingRequest() {
        LocalDate shiftDate = LocalDate.now(clock);
        int expectedOrderCount = 10;
        int expectedMultiOrdersCount = 10;
        int expectedRoutesCount = 1;

        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(123L, shiftDate, expectedOrderCount, 0);
        routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest, routingRequest.getProfileType()));

        PartnerRoutingInfoDto routingInfoDto =
                partnerRoutingService.findRoutingInfoByRequestId(routingRequest.getRequestId());

        assertThat(routingInfoDto.getRoutingDate()).isEqualTo(shiftDate);
        assertThat(routingInfoDto.getSortingCenterId()).isEqualTo(routingRequest.getDepot().getId());
        assertThat(routingInfoDto.getOrdersCount()).isEqualTo(expectedOrderCount);
        assertThat(routingInfoDto.getMultiOrdersCount()).isEqualTo(expectedMultiOrdersCount);
        assertThat(routingInfoDto.getRoutesCount()).isEqualTo(expectedRoutesCount);

        // для еще не полученных метрик маршрута должны быть нули
        assertThat(routingInfoDto.getDroppedOrdersCount()).isEqualTo(0);
        assertThat(routingInfoDto.getLateShiftsCount()).isEqualTo(0);
        assertThat(routingInfoDto.getFailedTimeShiftsDuration()).isEqualTo(0);
        assertThat(routingInfoDto.getLatenessRiskLocationsCount()).isEqualTo(0);
        assertThat(routingInfoDto.getFailedTimeLocationsDuration()).isEqualTo(0);
        assertThat(routingInfoDto.getLateLocationsCount()).isEqualTo(0);
    }

    @SneakyThrows
    @Test
    void shouldSaveRoutingMetricsOnUpdateRawResponse() {
        LocalDate shiftDate = LocalDate.parse("2019-11-19");
        int expectedOrderCount = 10;

        MvrpResponse mvrpResponse = TplCoreTestUtils.mapFromResource("/vrp/vrp_mvrp_response.json", MvrpResponse.class);

        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(123L, shiftDate, expectedOrderCount, 0);
        routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest, routingRequest.getProfileType()));

        routingLogDao.updateRawResponse(
                new RoutingApiEvent.ResponseReceived(routingRequest.getRequestId(), mvrpResponse));

        PartnerRoutingInfoDto routingInfoDto =
                partnerRoutingService.findRoutingInfoByRequestId(routingRequest.getRequestId());

        SolutionMetrics metrics = mvrpResponse.getMetrics();
        assertThat(routingInfoDto.getDroppedOrdersCount()).isEqualTo(metrics.getDroppedLocationsCount());
        assertThat(routingInfoDto.getLateShiftsCount()).isEqualTo(metrics.getLateShiftsCount());
        assertThat(routingInfoDto.getFailedTimeShiftsDuration()).isEqualTo(metrics.getFailedTimeWindowShiftsDurationS().intValue());
        assertThat(routingInfoDto.getLatenessRiskLocationsCount()).isEqualTo(metrics.getLatenessRiskLocationsCount());
        assertThat(routingInfoDto.getFailedTimeLocationsDuration()).isEqualTo(metrics.getFailedTimeWindowLocationsDurationS().intValue());
        assertThat(routingInfoDto.getLateLocationsCount()).isEqualTo(metrics.getLateLocationsCount());
    }

    @SneakyThrows
    @Test
    void shouldSaveRoutingMetricsOnFinishRouting() {
        LocalDate shiftDate = LocalDate.parse("2019-11-19");
        int expectedOrderCount = 10;

        MvrpResponse mvrpResponse = TplCoreTestUtils.mapFromResource("/vrp/vrp_mvrp_response.json", MvrpResponse.class);

        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(123L, shiftDate, expectedOrderCount, 0);
        routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest, routingRequest.getProfileType()));

        routingLogDao.updateAtFinished(
                routingRequest.getRequestId(),
                routingApiDataHelper.mockResult(routingRequest, false),
                mvrpResponse);

        PartnerRoutingInfoDto routingInfoDto =
                partnerRoutingService.findRoutingInfoByRequestId(routingRequest.getRequestId());

        SolutionMetrics metrics = mvrpResponse.getMetrics();
        assertThat(routingInfoDto.getDroppedOrdersCount()).isEqualTo(metrics.getDroppedLocationsCount());
        assertThat(routingInfoDto.getLateShiftsCount()).isEqualTo(metrics.getLateShiftsCount());
        assertThat(routingInfoDto.getFailedTimeShiftsDuration()).isEqualTo(metrics.getFailedTimeWindowShiftsDurationS().intValue());
        assertThat(routingInfoDto.getLatenessRiskLocationsCount()).isEqualTo(metrics.getLatenessRiskLocationsCount());
        assertThat(routingInfoDto.getFailedTimeLocationsDuration()).isEqualTo(metrics.getFailedTimeWindowLocationsDurationS().intValue());
        assertThat(routingInfoDto.getLateLocationsCount()).isEqualTo(metrics.getLateLocationsCount());

    }
}
