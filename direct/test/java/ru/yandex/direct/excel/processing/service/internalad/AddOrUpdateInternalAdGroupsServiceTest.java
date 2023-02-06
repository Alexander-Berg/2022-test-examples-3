package ru.yandex.direct.excel.processing.service.internalad;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupAddOrUpdateItem;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupOperationContainer;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupOperationContainer.RequestSource;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.internal.ComplexInternalAdGroupServiceTestHelper;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.old.OldInternalBanner;
import ru.yandex.direct.core.entity.banner.model.old.TemplateVariable;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsProductOption;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingConditionBase;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.excel.processing.configuration.ExcelProcessingTest;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.duplicatedGoal;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validInternalNetworkTargeting;
import static ru.yandex.direct.core.testing.data.TestBanners.activeInternalBanner;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.anyValidationErrorOnPath;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ExcelProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddOrUpdateInternalAdGroupsServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private ComplexInternalAdGroupServiceTestHelper complexInternalAdGroupServiceTestHelper;

    @Autowired
    private AddOrUpdateInternalAdGroupsService service;

    private CampaignInfo campaign;
    private ClientInfo clientInfo;
    private UidAndClientId uidAndClientId;
    private InternalAdGroup existentAdGroup1;
    private AdGroupInfo adGroupInfo1;

    @Before
    public void before() {
        clientInfo =
                steps.internalAdProductSteps().createDefaultInternalAdProductWithOptions(Set.of(InternalAdsProductOption.SOFTWARE));
        campaign = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo);
        uidAndClientId = UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId());

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo);
        adGroupInfo1 = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        existentAdGroup1 = (InternalAdGroup) adGroupInfo1.getAdGroup();
    }

    @Test
    public void empty_AddAndUpdate() {
        MassResult<Long> result = addOrUpdate(emptyList());

        assertThat(result, isFullySuccessful());
    }

    @Test
    public void oneValidUpdate_AddAndUpdate() {
        var validInternalNetworkTargeting = validInternalNetworkTargeting();

        var itemToUpdate = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(existentAdGroup1
                        .withName(existentAdGroup1.getName() + "123"))
                .withAdditionalTargetings(List.of(validInternalNetworkTargeting));

        MassResult<Long> result = addOrUpdate(Collections.singletonList(itemToUpdate));

        assertThat(result, isFullySuccessful());
        checkInternalAdGroupAndTargetingInDb(
                existentAdGroup1.withStatusBsSynced(StatusBsSynced.NO), List.of(validInternalNetworkTargeting));
    }

    @Test
    public void oneValidAdd_AddAndUpdate() {
        var validInternalNetworkTargeting = validInternalNetworkTargeting();

        var addItem = (InternalAdGroup) steps.adGroupSteps().createActiveInternalAdGroup(clientInfo).getAdGroup();

        var itemToAdd = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(addItem.withId(null))
                .withAdditionalTargetings(List.of(validInternalNetworkTargeting));

        MassResult<Long> result = addOrUpdate(Collections.singletonList(itemToAdd));

        assertThat(result, isFullySuccessful());
        checkInternalAdGroupAndTargetingInDb(
                addItem.withStatusBsSynced(StatusBsSynced.NO), List.of(validInternalNetworkTargeting));
    }

    @Test
    public void twoValidOneTargeting_AddAndUpdate() {
        var itemToUpdate = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(existentAdGroup1
                        .withName(existentAdGroup1.getName() + "123"))
                .withAdditionalTargetings(Collections.emptyList());
        var addItem = (InternalAdGroup) steps.adGroupSteps().createActiveInternalAdGroup(clientInfo).getAdGroup();

        var validInternalNetworkTargeting = validInternalNetworkTargeting();
        var itemToAdd = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(addItem.withId(null))
                .withAdditionalTargetings(List.of(validInternalNetworkTargeting));

        MassResult<Long> result = addOrUpdate(asList(itemToUpdate, itemToAdd));

        assertThat(result, isFullySuccessful());
    }

    @Test
    public void retargetingCondition_validationOnly() {
        var itemToUpdate = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(existentAdGroup1
                        .withName(existentAdGroup1.getName() + "123"))
                .withAdditionalTargetings(Collections.emptyList())
                .withRetargetingCondition(new RetargetingConditionBase()
                        .withType(ConditionType.ab_segments)
                        .withRules(List.of(
                                new Rule().withType(RuleType.ALL).withGoals(List.of((Goal) new Goal().withId(123L))),
                                new Rule().withType(RuleType.NOT).withGoals(List.of((Goal) new Goal().withId(123L))))));
        var addItem = (InternalAdGroup) steps.adGroupSteps().createActiveInternalAdGroup(clientInfo).getAdGroup();

        var validInternalNetworkTargeting = validInternalNetworkTargeting();
        var itemToAdd = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(addItem.withId(null))
                .withAdditionalTargetings(List.of(validInternalNetworkTargeting))
                .withRetargetingCondition(new RetargetingConditionBase()
                        .withType(ConditionType.interests)
                        .withRules(List.of(
                                new Rule().withType(RuleType.ALL).withGoals(List.of((Goal) new Goal().withId(321L))),
                                new Rule().withType(RuleType.OR).withGoals(List.of((Goal) new Goal().withId(321L))))));

        MassResult<Long> result = addOrUpdateOnlyValidation(asList(itemToUpdate, itemToAdd));

        assertThat(result, isSuccessful(false, false));
        assertThat(result.get(0).getValidationResult(),
                hasDefectWithDefinition(validationError(path(field("retargetingConditions"), index(0)),
                        duplicatedGoal())));
        assertThat(result.get(1).getValidationResult(),
                hasDefectWithDefinition(validationError(path(field("retargetingConditions"), index(0)),
                        duplicatedGoal())));
    }

    @Test
    public void oneValidOneInvalid_AddAndUpdateOnlyValidation() {
        var validInternalNetworkTargeting = validInternalNetworkTargeting();

        var addItem1 = (InternalAdGroup) steps.adGroupSteps().createActiveInternalAdGroup(clientInfo).getAdGroup();
        var itemToAdd1 = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(addItem1.withId(null))
                .withAdditionalTargetings(List.of(validInternalNetworkTargeting));
        var addItem2 = (InternalAdGroup) steps.adGroupSteps().createActiveInternalAdGroup(clientInfo).getAdGroup();
        var itemToAdd2 = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(addItem2.withRf(-999))
                .withAdditionalTargetings(Collections.emptyList());

        MassResult<Long> result = addOrUpdateOnlyValidation(asList(itemToAdd1, itemToAdd2));

        assertThat(result, isSuccessful(true, false));
        assertThat(result.get(1).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("adGroup"), field("rf")))));
    }

    @Test
    public void twoValid_AddAndUpdate() {
        List<AdGroupAdditionalTargeting> additionalTargetingsToUpdate = List.of(validInternalNetworkTargeting());
        var itemToUpdate = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(existentAdGroup1
                        .withName(existentAdGroup1.getName() + "123"))
                .withAdditionalTargetings(additionalTargetingsToUpdate);
        var addItem = (InternalAdGroup) steps.adGroupSteps().createActiveInternalAdGroup(clientInfo).getAdGroup();

        List<AdGroupAdditionalTargeting> additionalTargetingsToAdd = List.of(validInternalNetworkTargeting());
        var itemToAdd = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(addItem.withId(null))
                .withAdditionalTargetings(additionalTargetingsToAdd);

        MassResult<Long> result = addOrUpdate(asList(itemToUpdate, itemToAdd));

        assertThat(result, isFullySuccessful());

        checkInternalAdGroupAndTargetingInDb(
                addItem.withStatusBsSynced(StatusBsSynced.NO), additionalTargetingsToAdd);
        checkInternalAdGroupAndTargetingInDb(
                existentAdGroup1.withStatusBsSynced(StatusBsSynced.NO), additionalTargetingsToUpdate);
    }

    @Test
    public void oneInvalid_requiredAgeVariable_update() {
        OldInternalBanner internalBanner = activeInternalBanner(adGroupInfo1.getCampaignId(),
                adGroupInfo1.getAdGroupId())
                .withTemplateId(TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_WITH_AGE_VARIABLE)
                .withTemplateVariables(List.of(new TemplateVariable()
                        .withTemplateResourceId(TemplateResourceRepositoryMockUtils.TEMPLATE_6_RESOURCE_AGE)
                        .withInternalValue(null)))
                .withStatusActive(false)
                .withLanguage(Language.RU_);
        OldInternalBanner normalInternalBanner = activeInternalBanner(adGroupInfo1.getCampaignId(),
                adGroupInfo1.getAdGroupId())
                .withTemplateId(TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_WITH_AGE_VARIABLE)
                .withTemplateVariables(List.of(new TemplateVariable()
                        .withTemplateResourceId(TemplateResourceRepositoryMockUtils.TEMPLATE_6_RESOURCE_AGE)
                        .withInternalValue("123")))
                .withStatusActive(false)
                .withLanguage(Language.RU_);
        steps.bannerSteps().createActiveInternalBanner(adGroupInfo1, internalBanner);
        steps.bannerSteps().createActiveInternalBanner(adGroupInfo1, normalInternalBanner);
        List<AdGroupAdditionalTargeting> additionalTargetingsToUpdate = List.of(validInternalNetworkTargeting());
        var itemToUpdate = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(existentAdGroup1
                        .withGeo(List.of(225L))
                        .withName(existentAdGroup1.getName() + "123"))
                .withAdditionalTargetings(additionalTargetingsToUpdate);

        MassResult<Long> result = addOrUpdate(Collections.singletonList(itemToUpdate));

        assertThat(result, isSuccessful(false));
        assertThat(result.get(0).getValidationResult(),
                hasDefectWithDefinition(validationError(path(field("adGroup"), field("geo")),
                        AdGroupDefects.requiredAgeVariable(List.of(internalBanner.getId())))));
    }

    @Test
    public void oneValidTwoInvalid_AddAndUpdate() {
        String oldAdGroupName = existentAdGroup1.getName();
        Integer oldRf = existentAdGroup1.getRf();
        var itemToUpdate = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(existentAdGroup1.withRf(-999).withName(oldAdGroupName + "123"))
                .withAdditionalTargetings(List.of(validInternalNetworkTargeting()));

        var addItem1 = (InternalAdGroup) steps.adGroupSteps().createActiveInternalAdGroup(clientInfo).getAdGroup();
        var addItem2 = (InternalAdGroup) steps.adGroupSteps().createActiveInternalAdGroup(clientInfo).getAdGroup();
        var itemToAdd1 = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(addItem1.withId(null))
                .withAdditionalTargetings(List.of(validInternalNetworkTargeting()));
        var itemToAdd2 = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(addItem2.withId(null).withMinusKeywords(List.of("$$$$$$$$******!@!@!@!")))
                .withAdditionalTargetings(Collections.emptyList());

        MassResult<Long> result = addOrUpdate(asList(itemToAdd1, itemToUpdate, itemToAdd2));

        assertThat(result, isSuccessful(true, false, false));
        assertThat(result.get(1).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("adGroup"), field("rf")))));
        assertThat(result.get(2).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("adGroup"), field("minusKeywords")))));
        checkInternalAdGroupAndTargetingInDb(existentAdGroup1.withName(oldAdGroupName).withRf(oldRf), emptyList());
    }

    @Test
    public void twoValidOneInvalid_AddAndUpdateOnlyValidation() {
        var oldName = existentAdGroup1.getName();
        var itemToUpdate = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(existentAdGroup1.withName(oldName + "123"))
                .withAdditionalTargetings(List.of(validInternalNetworkTargeting()));

        var addItem1 = (InternalAdGroup) steps.adGroupSteps().createActiveInternalAdGroup(clientInfo).getAdGroup();
        var itemToAdd1 = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(addItem1.withId(null))
                .withAdditionalTargetings(List.of(validInternalNetworkTargeting()));
        var addItem2 = (InternalAdGroup) steps.adGroupSteps().createActiveInternalAdGroup(clientInfo).getAdGroup();
        var itemToAdd2 = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(addItem2.withRf(-999))
                .withAdditionalTargetings(Collections.emptyList());

        MassResult<Long> result = addOrUpdateOnlyValidation(asList(itemToAdd1, itemToUpdate, itemToAdd2));

        assertThat(result, isSuccessful(true, true, false));
        assertThat(result.get(2).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("adGroup"), field("rf")))));
        checkInternalAdGroupAndTargetingInDb(existentAdGroup1.withName(oldName), emptyList());
    }


    private MassResult<Long> addOrUpdate(List<InternalAdGroupAddOrUpdateItem> addItems) {
        var operationContainer = new InternalAdGroupOperationContainer(
                Applicability.FULL, clientInfo.getUid(), uidAndClientId, true,
                RequestSource.EXCEL
        );
        return service.addOrUpdateInternalAdGroups(addItems, operationContainer, false);
    }

    private MassResult<Long> addOrUpdateOnlyValidation(List<InternalAdGroupAddOrUpdateItem> addItems) {
        var operationContainer = new InternalAdGroupOperationContainer(
                Applicability.FULL, clientInfo.getUid(), uidAndClientId, true,
                RequestSource.EXCEL
        );
        return service.addOrUpdateInternalAdGroups(addItems, operationContainer, true);
    }

    private void checkInternalAdGroupAndTargetingInDb(
            AdGroup adGroup, List<AdGroupAdditionalTargeting> targetings) {
        complexInternalAdGroupServiceTestHelper.checkInternalAdGroupAndTargetingInDb(
                campaign.getShard(), adGroup, targetings);
    }
}
