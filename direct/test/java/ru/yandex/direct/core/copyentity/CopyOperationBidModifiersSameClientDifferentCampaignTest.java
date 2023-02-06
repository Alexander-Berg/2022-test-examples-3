package ru.yandex.direct.core.copyentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.container.UntypedBidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBidModifiers;
import ru.yandex.direct.core.entity.campaign.service.BaseCampaignService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupBidModifierInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignBidModifierInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.qatools.allure.annotations.Description;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertFalse;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверяет копирование модификаторов ставок")
public class CopyOperationBidModifiersSameClientDifferentCampaignTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private CopyOperationFactory factory;

    @Autowired
    private Steps steps;

    @Autowired
    private CopyOperationAssert asserts;

    @Autowired
    private BaseCampaignService baseCampaignService;

    private static final RecursiveComparisonConfiguration BID_MODIFIERS_COMPARE_STRATEGY =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreAllExpectedNullFields(true)
                    .withIgnoredFields("id", "campaignId", "adGroupId", "lastChange")
                    .withIgnoredFieldsMatchingRegexes("^.*\\.id$", "^.*\\.lastChange$")
                    .build();

    private Long uid;
    private ClientId clientId;
    private ClientInfo clientInfo;

    private List<Long> retargetingConditionsIds;
    private List<Long> campaignRetargetingConditionsIds;

    @Before
    public void setUp() {
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superClientInfo.getUid();

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        steps.featureSteps().setCurrentClient(clientId);

        long retCondId1 = retConditionSteps.createBigRetCondition(clientInfo).getRetConditionId();
        long retCondId2 = retConditionSteps.createDefaultRetCondition(clientInfo).getRetConditionId();

        retargetingConditionsIds = List.of(retCondId1, retCondId2);

        retCondId1 = retConditionSteps.createBigRetCondition(clientInfo).getRetConditionId();
        retCondId2 = retConditionSteps.createDefaultRetCondition(clientInfo).getRetConditionId();

        campaignRetargetingConditionsIds = List.of(retCondId1, retCondId2);

        asserts.init(clientId, clientId, uid);
    }

    @Test
    @Description("Копирование группы с включенными модификаторами ставок")
    public void copyAdGroupWithEnabledBidModifiers() {
        AdGroupInfo adGroupInfo = createTextCampaignWithAdGroup();
        List<BidModifier> originalBidModifiersIds = createAdGroupBidModifiers(adGroupInfo);
        CopyResult copyResult = copyAdGroup(adGroupInfo);
        compareAdGroupBidModifiers(copyResult, originalBidModifiersIds);
    }

    @Test
    @Description("Копирование группы с выключенными модификаторами ставок")
    public void copyAdGroupWithDisabledBidModifiers() {
        AdGroupInfo adGroupInfo = createTextCampaignWithAdGroup();
        List<BidModifier> originalBidModifiersIds = createAdGroupBidModifiers(adGroupInfo);
        toggleBidModifiers(originalBidModifiersIds);
        CopyResult copyResult = copyAdGroup(adGroupInfo);
        compareAdGroupBidModifiers(copyResult, originalBidModifiersIds);
    }

    @Test
    @Description("Копирование кампании с включенными модификаторами ставок")
    public void copyCampaignWithEnabledBidModifiers() {
        AdGroupInfo adGroupInfo = createTextCampaignWithAdGroup();
        CampaignInfo campaignInfo = adGroupInfo.getCampaignInfo();
        List<BidModifier> originalBidModifiersIds = createCampaignBidModifiers(campaignInfo);
        CopyResult copyResult = copyCampaign(adGroupInfo.getCampaignInfo());
        compareCampaignsBidModifiers(copyResult, originalBidModifiersIds);
    }

    @Test
    @Description("Копирование кампании с выключенными модификаторами ставок")
    public void copyCampaignWithDisabledBidModifiers() {
        AdGroupInfo adGroupInfo = createTextCampaignWithAdGroup();
        CampaignInfo campaignInfo = adGroupInfo.getCampaignInfo();
        List<BidModifier> originalBidModifiersIds = createCampaignBidModifiers(campaignInfo);
        toggleBidModifiers(originalBidModifiersIds);
        CopyResult copyResult = copyCampaign(adGroupInfo.getCampaignInfo());
        compareCampaignsBidModifiers(copyResult, originalBidModifiersIds);
    }

    @Test
    @Description("Копирование кампании и группы с включенными модификаторами ставок")
    public void copyCampaignAndAdGroupWithEnabledBidModifiers() {
        AdGroupInfo adGroupInfo = createTextCampaignWithAdGroup();
        CampaignInfo campaignInfo = adGroupInfo.getCampaignInfo();
        List<BidModifier> originalCampaignBidModifiersIds = createCampaignBidModifiers(campaignInfo);
        List<BidModifier> originalAdGroupBidModifiersIds = createAdGroupBidModifiers(adGroupInfo);
        CopyResult copyResult = copyCampaign(adGroupInfo.getCampaignInfo());
        compareCampaignsBidModifiers(copyResult, originalCampaignBidModifiersIds);
        compareAdGroupBidModifiers(copyResult, originalAdGroupBidModifiersIds);
    }

    @Test
    @Description("Копирование кампании и группы с выключенными модификаторами ставок")
    public void copyCampaignAndAdGroupWithDisabledBidModifiers() {
        AdGroupInfo adGroupInfo = createTextCampaignWithAdGroup();
        CampaignInfo campaignInfo = adGroupInfo.getCampaignInfo();
        List<BidModifier> originalCampaignBidModifiersIds = createCampaignBidModifiers(campaignInfo);
        List<BidModifier> originalAdGroupBidModifiersIds = createAdGroupBidModifiers(adGroupInfo);
        List<BidModifier> allModifiersIds = new ArrayList<>(originalCampaignBidModifiersIds);
        allModifiersIds.addAll(originalAdGroupBidModifiersIds);
        toggleBidModifiers(allModifiersIds);
        CopyResult copyResult = copyCampaign(adGroupInfo.getCampaignInfo());
        compareCampaignsBidModifiers(copyResult, originalCampaignBidModifiersIds);
        compareAdGroupBidModifiers(copyResult, originalAdGroupBidModifiersIds);
    }

    private void toggleBidModifiers(List<BidModifier> bidModifiers) {
        List<UntypedBidModifier> untypedBidModifiers = bidModifiers.stream()
                        .map(bm -> (UntypedBidModifier) new UntypedBidModifier()
                                .withCampaignId(bm.getAdGroupId() == null ? bm.getCampaignId() : null)
                                .withAdGroupId(bm.getAdGroupId())
                                .withType(bm.getType())
                                .withEnabled(!bm.getEnabled()))
                .collect(Collectors.toList());
        MassResult<UntypedBidModifier> result =
                bidModifierService.toggle(untypedBidModifiers, clientInfo.getClientId(), uid);
        assertFalse(String.format(
                "Something goes wrong while toggle bids modifiers:%n%s",
                        result.getValidationResult().flattenErrors().stream()
                                .map(DefectInfo::toString).collect(Collectors.joining("\n"))),
                result.getValidationResult().hasAnyErrors());
    }

    /**
     * Модификаторы ставок, привязанные к кампаниям получаются не по связям графа сущностей, а через сервис по работе
     * с кампаниями. Они как бы являются частью кампаний. Поэтому в результатах копирования модификаторов ставок
     * для кампаний не будет. Так что приходится выкручиваться.
     * @param copyResult результат копирования кампаний
     * @param createdOriginalBidModifiers оригинальные модификаторы ставок кампаний
     */
    private void compareCampaignsBidModifiers(CopyResult copyResult, List<BidModifier> createdOriginalBidModifiers) {
        List<BidModifier> originalBidModifiers =
                bidModifierService.get(clientInfo.getClientId(), uid,
                        createdOriginalBidModifiers.stream().map(BidModifier::getId).collect(Collectors.toList()));

        var copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());

        List<BaseCampaign> copiedCampaigns = baseCampaignService.get(clientId, uid, copiedCampaignIds);

        var copiedBidModifierIds = StreamEx.of(copiedCampaigns)
                .select(CampaignWithBidModifiers.class)
                .flatMap(cdb -> cdb.getBidModifiers().stream())
                .map(BidModifier::getId)
                .toSet();

        asserts.assertCampaignBidModifiersAreCopied(copiedBidModifierIds, originalBidModifiers);
    }

    private void compareAdGroupBidModifiers(CopyResult copyResult, List<BidModifier> createdOriginalBidModifiers) {
        SoftAssertions softly = new SoftAssertions();
        List<BidModifier> originalBidModifiers =
                bidModifierService.get(clientInfo.getClientId(), uid,
                        createdOriginalBidModifiers.stream().map(BidModifier::getId).collect(Collectors.toList()));

        List<Long> newBidModifiersIds = new ArrayList<>();
        for (BidModifier modifier: originalBidModifiers) {
            Long newBidModifierId = (Long) copyResult.getEntityMapping(BidModifier.class).get(modifier.getId());
            softly.assertThat(newBidModifierId).isNotNull().withFailMessage(
                    "BidModifier with id '%d' not copied", modifier.getId());
            newBidModifiersIds.add(newBidModifierId);
        }

        List<BidModifier> newModifiers = bidModifierService.get(clientInfo.getClientId(), uid, newBidModifiersIds);

        Map<Long, BidModifier> newModifiersByIdsMap = newModifiers.stream()
                .collect(Collectors.toMap(BidModifier::getId, Function.identity()));

        for (BidModifier modifier: originalBidModifiers) {
            Long newBidModifierId = (Long) copyResult.getEntityMapping(BidModifier.class).get(modifier.getId());
            BidModifier newModifier = newModifiersByIdsMap.get(newBidModifierId);
            softly.assertThat(newModifier)
                    .describedAs("BidModifier with id '%d' mapped to new id '%d' not found in database",
                            modifier.getId(), newBidModifierId)
                    .isNotNull();
            softly.assertThat(newModifier)
                    .usingRecursiveComparison(BID_MODIFIERS_COMPARE_STRATEGY)
                    .isEqualTo(modifier);

        }
        softly.assertAll();
    }

    private AdGroupInfo createTextCampaignWithAdGroup() {
        CampaignInfo campaignInfoFrom = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru"),
                clientInfo);
        return steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
    }

    private List<BidModifier> createAdGroupBidModifiers(AdGroupInfo adGroupInfo) {
        AdGroupBidModifierInfo demographicsBidModifier =
                steps.bidModifierSteps().createDefaultAdGroupBidModifierDemographics(adGroupInfo);
        AdGroupBidModifierInfo retargetingBidModifier =
                steps.bidModifierSteps().createDefaultAdGroupBidModifierRetargeting(
                        adGroupInfo, retargetingConditionsIds);
        return List.of(demographicsBidModifier.getBidModifier(), retargetingBidModifier.getBidModifier());
    }

    private List<BidModifier> createCampaignBidModifiers(CampaignInfo campaignInfo) {
        CampaignBidModifierInfo demographicsBidModifier =
                steps.bidModifierSteps().createAnotherCampaignBidModifierDemographics(campaignInfo);
        CampaignBidModifierInfo retargetingBidModifier =
                steps.bidModifierSteps().createDefaultCampaignBidModifierRetargeting(
                        campaignInfo, campaignRetargetingConditionsIds);
        return List.of(demographicsBidModifier.getBidModifier(), retargetingBidModifier.getBidModifier());
    }

    private CopyResult<Long> copyAdGroup(AdGroupInfo adGroupInfo) {
        var xerox = factory.build(
                CopyEntityTestUtils.adGroupCopyConfig(clientInfo, adGroupInfo, adGroupInfo.getCampaignId(), uid));
        var copyResult = xerox.copy();

        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMappings(AdGroup.class).values());
        checkState(!copiedAdGroupIds.isEmpty(), "AdGroup not copied");
        return copyResult;
    }

    private CopyResult<Long> copyCampaign(CampaignInfo campaignInfo) {
        var xerox = factory.build(CopyEntityTestUtils.campaignCopyConfig(clientInfo, campaignInfo, uid));
        var copyResult = xerox.copy();

        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMappings(AdGroup.class).values());
        checkState(!copiedAdGroupIds.isEmpty(), "AdGroup not copied");
        return copyResult;
    }
}
