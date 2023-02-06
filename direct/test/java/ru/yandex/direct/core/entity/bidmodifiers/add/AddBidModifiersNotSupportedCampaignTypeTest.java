package ru.yandex.direct.core.entity.bidmodifiers.add;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds;
import ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientMobileModifier;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddBidModifiersNotSupportedCampaignTypeTest {
    @Autowired
    private Steps steps;

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private RequestCampaignAccessibilityCheckerProvider requestAccessibleCampaignTypes;

    @Before
    public void setUp() throws Exception {
        // В ядре не должно быть неподдерживаемых типов, это скорее тест для API
        requestAccessibleCampaignTypes.setApi5();
    }

    @Test
    public void notSupportedAdGroupTypeTest() {
        AdGroupInfo activeMcBannerAdGroup = steps.adGroupSteps().createActiveMcBannerAdGroup();
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientMobileModifier()
                                .withAdGroupId(activeMcBannerAdGroup.getAdGroupId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())
                ), activeMcBannerAdGroup.getClientId(), activeMcBannerAdGroup.getUid());
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("adGroupId")),
                        RetargetingDefects.adGroupNotFound(activeMcBannerAdGroup.getAdGroupId()))));
    }

    @Test
    public void notSupportedCampaignTypeTest() {
        AdGroupInfo activeMcBannerAdGroup = steps.adGroupSteps().createActiveMcBannerAdGroup();
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientMobileModifier()
                                .withCampaignId(activeMcBannerAdGroup.getCampaignId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())
                ), activeMcBannerAdGroup.getClientId(), activeMcBannerAdGroup.getUid());
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("campaignId")),
                        new Defect<>(CampaignDefectIds.Gen.CAMPAIGN_NOT_FOUND))));
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers, ClientId clientId, Long clientUid) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, clientId, clientUid);
    }
}
