package ru.yandex.direct.core.entity.bidmodifiers.add.mobile;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.singleValueModifierAlreadyExists;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientMobileModifier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Негативные сценарии добавления мобильных корректировок ставок")
public class AddBidModifiersMobileNegativeTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private CampaignSteps campaignSteps;
    private CampaignInfo campaign;

    @Before
    public void before() {
        campaign = campaignSteps.createActiveTextCampaign();
    }

    @Test
    public void addTwoMobileModifiersTest() {
        BidModifierMobileAdjustment mobileAdjustment = createDefaultClientMobileAdjustment();
        MassResult<List<Long>> firstResult = addMobile(mobileAdjustment);
        MassResult<List<Long>> secondResult = addMobile(mobileAdjustment);

        assertSoftly(softly -> {
            softly.assertThat(firstResult.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(secondResult.getValidationResult()).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(index(0), field("mobileAdjustment")),
                            singleValueModifierAlreadyExists()))));
        });
    }

    private MassResult<List<Long>> addMobile(BidModifierMobileAdjustment mobileAdjustment) {
        return bidModifierService.add(
                singletonList(
                        createEmptyClientMobileModifier()
                                .withEnabled(true)
                                .withCampaignId(campaign.getCampaignId())
                                .withMobileAdjustment(mobileAdjustment)
                ), campaign.getClientId(), campaign.getUid());
    }
}
