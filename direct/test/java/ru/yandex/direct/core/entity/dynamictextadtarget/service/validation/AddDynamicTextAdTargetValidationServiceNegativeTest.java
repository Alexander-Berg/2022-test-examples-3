package ru.yandex.direct.core.entity.dynamictextadtarget.service.validation;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.container.AffectedCampaignIdsContainer;
import ru.yandex.direct.core.entity.campaign.repository.CampaignAccessCheckRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleKind;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleType;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.entity.dynamictextadtarget.service.DynamicTextAdTargetService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.DynamicTextAdTargetInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.DynamicTextAdTargetSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetConstants.MAX_ARGUMENTS;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetConstants.MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetConstants.MAX_NAME_LENGTH;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetConstants.MIN_ARGUMENTS;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetConstants.WEBPAGE_RULE_AVAILABLE_ARGUMENT_LENGTH;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.adGroupNotFound;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.exceededMaxLengthInArguments;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.exceededMaxLengthInName;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.invalidEmptyNameFormat;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.invalidEmptyUrlFormat;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.invalidLettersInName;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.invalidLettersInRule;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.invalidUrlFormat;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.maxDynamicTextAdTargetsInAdGroup;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.numberArgumentsMustBeFromTo;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTarget;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTargetWithRandomRules;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedObject;
import static ru.yandex.direct.validation.defect.CommonDefects.inconsistentStateAlreadyExists;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddDynamicTextAdTargetValidationServiceNegativeTest {

    private AddDynamicTextAdTargetValidationService addValidationService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignAccessCheckRepository campaignAccessCheckRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;

    @Autowired
    private DynamicTextAdTargetService dynamicTextAdTargetService;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private DynamicTextAdTargetSteps dynamicTextAdTargetSteps;

    @Autowired
    private RequestCampaignAccessibilityCheckerProvider requestAccessibleCampaignTypes;

    @Autowired
    private FeatureService featureService;

    private long operatorUid;
    private ClientId clientId;
    private int shard;
    private DynamicTextAdTarget dynamicTextAdTarget;
    private AdGroupInfo defaultAdGroup;

    @Before
    public void before() {
        defaultAdGroup = adGroupSteps.createActiveDynamicTextAdGroup();
        operatorUid = defaultAdGroup.getUid();
        clientId = defaultAdGroup.getClientId();
        dynamicTextAdTarget = defaultDynamicTextAdTarget(defaultAdGroup);
        shard = defaultAdGroup.getShard();
        addValidationService = new AddDynamicTextAdTargetValidationService(
                new CampaignSubObjectAccessCheckerFactory(
                        shardHelper, spy(rbacService), campaignAccessCheckRepository,
                        new AffectedCampaignIdsContainer(), requestAccessibleCampaignTypes, featureService), clientService,
                campaignRepository, adGroupRepository,
                dynamicTextAdTargetRepository);
    }

    private ValidationResult<List<DynamicTextAdTarget>, Defect> validate(List<DynamicTextAdTarget> models) {
        return addValidationService.validateAdd(shard, operatorUid, clientId, models);
    }

    @Test
    public void validate_AdGroupIdNotDynamicType() {
        AdGroupInfo cpmBannerAdGroup = adGroupSteps.createActiveCpmBannerAdGroup();
        ValidationResult<List<DynamicTextAdTarget>, Defect> actual =
                validate(singletonList(dynamicTextAdTarget.withAdGroupId(cpmBannerAdGroup.getAdGroupId())));
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), adGroupNotFound()))));

    }

    @Test
    public void validate_AdGroupIdNegative() {
        ValidationResult<List<DynamicTextAdTarget>, Defect> actual =
                validate(singletonList(dynamicTextAdTarget.withAdGroupId(-1L)));
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.AD_GROUP_ID.name())), validId()))));

    }

    @Test
    public void validate_DuplicatedObjects() {
        ValidationResult<List<DynamicTextAdTarget>, Defect> actual =
                validate(asList(
                        dynamicTextAdTarget,
                        dynamicTextAdTarget));
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), duplicatedObject()))));
    }

    @Test
    public void validate_DuplicateConditionInExisting() {
        DynamicTextAdTargetInfo dynamicTextAdTargetInfo =
                dynamicTextAdTargetSteps.createDefaultDynamicTextAdTarget(defaultAdGroup);
        ValidationResult<List<DynamicTextAdTarget>, Defect> actual =
                validate(singletonList(dynamicTextAdTargetInfo.getDynamicTextAdTarget().withId(null)));
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.CONDITION.name())),
                        inconsistentStateAlreadyExists()))));
    }

    @Test
    public void validate_ConditionNumberArguments() {
        ValidationResult<List<DynamicTextAdTarget>, Defect> actual =
                validate(singletonList(
                        dynamicTextAdTarget.withCondition(
                                singletonList(new WebpageRule()
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withType(WebpageRuleType.TITLE)
                                        .withValue(asList("title1", "title2", "title3", "title4", "title5", "title6",
                                                "title7", "title8", "title9", "title10", "title11"))
                                )
                        )));
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.CONDITION.name()), index(0),
                        field(WebpageRule.VALUE.name())),
                        numberArgumentsMustBeFromTo(1, MIN_ARGUMENTS, MAX_ARGUMENTS)))));
    }

    @Test
    public void validate_ConditionArgumentMaxLength() {
        ValidationResult<List<DynamicTextAdTarget>, Defect> actual =
                validate(singletonList(
                        dynamicTextAdTarget.withCondition(
                                singletonList(new WebpageRule()
                                        .withKind(WebpageRuleKind.EXACT)
                                        .withType(WebpageRuleType.TITLE)
                                        .withValue(singletonList(randomAlphabetic(101)))
                                )
                        )));
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.CONDITION.name()), index(0),
                        field(WebpageRule.VALUE.name()), index(0)), exceededMaxLengthInArguments(1, 1,
                        WEBPAGE_RULE_AVAILABLE_ARGUMENT_LENGTH.get(WebpageRuleType.TITLE))))));
    }

    @Test
    public void validate_ConditionArgumentValidHref() {
        ValidationResult<List<DynamicTextAdTarget>, Defect> actual =
                validate(singletonList(
                        dynamicTextAdTarget.withCondition(
                                singletonList(new WebpageRule()
                                        .withKind(WebpageRuleKind.EQUALS)
                                        .withType(WebpageRuleType.URL_PRODLIST)
                                        .withValue(singletonList(randomAlphabetic(10)))
                                )
                        )));
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.CONDITION.name()), index(0),
                        field(WebpageRule.VALUE.name()), index(0)), invalidUrlFormat(1, 1)))));
    }

    @Test
    public void validate_ConditionArgumentIsNotBlankUrl() {
        ValidationResult<List<DynamicTextAdTarget>, Defect> actual =
                validate(singletonList(
                        dynamicTextAdTarget.withCondition(
                                singletonList(new WebpageRule()
                                        .withKind(WebpageRuleKind.EQUALS)
                                        .withType(WebpageRuleType.URL_PRODLIST)
                                        .withValue(singletonList(" "))
                                )
                        )));
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.CONDITION.name()), index(0),
                        field(WebpageRule.VALUE.name()), index(0)), invalidEmptyUrlFormat(1, 1)))));
    }

    @Test
    public void validate_ConditionArgumentAllowedChars() {
        ValidationResult<List<DynamicTextAdTarget>, Defect> actual =
                validate(singletonList(
                        dynamicTextAdTarget.withAutobudgetPriority(null).withCondition(
                                singletonList(new WebpageRule()
                                        .withKind(WebpageRuleKind.EQUALS)
                                        .withType(WebpageRuleType.URL_PRODLIST)
                                        .withValue(singletonList("https://サムライ.com"))
                                )
                        )));
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.CONDITION.name()), index(0),
                        field(WebpageRule.VALUE.name()), index(0)), invalidLettersInRule(1, 1)))));
    }

    @Test
    public void validateName_MaxLength() {
        ValidationResult<List<DynamicTextAdTarget>, Defect> actual =
                validate(singletonList(dynamicTextAdTarget.withConditionName(randomAlphabetic(101))));
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.CONDITION_NAME.name())),
                        exceededMaxLengthInName(MAX_NAME_LENGTH)))));
    }

    @Test
    public void validate_NameIsNotBlank() {
        ValidationResult<List<DynamicTextAdTarget>, Defect> actual =
                validate(singletonList(dynamicTextAdTarget.withConditionName(" ")));
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.CONDITION_NAME.name())),
                        invalidEmptyNameFormat()))));
    }

    @Test
    public void validate_NameAllowedChars() {
        ValidationResult<List<DynamicTextAdTarget>, Defect> actual =
                validate(singletonList(dynamicTextAdTarget.withConditionName("サムライ")));
        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.CONDITION_NAME.name())),
                        invalidLettersInName()))));
    }

    @Test
    public void validate_addMoreThanMaxCountInEmptyGroup() {
        ArrayList<DynamicTextAdTarget> dynamicTextAdTargetsToAdd = new ArrayList<>();
        for (int i = 0; i < MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP + 1; i++) {
            dynamicTextAdTargetsToAdd.add(defaultDynamicTextAdTargetWithRandomRules(defaultAdGroup));
        }

        ValidationResult<List<DynamicTextAdTarget>, Defect> actual = validate(dynamicTextAdTargetsToAdd);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)),
                        maxDynamicTextAdTargetsInAdGroup(MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP)))));
    }

    @Test
    public void validate_addMoreThanMaxCountInNotEmptyGroup() {
        dynamicTextAdTargetService.addDynamicTextAdTargets(clientId, operatorUid,
                singletonList(defaultDynamicTextAdTargetWithRandomRules(defaultAdGroup)));

        ArrayList<DynamicTextAdTarget> dynamicTextAdTargetsToAdd = new ArrayList<>();
        for (int i = 0; i < MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP; i++) {
            dynamicTextAdTargetsToAdd.add(defaultDynamicTextAdTargetWithRandomRules(defaultAdGroup));
        }

        ValidationResult<List<DynamicTextAdTarget>, Defect> actual = validate(dynamicTextAdTargetsToAdd);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)),
                        maxDynamicTextAdTargetsInAdGroup(MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP)))));
    }
}
