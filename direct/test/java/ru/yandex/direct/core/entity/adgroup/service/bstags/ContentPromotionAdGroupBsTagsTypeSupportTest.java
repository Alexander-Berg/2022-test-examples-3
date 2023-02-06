package ru.yandex.direct.core.entity.adgroup.service.bstags;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ContentPromotionAdGroupBsTagsTypeSupportTest extends AdGroupBsTagsTypeSupportTestBase {

    @Test
    public void videoType() {
        var adGroup = fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO);

        var actualSettings = getAdGroupBsTagsSettings(adGroup);
        var expectedSettings = new AdGroupBsTagsSettings.Builder()
                .withRequiredPageGroupTags(List.of("content-promotion-video"))
                .withRequiredTargetTags(List.of("content-promotion-video"))
                .build();

        assertThat(actualSettings).is(matchedBy(beanDiffer(expectedSettings)));
    }

    @Test
    public void collectionType() {
        var adGroup = fullContentPromotionAdGroup(ContentPromotionAdgroupType.COLLECTION);

        var actualSettings = getAdGroupBsTagsSettings(adGroup);
        var expectedSettings = new AdGroupBsTagsSettings.Builder()
                .withRequiredPageGroupTags(List.of("content-promotion-collection"))
                .withRequiredTargetTags(List.of("content-promotion-collection"))
                .build();

        assertThat(actualSettings).is(matchedBy(beanDiffer(expectedSettings)));
    }

    @Test
    public void serviceType() {
        var adGroup = fullContentPromotionAdGroup(ContentPromotionAdgroupType.SERVICE);

        var actualSettings = getAdGroupBsTagsSettings(adGroup);
        var expectedSettings = new AdGroupBsTagsSettings.Builder()
                .withRequiredPageGroupTags(List.of("yndx-services"))
                .withRequiredTargetTags(List.of("yndx-services"))
                .build();

        assertThat(actualSettings).is(matchedBy(beanDiffer(expectedSettings)));
    }
}
