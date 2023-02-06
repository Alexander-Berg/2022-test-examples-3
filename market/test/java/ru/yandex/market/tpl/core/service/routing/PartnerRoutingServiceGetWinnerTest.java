package ru.yandex.market.tpl.core.service.routing;

import java.time.LocalDate;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
public class PartnerRoutingServiceGetWinnerTest {

    private final PartnerRoutingService partnerRoutingService;

    private final LocalDate shiftDate = LocalDate.parse("2020-04-20");

    @Test
    void shouldReturnEmptyIfAbsentPreRoutingResults() {
        Optional<RoutingProfileType> strategyO =
                partnerRoutingService.getWinnerStrategyForShift(SortingCenter.DEFAULT_SC_ID, shiftDate);

        assertThat(strategyO).isEmpty();
    }

    @Test
    @Sql("classpath:mockRoutingResult/oneResultWithoutDroppedPoint.sql")
    void shouldReturnStrategyWithoutDroppedPoints() {
        Optional<RoutingProfileType> strategyO =
                partnerRoutingService.getWinnerStrategyForShift(SortingCenter.DEFAULT_SC_ID, shiftDate);

        assertThat(strategyO).isPresent();
        assertThat(strategyO.get()).isEqualTo(RoutingProfileType.GROUP_FALLBACK_2);
    }

    @Test
    @Sql("classpath:mockRoutingResult/resultWithFailedTimeLocationMinTransitDistance.sql")
    void shouldReturnStrategyWithMinTransitDistance() {
        Optional<RoutingProfileType> strategyO =
                partnerRoutingService.getWinnerStrategyForShift(SortingCenter.DEFAULT_SC_ID, shiftDate);

        assertThat(strategyO).isPresent();
        assertThat(strategyO.get()).isEqualTo(RoutingProfileType.GROUP_FALLBACK_1);
    }

    @Test
    @Sql("classpath:mockRoutingResult/resultWithFailedTimeLocationMinLatenessLocation.sql")
    void shouldReturnStrategyWithMinLatenessLocationCount() {
        Optional<RoutingProfileType> strategyO =
                partnerRoutingService.getWinnerStrategyForShift(SortingCenter.DEFAULT_SC_ID, shiftDate);

        assertThat(strategyO).isPresent();
        assertThat(strategyO.get()).isEqualTo(RoutingProfileType.GROUP);
    }


}
