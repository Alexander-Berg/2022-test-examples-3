package ru.yandex.direct.core.entity.adgroup.service.bstags;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.bsTagNotAllowed;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.feature.FeatureName.TARGET_TAGS_ALLOWED;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AllowedBsTagsValidatorTest {

    @Autowired
    private Steps steps;

    @Autowired
    private FeatureService featureService;

    private AdGroupBsTagsSettings adGroupBsTagsSettings;

    private ClientInfo client;

    private ClientInfo operator;

    @Before
    public void before() {
        adGroupBsTagsSettings = new AdGroupBsTagsSettings.Builder()
                .withRequiredPageGroupTags(asList("default_page_tag"))
                .withAllowedPageGroupTags(asList("allowed_page_tag"))
                .withRequiredTargetTags(asList("default_target_tag"))
                .withAllowedTargetTags(asList("allowed_target_tag"))
                .build();
        client = steps.clientSteps().createDefaultClient();
        operator = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void allowedTag() {
        var adGroup = defaultAdGroup()
                .withPageGroupTags(asList("allowed_page_tag"))
                .withTargetTags(asList("allowed_target_tag"));

        var result = validate(adGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void defaultTag() {
        var adGroup = defaultAdGroup()
                .withPageGroupTags(asList("default_page_tag"))
                .withTargetTags(asList("default_target_tag"));

        var result = validate(adGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void notAllowedTag() {
        var adGroup = defaultAdGroup()
                .withPageGroupTags(asList("non_existing_tag"))
                .withTargetTags(asList("non_existing_tag"));

        var result = validate(adGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(AdGroup.PAGE_GROUP_TAGS)), bsTagNotAllowed()))));
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(AdGroup.TARGET_TAGS)), bsTagNotAllowed()))));
    }

    @Test
    public void anyBsTagsAllowedForClientWithFeature() {
        enableTargetTagsAllowed(client);

        var adGroup = defaultAdGroup()
                .withPageGroupTags(asList("non_existing_tag"))
                .withTargetTags(asList("non_existing_tag"));

        var result = validate(adGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void anyBsTagsAllowedForOperatorWithFeature() {
        enableTargetTagsAllowed(operator);

        var adGroup = defaultAdGroup()
                .withPageGroupTags(asList("non_existing_tag"))
                .withTargetTags(asList("non_existing_tag"));

        var result = validate(adGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    private ValidationResult<AdGroup, Defect> validate(AdGroup adGroup) {
        var adGroupsBsTagsSettings = Map.of(adGroup, adGroupBsTagsSettings);
        return new AllowedBsTagsValidator(adGroupsBsTagsSettings, featureService, client.getClientId(),
                operator.getUid())
                .apply(adGroup);
    }

    private AdGroup defaultAdGroup() {
        var campaign = steps.campaignSteps().createActiveTextCampaign(client);
        return defaultTextAdGroup(campaign.getCampaignId());
    }

    private void enableTargetTagsAllowed(ClientInfo client) {
        steps.featureSteps().addClientFeature(client.getClientId(), TARGET_TAGS_ALLOWED, true);
    }
}
