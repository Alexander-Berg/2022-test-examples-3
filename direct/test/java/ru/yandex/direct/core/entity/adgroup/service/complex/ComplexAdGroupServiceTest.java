package ru.yandex.direct.core.entity.adgroup.service.complex;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.container.ComplexAdGroup;
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.banner.service.ComplexBannerService;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting;
import ru.yandex.direct.core.entity.offerretargeting.repository.OfferRetargetingRepository;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.retargeting.model.InterestLink;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestVcards;
import ru.yandex.direct.core.testing.info.AdGroupBidModifierInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.bidmodifiers.container.ComplexBidModifierConverter.convertToComplexBidModifier;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingUtils.convertRetargetingsToTargetInterests;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDesktop;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexAdGroupServiceTest {

    private static final CompareStrategy STRATEGY =
            DefaultCompareStrategies.allFieldsExcept(
                    newPath("adGroup", "lastChange"),
                    newPath("complexBidModifier", "mobileModifier", "lastChange"),
                    newPath("complexBidModifier", "mobileModifier", "mobileAdjustment", "id"),
                    newPath("complexBidModifier", "mobileModifier", "mobileAdjustment", "lastChange"),
                    newPath("complexBidModifier", "desktopModifier", "lastChange"),
                    newPath("complexBidModifier", "desktopModifier", "desktopAdjustment", "id")
            ).forFields(
                    newPath("targetInterests", "\\d+", "priceContext")).useDiffer(new BigDecimalDiffer());

    @Autowired
    private ComplexAdGroupService complexAdGroupService;

    @Autowired
    private ComplexBannerService complexBannerService;

    @Autowired
    private RelevanceMatchRepository relevanceMatchRepository;

    @Autowired
    private OfferRetargetingRepository offerRetargetingRepository;

    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private AdGroupInfo adGroupInfo;
    private RelevanceMatch relevanceMatch;
    private OfferRetargeting offerRetargeting;
    private TargetInterest targetInterest;
    private ComplexBanner complexBanner;
    private ComplexBidModifier complexBidModifier;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        RetargetingInfo retargetingInfo = steps.retargetingSteps().createDefaultRetargeting(adGroupInfo);

        Long relevanceMatchId = steps.relevanceMatchSteps().addDefaultRelevanceMatchToAdGroup(adGroupInfo);
        Map<Long, RelevanceMatch> relevanceMatches =
                relevanceMatchRepository.getRelevanceMatchesByIds(clientInfo.getShard(), clientInfo.getClientId(),
                        singletonList(relevanceMatchId));
        relevanceMatch = relevanceMatches.get(relevanceMatchId);

        OfferRetargeting defaultOfferRetargeting = steps.offerRetargetingSteps()
                .defaultOfferRetargetingForGroup(adGroupInfo);
        offerRetargeting = steps.offerRetargetingSteps()
                .addOfferRetargetingToAdGroup(defaultOfferRetargeting, adGroupInfo);

        List<InterestLink> existingInterests =
                retargetingConditionRepository.getExistingInterest(clientInfo.getShard(), clientInfo.getClientId());
        targetInterest = convertRetargetingsToTargetInterests(singletonList(retargetingInfo.getRetargeting()),
                existingInterests).get(0);

        VcardInfo vcardInfo = steps.vcardSteps().createVcard(TestVcards.fullVcard(), clientInfo);
        SitelinkSetInfo sitelinkSetInfo = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(clientInfo);

        steps.bannerSteps().createBanner(
                activeTextBanner()
                        .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId())
                        .withVcardId(vcardInfo.getVcardId())
                        .withCreativeId(creativeInfo.getCreativeId()),
                adGroupInfo);

        complexBanner = complexBannerService.getComplexBannersByAdGroupIds(
                        clientInfo.getClientId(), clientInfo.getUid(), singletonList(adGroupInfo.getAdGroupId()))
                .get(0);

        AdGroupBidModifierInfo modifierInfo1 = steps.bidModifierSteps()
                .createAdGroupBidModifier(createDefaultBidModifierMobile(adGroupInfo.getCampaignId()), adGroupInfo);
        AdGroupBidModifierInfo modifierInfo2 = steps.bidModifierSteps()
                .createAdGroupBidModifier(createDefaultBidModifierDesktop(adGroupInfo.getCampaignId()), adGroupInfo);
        complexBidModifier =
                convertToComplexBidModifier(asList(modifierInfo1.getBidModifier(), modifierInfo2.getBidModifier()));
    }

    @Test
    public void getComplexAdGroupsWithoutKeywords_ReturnAllPropertiesExceptKeywords() {
        List<ComplexTextAdGroup> complexAdGroups = complexAdGroupService.getComplexAdGroupsWithoutKeywords(
                clientInfo.getUid(), UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                singletonList(adGroupInfo.getAdGroupId()));
        assertThat(complexAdGroups, hasSize(1));

        ComplexAdGroup expectedComplexAdGroup = new ComplexTextAdGroup()
                .withKeywords(null) // явно задаём, что ключевиков быть не должно в ответе
                .withAdGroup(adGroupInfo.getAdGroup())
                .withComplexBanners(singletonList(complexBanner))
                .withComplexBidModifier(complexBidModifier)
                .withRelevanceMatches(singletonList(relevanceMatch))
                .withOfferRetargetings(singletonList(offerRetargeting))
                .withTargetInterests(singletonList(targetInterest));

        assertThat(complexAdGroups.get(0), beanDiffer(expectedComplexAdGroup).useCompareStrategy(STRATEGY));
    }
}
