package ru.yandex.market.tpl.carrier.core.domain.rating;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.rating.model.RatingCommand;
import ru.yandex.market.tpl.carrier.core.domain.rating.model.RatingCommandService;
import ru.yandex.market.tpl.carrier.core.domain.rating.model.RatingEntityType;
import ru.yandex.market.tpl.carrier.core.domain.rating.model.RatingType;

@Service
@RequiredArgsConstructor
public class RatingHelper {

    private final RatingCommandService ratingCommandService;
    private final TestableClock clock;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    public void createRatings(Long entityId) {
        Instant now = Instant.now(clock);
        List<Instant> createdAtList = List.of(now,
                now.minus(1, ChronoUnit.DAYS),
                now.minus(2, ChronoUnit.DAYS),
                now.minus(3, ChronoUnit.DAYS),
                now.minus(4, ChronoUnit.DAYS),
                now.minus(5, ChronoUnit.DAYS),
                now.minus(6, ChronoUnit.DAYS));
        int ratingIterator = 0;
        for (Instant createdAt : createdAtList) {
            //clock.setFixed(createdAt, ZoneId.of("UTC"));
            for (RatingType ratingType : RatingType.values()) {
                for (RatingEntityType ratingEntityType : RatingEntityType.values()) {
                    ratingCommandService.create(
                            RatingCommand.Create.builder()
                                    .ratingValue(42L + ratingIterator)
                                    .ratingType(ratingType)
                                    .ratingEntityType(ratingEntityType)
                                    .entityId(entityId)
                                    .build());
                }
            }
            ratingIterator++;
        }

        configurationServiceAdapter.mergeValue(ConfigurationProperties.RUNS_FINISHED_IN_APPLICATION_RATING_CONFIGURATION, "80, 90");
        configurationServiceAdapter.mergeValue(ConfigurationProperties.RUNS_ASSIGNED_DRIVER_AND_TRANSPORT_RATING_CONFIGURATION, "80, 90");
        configurationServiceAdapter.mergeValue(ConfigurationProperties.RUNS_PROCESSED_BY_CARRIER_RATING_CONFIGURATION, "80, 90");
    }
}
