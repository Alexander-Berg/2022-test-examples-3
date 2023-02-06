package ru.yandex.direct.api.v5.entity.ads.validation;

import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class AdsApiValidationSignalsTest {

    @Test
    @TestCaseName("Expected: {2}")
    @Parameters(method = "params")
    public void shouldDetermineDefectsProperly(BannerWithAdGroupId banner,
                                               Predicate<BannerWithAdGroupId> method, boolean expected) {
        assertThat(method.test(banner)).isEqualTo(expected);
    }

    Iterable<Object[]> params() {
        return asList(
                negative(AdsApiValidationSignals.bannerWithVideoExtensionIdNotSpecified(),
                        AdsApiValidationSignals::hasVideoExtensionIdSpecified),
                negative(AdsApiValidationSignals.bannerWithBannerTypeNotSpecified(),
                        AdsApiValidationSignals::hasBannerTypeSpecified),
                negative(AdsApiValidationSignals.bannerWithAmbiguousType(),
                        AdsApiValidationSignals::hasUnambiguousType),

                positive(AdsApiValidationSignals.bannerWithVideoExtensionIdNotSpecified(),
                        AdsApiValidationSignals::hasBannerTypeSpecified),
                positive(AdsApiValidationSignals.bannerWithVideoExtensionIdNotSpecified(),
                        AdsApiValidationSignals::hasUnambiguousType),
                positive(AdsApiValidationSignals.bannerWithBannerTypeNotSpecified(),
                        AdsApiValidationSignals::hasVideoExtensionIdSpecified),
                positive(AdsApiValidationSignals.bannerWithBannerTypeNotSpecified(),
                        AdsApiValidationSignals::hasUnambiguousType),
                positive(AdsApiValidationSignals.bannerWithAmbiguousType(),
                        AdsApiValidationSignals::hasBannerTypeSpecified),
                positive(AdsApiValidationSignals.bannerWithAmbiguousType(),
                        AdsApiValidationSignals::hasVideoExtensionIdSpecified)
        );
    }

    private static Object[] positive(BannerWithAdGroupId banner, Predicate<BannerWithAdGroupId> method) {
        return testCase(banner, method, true);
    }

    private static Object[] negative(BannerWithAdGroupId banner, Predicate<BannerWithAdGroupId> method) {
        return testCase(banner, method, false);
    }

    private static Object[] testCase(BannerWithAdGroupId banner, Predicate<BannerWithAdGroupId> method,
                                     boolean expected) {
        return new Object[]{banner, method, expected};
    }

}
