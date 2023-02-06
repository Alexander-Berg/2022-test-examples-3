package ru.yandex.direct.grid.core.entity.banner.repository;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction;
import ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute;
import ru.yandex.direct.core.testing.data.TestNewMobileAppBanners;
import ru.yandex.direct.core.testing.info.NewMobileAppBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerMobileContentInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute.ICON;
import static ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute.PRICE;
import static ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute.RATING;
import static ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute.RATING_VOTES;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridCoreTest
@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class GridBannerMobileContentRepositoryTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;
    @Autowired
    private GridBannerMobileContentRepository gridBannerMobileContentRepository;

    public static Object[] actionParameters() {
        return NewMobileContentPrimaryAction.values();
    }

    @Test
    @TestCaseName("[{index}]: действие на баннере {0}")
    @Parameters(method = "actionParameters")
    public void getMobileContentInfoByBannerId_WithDifferentActions(NewMobileContentPrimaryAction primaryAction) {
        MobileAppBanner banner = TestNewMobileAppBanners.fullMobileBanner()
                .withPrimaryAction(primaryAction);
        NewMobileAppBannerInfo mobileAppBannerInfo =
                steps.mobileAppBannerSteps().createMobileAppBanner(new NewMobileAppBannerInfo()
                        .withBanner(banner));

        Map<Long, GdiBannerMobileContentInfo> bannerIdToMobileContentInfo =
                gridBannerMobileContentRepository.getMobileContentInfoByBannerId(mobileAppBannerInfo.getShard(),
                        Set.of(mobileAppBannerInfo.getBannerId()));

        Map<Long, GdiBannerMobileContentInfo> expectedBannerIdToMobileContentInfo =
                Map.of(mobileAppBannerInfo.getBannerId(), new GdiBannerMobileContentInfo()
                        .withPrimaryAction(NewMobileContentPrimaryAction.valueOf(primaryAction.name())));

        assertThat(bannerIdToMobileContentInfo).as("дополнительные атрибуты баннера мобильного контента")
                .is(matchedBy(beanDiffer(expectedBannerIdToMobileContentInfo).useCompareStrategy(onlyExpectedFields())));
    }

    public static Object[][] featureParameters() {
        return new Object[][]{
                {"[ICON, RATING_VOTES, PRICE, RATING]",
                        Map.of(ICON, true, RATING_VOTES, true, PRICE, true, RATING, true),
                        Set.of(NewReflectedAttribute.ICON, NewReflectedAttribute.RATING_VOTES,
                                NewReflectedAttribute.PRICE, NewReflectedAttribute.RATING)},
                {"[ICON]",
                        Map.of(ICON, true, RATING_VOTES, false, PRICE, false, RATING, false),
                        Set.of(NewReflectedAttribute.ICON)},
                {"[RATING_VOTES]",
                        Map.of(ICON, false, RATING_VOTES, true, PRICE, false, RATING, false),
                        Set.of(NewReflectedAttribute.RATING_VOTES)},
                {"[PRICE]",
                        Map.of(ICON, false, RATING_VOTES, false, PRICE, true, RATING, false),
                        Set.of(NewReflectedAttribute.PRICE)},
                {"[RATING]",
                        Map.of(ICON, false, RATING_VOTES, false, PRICE, false, RATING, true),
                        Set.of(NewReflectedAttribute.RATING)},
                {"[]",
                        Map.of(ICON, false, RATING_VOTES, false, PRICE, false, RATING, false),
                        Collections.emptySet()},
        };
    }

    @Test
    @TestCaseName("[{index}]: разрешенные к показу атрибуты {0}")
    @Parameters(method = "featureParameters")
    public void getMobileContentInfoByBannerId_WithDifferentFeatures(@SuppressWarnings("unused") String description,
                                                                     Map<NewReflectedAttribute, Boolean> reflectedAttributes,
                                                                     Set<NewReflectedAttribute> expectedReflectedAttributes) {
        MobileAppBanner banner = TestNewMobileAppBanners.fullMobileBanner()
                .withReflectedAttributes(reflectedAttributes);
        NewMobileAppBannerInfo mobileAppBannerInfo =
                steps.mobileAppBannerSteps().createMobileAppBanner(new NewMobileAppBannerInfo()
                        .withBanner(banner));

        Map<Long, GdiBannerMobileContentInfo> bannerIdToMobileContentInfo =
                gridBannerMobileContentRepository.getMobileContentInfoByBannerId(mobileAppBannerInfo.getShard(),
                        Set.of(mobileAppBannerInfo.getBannerId()));

        Map<Long, GdiBannerMobileContentInfo> expectedBannerIdToMobileContentInfo
                = Map.of(mobileAppBannerInfo.getBannerId(), new GdiBannerMobileContentInfo()
                .withReflectedAttrs(expectedReflectedAttributes));

        assertThat(bannerIdToMobileContentInfo).as("дополнительные атрибуты баннера мобильного контента")
                .is(matchedBy(beanDiffer(expectedBannerIdToMobileContentInfo).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void getMobileContentInfoByBannerId_WithImpressionUrl() {
        MobileAppBanner banner = TestNewMobileAppBanners.fullMobileBanner()
                .withImpressionUrl("someImpressionUrl");
        NewMobileAppBannerInfo mobileAppBannerInfo =
                steps.mobileAppBannerSteps().createMobileAppBanner(new NewMobileAppBannerInfo()
                        .withBanner(banner));

        Map<Long, GdiBannerMobileContentInfo> bannerIdToMobileContentInfo =
                gridBannerMobileContentRepository.getMobileContentInfoByBannerId(mobileAppBannerInfo.getShard(),
                        Set.of(mobileAppBannerInfo.getBannerId()));

        Map<Long, GdiBannerMobileContentInfo> expectedBannerIdToMobileContentInfo =
                Map.of(mobileAppBannerInfo.getBannerId(), new GdiBannerMobileContentInfo()
                        .withImpressionUrl("someImpressionUrl"));

        assertThat(bannerIdToMobileContentInfo).as("трекерная ссылка для учёта показа баннера мобильного контента")
                .is(matchedBy(beanDiffer(expectedBannerIdToMobileContentInfo).useCompareStrategy(onlyExpectedFields())));
    }
}
