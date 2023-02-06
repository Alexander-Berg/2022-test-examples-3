package ru.yandex.direct.core.entity.adgroup.repository.internal;

import java.util.Arrays;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupBsTags;
import ru.yandex.direct.core.entity.adgroup.model.PageGroupTagEnum;
import ru.yandex.direct.core.entity.adgroup.model.TargetTagEnum;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.repository.TestAdGroupBsTagsRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singletonList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupBsTagsRepositoryTest {
    @Autowired
    private TestAdGroupBsTagsRepository testAdGroupBsTagsRepository;

    @Autowired
    private AdGroupBsTagsRepository adGroupPageTagsRepository;

    @Autowired
    private Steps steps;

    @Test
    public void testAddSingleRecord() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        AdGroupBsTags adGroupBsTags = new AdGroupBsTags()
                .withPageGroupTags(ImmutableList.of(PageGroupTagEnum.APP_METRO_TAG, PageGroupTagEnum.FRONTPAGE_TAG))
                .withTargetTags(ImmutableList.of(TargetTagEnum.FRONTPAGE_TAG));
        testRecordsInsertedCorrectly(adGroupInfo, adGroupBsTags, "[\"app-metro\", \"portal-trusted\"]",
                "[\"portal-trusted\"]");
    }

    @Test
    public void testAddSingleRecordOnlyPageTags() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        AdGroupBsTags adGroupBsTags = new AdGroupBsTags()
                .withPageGroupTags(ImmutableList.of(PageGroupTagEnum.FRONTPAGE_TAG))
                .withTargetTags(null);
        testRecordsInsertedCorrectly(adGroupInfo, adGroupBsTags, "[\"portal-trusted\"]", null);
    }

    @Test
    public void testAddSingleRecordOnlyTargetTags() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        AdGroupBsTags adGroupBsTags = new AdGroupBsTags()
                .withPageGroupTags(null)
                .withTargetTags(ImmutableList.of(TargetTagEnum.CONTENT_PROMOTION_VIDEO_TAG));
        testRecordsInsertedCorrectly(adGroupInfo, adGroupBsTags, null, "[\"content-promotion-video\"]");
    }

    @Test
    public void testUpdateRecordSoftlyOldValuePreserved() {
        AdGroupInfo adGroupInfoFirst = steps.adGroupSteps().createDefaultAdGroup();
        AdGroupInfo adGroupInfoSecond = steps.adGroupSteps().createDefaultAdGroup();
        AdGroupBsTags adGroupBsTags = new AdGroupBsTags()
                .withPageGroupTags(ImmutableList.of(PageGroupTagEnum.APP_METRO_TAG, PageGroupTagEnum.FRONTPAGE_TAG))
                .withTargetTags(ImmutableList.of(TargetTagEnum.FRONTPAGE_TAG));
        adGroupPageTagsRepository.addDefaultTagsForAdGroupList(adGroupInfoFirst.getClientId(),
                Arrays.asList(adGroupInfoFirst.getAdGroupId(), adGroupInfoSecond.getAdGroupId()), adGroupBsTags);
        AdGroupBsTags adGroupBsTagsFirst = new AdGroupBsTags()
                .withPageGroupTags(ImmutableList.of(PageGroupTagEnum.APP_METRO_TAG))
                .withTargetTags(ImmutableList.of(TargetTagEnum.FRONTPAGE_TAG));
        AdGroupBsTags adGroupBsTagsSecond = new AdGroupBsTags()
                .withPageGroupTags(ImmutableList.of(PageGroupTagEnum.APP_METRO_TAG, PageGroupTagEnum.FRONTPAGE_TAG))
                .withTargetTags(ImmutableList.of(TargetTagEnum.CONTENT_PROMOTION_VIDEO_TAG));
        adGroupPageTagsRepository.addAdGroupPageTags(adGroupInfoFirst.getShard(),
                ImmutableMap.of(adGroupInfoFirst.getAdGroupId(), adGroupBsTagsFirst,
                        adGroupInfoSecond.getAdGroupId(), adGroupBsTagsSecond));
        SoftAssertions.assertSoftly(soft -> {
            testAdGroupBsTagsRepository.softAssertionCheckAdGroupTagsInDbRawConsumer(
                    soft,
                    adGroupInfoFirst.getShard(),
                    adGroupInfoFirst.getAdGroupId(),
                    "[\"app-metro\", \"portal-trusted\"]", "[\"portal-trusted\"]");
            testAdGroupBsTagsRepository.softAssertionCheckAdGroupTagsInDbRawConsumer(
                    soft,
                    adGroupInfoSecond.getShard(),
                    adGroupInfoSecond.getAdGroupId(),
                    "[\"app-metro\", \"portal-trusted\"]", "[\"portal-trusted\"]");
        });
    }

    @Test
    public void testUpdateRecordSoftlyNewValueAssigned() {
        AdGroupInfo adGroupInfoFirst = steps.adGroupSteps().createDefaultAdGroup();
        AdGroupInfo adGroupInfoSecond = steps.adGroupSteps().createDefaultAdGroup();
        AdGroupBsTags adGroupBsTags = new AdGroupBsTags()
                .withPageGroupTags(ImmutableList.of(PageGroupTagEnum.APP_METRO_TAG, PageGroupTagEnum.FRONTPAGE_TAG))
                .withTargetTags(ImmutableList.of(TargetTagEnum.FRONTPAGE_TAG));
        adGroupPageTagsRepository.addDefaultTagsForAdGroupList(adGroupInfoFirst.getClientId(),
                Arrays.asList(adGroupInfoFirst.getAdGroupId(), adGroupInfoSecond.getAdGroupId()), adGroupBsTags);
        AdGroupBsTags adGroupBsTagsFirst = new AdGroupBsTags()
                .withPageGroupTags(ImmutableList.of(PageGroupTagEnum.APP_METRO_TAG))
                .withTargetTags(ImmutableList.of(TargetTagEnum.FRONTPAGE_TAG));
        AdGroupBsTags adGroupBsTagsSecond = new AdGroupBsTags()
                .withPageGroupTags(ImmutableList.of(PageGroupTagEnum.CONTENT_PROMOTION_VIDEO_TAG))
                .withTargetTags(ImmutableList.of(TargetTagEnum.APP_METRO_TAG));
        adGroupPageTagsRepository.addAdGroupPageTagsForce(adGroupInfoFirst.getShard(),
                ImmutableMap.of(adGroupInfoFirst.getAdGroupId(), adGroupBsTagsFirst,
                        adGroupInfoSecond.getAdGroupId(), adGroupBsTagsSecond));

        SoftAssertions.assertSoftly(soft -> {
            testAdGroupBsTagsRepository.softAssertionCheckAdGroupTagsInDbRawConsumer(
                    soft,
                    adGroupInfoFirst.getShard(),
                    adGroupInfoFirst.getAdGroupId(),
                    "[\"app-metro\"]", "[\"portal-trusted\"]");
            testAdGroupBsTagsRepository.softAssertionCheckAdGroupTagsInDbRawConsumer(
                    soft,
                    adGroupInfoSecond.getShard(),
                    adGroupInfoSecond.getAdGroupId(),
                    "[\"content-promotion-video\"]", "[\"app-metro\"]");
        });
    }

    private void testRecordsInsertedCorrectly(AdGroupInfo adGroupInfo,
                                              AdGroupBsTags adGroupBsTags,
                                              String expectedPageGroupTagsValue,
                                              String expectedTargetTagsValue) {
        adGroupPageTagsRepository.addDefaultTagsForAdGroupList(adGroupInfo.getClientId(),
                singletonList(adGroupInfo.getAdGroupId()), adGroupBsTags);
        SoftAssertions.assertSoftly(soft ->
                testAdGroupBsTagsRepository.softAssertionCheckAdGroupTagsInDbRawConsumer(
                        soft,
                        adGroupInfo.getShard(), adGroupInfo
                                .getAdGroupId(),
                        expectedPageGroupTagsValue, expectedTargetTagsValue)
        );
    }
}
