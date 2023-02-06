package ru.yandex.direct.core.entity.adgroup.service.bstags;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoproductAdGroup;
import static ru.yandex.direct.feature.FeatureName.ZERO_SPEED_PAGE_ENABLED_FOR_GEOPRODUCT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CpmGeoproductAdGroupBsTagsTypeSupportTest extends AdGroupBsTagsTypeSupportTestBase {

    @Test
    public void withoutZeroSpeedPageFeature() {
        var adGroup = activeCpmGeoproductAdGroup(null);

        var actualConstraints = getAdGroupBsTagsSettings(adGroup);
        var expectedConstraints = new AdGroupBsTagsSettings.Builder()
                .withDefaultPageGroupTags(List.of("app-metro"))
                .withDefaultTargetTags(List.of("app-metro"))
                .build();

        assertThat(actualConstraints).is(matchedBy(beanDiffer(expectedConstraints)));
    }

    @Test
    public void withZeroSpeedPageFeature() {
        enableZeroSpeedPageEnabledForGeoproduct();
        var adGroup = activeCpmGeoproductAdGroup(null);

        var actualConstraints = getAdGroupBsTagsSettings(adGroup);
        var expectedConstraints = new AdGroupBsTagsSettings.Builder()
                .withDefaultPageGroupTags(List.of("app-metro"))
                .withAllowedPageGroupTags(List.of("app-navi"))
                .withDefaultTargetTags(List.of("app-metro"))
                .withAllowedTargetTags(List.of("app-navi"))
                .build();

        assertThat(actualConstraints).is(matchedBy(beanDiffer(expectedConstraints)));
    }

    private void enableZeroSpeedPageEnabledForGeoproduct() {
        steps.featureSteps().addClientFeature(client.getClientId(), ZERO_SPEED_PAGE_ENABLED_FOR_GEOPRODUCT, true);
    }
}
