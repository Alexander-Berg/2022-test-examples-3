package ru.yandex.direct.core.entity.bidmodifiers.toggle;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifiers.container.UntypedBidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefectIds.GeneralDefects.GEO_BID_MODIFIERS_NOT_SUPPORTED_ON_ADGROUPS;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.ARCHIVED_CAMPAIGN_MODIFICATION;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultGeoAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultRetargetingAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyGeoModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyRetargetingModifier;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@Description("Проверка негативных сценариев отключения набора корректировок ставок")
@RunWith(SpringJUnit4ClassRunner.class)
public class ToggleBidModifiersNegativeTest {
    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    private AdGroupInfo adGroupInfo;
    private CampaignInfo campaignInfo;
    private BidModifierGeo bidModifierGeoItem;
    private BidModifierRetargeting bidModifierRetargetingItem;
    private List<UntypedBidModifier> retargetingItems, geoItems;

    @Before
    public void before() throws Exception {
        adGroupInfo = adGroupSteps.createActiveTextAdGroup();
        campaignInfo = adGroupInfo.getCampaignInfo();

        //Создаем условие ретаргетинга
        RetConditionInfo retCondition = retConditionSteps.createDefaultRetCondition(campaignInfo.getClientInfo());

        //Добавляем корректировку на компанию
        bidModifierRetargetingItem = createEmptyRetargetingModifier()
                .withRetargetingAdjustments(createDefaultRetargetingAdjustments(retCondition.getRetConditionId()))
                .withCampaignId(campaignInfo.getCampaignId());

        bidModifierGeoItem = createEmptyGeoModifier()
                .withRegionalAdjustments(createDefaultGeoAdjustments())
                .withCampaignId(campaignInfo.getCampaignId());

        bidModifierService.add(
                asList(bidModifierRetargetingItem, bidModifierGeoItem),
                campaignInfo.getClientId(), campaignInfo.getUid());

        retargetingItems = singletonList((UntypedBidModifier) new UntypedBidModifier()
                .withCampaignId(campaignInfo.getCampaignId())
                .withType(bidModifierRetargetingItem.getType())
                .withEnabled(false));

        geoItems = singletonList((UntypedBidModifier) new UntypedBidModifier()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withType(bidModifierGeoItem.getType())
                .withEnabled(false));
    }

    @Test
    @Description("Попробуем отключить набор корректировок для архивной кампании")
    public void archivedCampaignTest() {
        testCampaignRepository.archiveCampaign(campaignInfo.getShard(), campaignInfo.getCampaignId());

        MassResult<UntypedBidModifier> result =
                bidModifierService.toggle(retargetingItems, campaignInfo.getClientId(),
                        campaignInfo.getUid());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(
                        validationError(path(index(0), field("campaignId")), ARCHIVED_CAMPAIGN_MODIFICATION)));
    }

    @Test
    @Description("Попробуем отключить набор корректировок на регион для группы")
    public void regionalAdjustmentWithGroupTest() {
        MassResult<UntypedBidModifier> result =
                bidModifierService.toggle(geoItems, adGroupInfo.getClientId(),
                        adGroupInfo.getUid());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), GEO_BID_MODIFIERS_NOT_SUPPORTED_ON_ADGROUPS)));
    }
}
