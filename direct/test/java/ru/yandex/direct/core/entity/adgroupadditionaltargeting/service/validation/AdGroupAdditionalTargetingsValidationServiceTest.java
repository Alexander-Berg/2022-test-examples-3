package ru.yandex.direct.core.entity.adgroupadditionaltargeting.service.validation;

import java.util.List;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InternalNetworkAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexUidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupAdditionalTargetingsValidationServiceTest {
    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupAdditionalTargetingsValidationService adGroupAdditionalTargetingsValidationService;

    private AdGroupInfo adGroupInfo1;
    private AdGroupInfo adGroupInfo2;

    @Before
    public void setUp() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalFreeCampaign();
        adGroupInfo1 = steps.adGroupSteps().createDefaultInternalAdGroup().withCampaignInfo(campaignInfo);
        adGroupInfo2 = steps.adGroupSteps().createDefaultInternalAdGroup().withCampaignInfo(campaignInfo);
    }

    @Test
    public void createTargetingWithoutAdGroup() {
        InternalNetworkAdGroupAdditionalTargeting targeting = new InternalNetworkAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        ValidationResult<List<AdGroupAdditionalTargeting>, Defect> actual =
                adGroupAdditionalTargetingsValidationService.validate(singletonList(targeting), false);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroupAdditionalTargeting.AD_GROUP_ID)), notNull()))));
    }

    @Test
    public void createTargetingWithoutTargetingMode() {
        InternalNetworkAdGroupAdditionalTargeting targeting = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        ValidationResult<List<AdGroupAdditionalTargeting>, Defect> actual =
                adGroupAdditionalTargetingsValidationService.validate(singletonList(targeting), false);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroupAdditionalTargeting.TARGETING_MODE)), notNull()))));
    }

    @Test
    public void createTargetingWithoutJoinType() {
        InternalNetworkAdGroupAdditionalTargeting targeting = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING);

        ValidationResult<List<AdGroupAdditionalTargeting>, Defect> actual =
                adGroupAdditionalTargetingsValidationService.validate(singletonList(targeting), false);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroupAdditionalTargeting.JOIN_TYPE)), notNull()))));
    }

    @Test
    public void createTargetingWithoutInvalidAdGroupId() {
        InternalNetworkAdGroupAdditionalTargeting targeting = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(-adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        ValidationResult<List<AdGroupAdditionalTargeting>, Defect> actual =
                adGroupAdditionalTargetingsValidationService.validate(singletonList(targeting), false);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroupAdditionalTargeting.AD_GROUP_ID)), validId()))));
    }

    @Test
    public void createMultipleInvalidTargetings() {
        InternalNetworkAdGroupAdditionalTargeting targeting1 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        YandexUidsAdGroupAdditionalTargeting targeting2 = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo2.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withValue(asList("8021110101545123184", "5482934511545124432"));

        ValidationResult<List<AdGroupAdditionalTargeting>, Defect> actual =
                adGroupAdditionalTargetingsValidationService.validate(asList(targeting1, targeting2), false);

        AssertionsForClassTypes.assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroupAdditionalTargeting.TARGETING_MODE)), notNull()))));

        AssertionsForClassTypes.assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(1), field(AdGroupAdditionalTargeting.JOIN_TYPE)), notNull()))));
    }

    @Test
    public void createSameTargeting_ForDifferentAdGroups() {
        InternalNetworkAdGroupAdditionalTargeting targeting1 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        InternalNetworkAdGroupAdditionalTargeting targeting2 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo2.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        ValidationResult<List<AdGroupAdditionalTargeting>, Defect> actual =
                adGroupAdditionalTargetingsValidationService.validate(asList(targeting1, targeting2), false);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void createTargetingWithDifferentMode_ForDifferentGroups() {
        InternalNetworkAdGroupAdditionalTargeting targeting1 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        InternalNetworkAdGroupAdditionalTargeting targeting2 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo2.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

        ValidationResult<List<AdGroupAdditionalTargeting>, Defect> actual =
                adGroupAdditionalTargetingsValidationService.validate(asList(targeting1, targeting2), false);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }
}
