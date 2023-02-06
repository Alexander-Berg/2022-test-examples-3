package ru.yandex.market.tpl.core.domain.forecast;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.forecast.task.PartnerForecastQueuePayload;
import ru.yandex.market.tpl.core.domain.forecast.task.PartnerForecastQueueService;
import ru.yandex.market.tpl.core.service.forecast.PartnerForecast;
import ru.yandex.market.tpl.core.service.forecast.PartnerForecastForSCService;
import ru.yandex.market.tpl.core.service.forecast.PartnerForecastView;
import ru.yandex.market.tpl.core.service.forecast.enums.PartnerForecastStatus;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PartnerForecastQueueServiceTest {

    private static final String REQUEST_ID = "765432";
    private static final Long SORTING_CENTER_ID = 1L;
    private static final Integer REAL_ORDER_COUNTS = 543;
    private static final Double CREATED_ORDERS_PERCENT = 43.53D;
    private static final Double AVERAGE_DEPARTURES_VOLUME = 29.77D;
    private static final Double AVERAGE_TRANSPORT_CAPACITY = 52.93D;
    private static final Integer EXPECTED_ORDER_COUNT = 383;
    private static final Integer EXPECTED_COURIERS_COUNT = 934;
    private static final PartnerForecastStatus STATUS_SUCCESS = PartnerForecastStatus.SUCCESS;
    private static final PartnerForecastStatus STATUS_PROCESSING = PartnerForecastStatus.PROCESSING;

    @MockBean
    private PartnerForecastForSCService partnerForecastForSCService;
    private final PartnerForecastQueueService partnerForecastQueueService;
    private final PartnerForecastRepository partnerForecastRepository;

    @Test
    void checkCalculatedAndUpdatedPartnerForecast() {
        PartnerForecast partnerForecast = new PartnerForecast();
        partnerForecast.setSortingCenterId(SORTING_CENTER_ID);
        partnerForecast.setStatus(STATUS_PROCESSING);
        partnerForecastRepository.save(partnerForecast);

        PartnerForecastView partnerForecastView = new PartnerForecastView();
        partnerForecastView.setSortingCenterId(SORTING_CENTER_ID);
        partnerForecastView.setRealOrdersCount(REAL_ORDER_COUNTS);
        partnerForecastView.setPercentCreatedOrders(CREATED_ORDERS_PERCENT);
        partnerForecastView.setAvgDeparturesVolume(AVERAGE_DEPARTURES_VOLUME);
        partnerForecastView.setAvgTransportCapacity(AVERAGE_TRANSPORT_CAPACITY);
        partnerForecastView.setExpectedOrderCount(EXPECTED_ORDER_COUNT);
        partnerForecastView.setExpectedCouriersCount(EXPECTED_COURIERS_COUNT);

        Mockito.when(partnerForecastForSCService.getForecastForSortingCenter(SORTING_CENTER_ID))
                .thenReturn(partnerForecastView);

        partnerForecastQueueService.processPayload(new PartnerForecastQueuePayload(REQUEST_ID, SORTING_CENTER_ID));

        Optional<PartnerForecast> firstBySortingCenterIdOrderByCreatedAtDesc =
                partnerForecastRepository.findFirstBySortingCenterIdOrderByCreatedAtDesc(SORTING_CENTER_ID);
        assertThat(firstBySortingCenterIdOrderByCreatedAtDesc).isPresent();
        PartnerForecast savedForecast = firstBySortingCenterIdOrderByCreatedAtDesc.get();
        assertThat(savedForecast.getSortingCenterId()).isEqualTo(SORTING_CENTER_ID);
        assertThat(savedForecast.getRealOrdersCount()).isEqualTo(REAL_ORDER_COUNTS);
        assertThat(savedForecast.getPercentCreatedOrders()).isEqualTo(CREATED_ORDERS_PERCENT);
        assertThat(savedForecast.getAvgDeparturesVolume()).isEqualTo(AVERAGE_DEPARTURES_VOLUME);
        assertThat(savedForecast.getAvgTransportCapacity()).isEqualTo(AVERAGE_TRANSPORT_CAPACITY);
        assertThat(savedForecast.getExpectedOrderCount()).isEqualTo(EXPECTED_ORDER_COUNT);
        assertThat(savedForecast.getExpectedCouriersCount()).isEqualTo(EXPECTED_COURIERS_COUNT);
        assertThat(savedForecast.getStatus()).isEqualTo(STATUS_SUCCESS);
    }
}
