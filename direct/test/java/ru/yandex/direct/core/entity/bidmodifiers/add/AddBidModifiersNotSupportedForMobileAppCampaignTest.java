package ru.yandex.direct.core.entity.bidmodifiers.add;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.notSupportedMultiplier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientMobileModifier;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddBidModifiersNotSupportedForMobileAppCampaignTest {
    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private BidModifierService bidModifierService;

    @Test
    public void mobileModifierIsNotSupportedOnMobileCampaign() {
        AdGroupInfo adGroup = adGroupSteps.createActiveMobileContentAdGroup();
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientMobileModifier()
                                .withCampaignId(adGroup.getCampaignId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())
                ), adGroup.getClientId(), adGroup.getUid());
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("mobileAdjustment")),
                        notSupportedMultiplier())));
    }

    @Test
    public void mobileModifierIsNotSupportedOnMobileAdGroup() {
        AdGroupInfo adGroup = adGroupSteps.createActiveMobileContentAdGroup();
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientMobileModifier()
                                .withAdGroupId(adGroup.getAdGroupId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())
                ), adGroup.getClientId(), adGroup.getUid());
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("mobileAdjustment")),
                        notSupportedMultiplier())));
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers, ClientId clientId, Long clientUid) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, clientId, clientUid);
    }
}
