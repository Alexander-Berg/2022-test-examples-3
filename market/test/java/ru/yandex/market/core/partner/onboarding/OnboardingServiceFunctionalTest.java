package ru.yandex.market.core.partner.onboarding;

import java.time.Clock;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feature.model.FeatureType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DbUnitDataSet(
        before = "OnboardingUtilsFunctionalTest.before.csv"
)
class OnboardingServiceFunctionalTest extends FunctionalTest {
    private static final Collection<Long> partnerIds = List.of(
            1L,
            2L,
            3L,
            11L,
            12L,
            13L
    );
    private static final Collection<FeatureType> featureTypes = List.of(
            FeatureType.CROSSDOCK,
            FeatureType.DROPSHIP
    );

    @Autowired
    OnboardingService onboardingService;

    @Autowired
    private Clock clock;

    @BeforeEach
    void init() {
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
    }
    @Test
    @DbUnitDataSet(
            after = "OnboardingUtilsFunctionalTest.after.csv"
    )
    void tryToEnableFeature() {
        for (FeatureType featureType : featureTypes) {
            for (long partnerId : partnerIds) {
                var info = onboardingService.tryToEnableFeature(partnerId, featureType);
                assertThat(info.getShopId()).isEqualTo(partnerId);
                assertThat(info.getFeatureId()).isEqualTo(featureType);
            }
        }
    }

    @Test
    @DbUnitDataSet(
            after = "OnboardingUtilsFunctionalTest.after.csv"
    )
    void tryToEnableFeatures() {
        onboardingService.tryToEnableFeatures(partnerIds, featureTypes);
    }
}
