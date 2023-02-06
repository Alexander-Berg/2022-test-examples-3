package ru.yandex.direct.oneshot.oneshots.reset_no_title_substitute;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.oneshot.configuration.OneshotTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@OneshotTest
@RunWith(SpringRunner.class)
public class ResetNoTitleSubstituteFlagFromCampaignsOneshotTest {

    @Autowired
    Steps steps;

    @Autowired
    ResetNoTitleSubstituteFlagFromCampaignsOneshot oneshot;

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Test
    public void removeHasTitleSubstitution() {
        TextCampaign textCampaign = fullTextCampaign()
                .withHasTitleSubstitution(false);

        TextCampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(textCampaign);

        oneshot.execute(null,
                new ResetNoTitleSubstituteFlagFromCampaignsState(campaignInfo.getCampaignId() - 1),
                campaignInfo.getShard());

        BaseCampaign actualCampaign =
                campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                        List.of(campaignInfo.getCampaignId())).get(0);


        assertThat(((CommonCampaign) actualCampaign).getHasTitleSubstitution())
                .isEqualTo(true);
    }

    @Test
    public void removeHasTitleSubstitutionFromCampaignWithAllAvailableOptions_OtherFieldsNotChanged() {
        TextCampaign textCampaign = fullTextCampaign()
                .withHasTitleSubstitution(false)
                .withEnableCpcHold(true)
                .withIsAloneTrafaretAllowed(true)
                .withEnableCompanyInfo(false)
                .withIsOrderPhraseLengthPrecedenceEnabled(true)
                .withIsSimplifiedStrategyViewEnabled(true)
                .withIsSkadNetworkEnabled(true)
                .withIsTouch(true)
                .withHasExtendedGeoTargeting(false)
                .withHasTitleSubstitution(false)
                .withRequireFiltrationByDontShowDomains(true);

        TextCampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(textCampaign);

        oneshot.execute(null,
                new ResetNoTitleSubstituteFlagFromCampaignsState(campaignInfo.getCampaignId() - 1),
                campaignInfo.getShard());

        BaseCampaign actualCampaign =
                campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                        List.of(campaignInfo.getCampaignId())).get(0);

        DefaultCompareStrategy compareStrategy = DefaultCompareStrategies
                .onlyFields(
                        newPath("enableCpcHold"),
                        newPath("isAloneTrafaretAllowed"),
                        newPath("isNewIosVersionEnabled"),
                        newPath("enableCompanyInfo"),
                        newPath("isOrderPhraseLengthPrecedenceEnabled"),
                        newPath("isSimplifiedStrategyViewEnabled"),
                        newPath("isSkadNetworkEnabled"),
                        newPath("isTouch"),
                        newPath("hasExtendedGeoTargeting"),
                        newPath("requireFiltrationByDontShowDomains")
                );

        assertThat(((CommonCampaign) actualCampaign))
                .is(matchedBy(beanDiffer(textCampaign).useCompareStrategy(compareStrategy)));
    }

}
