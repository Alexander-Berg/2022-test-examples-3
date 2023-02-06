package ru.yandex.direct.oneshot.oneshots.returnprostrategyview;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.oneshot.configuration.OneshotTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxImpressionsDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategyForSimpleView;
import static ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign;

@OneshotTest
@RunWith(SpringRunner.class)
public class ReturnProStrategyViewOneshotTest {
    @Autowired
    Steps steps;

    @Autowired
    ReturnProStrategyViewOneshot oneshot;

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Test
    public void returnProViewForInvalidAttributionModel() {
        TextCampaign textCampaign = fullTextCampaign()
                .withIsSimplifiedStrategyViewEnabled(true)
                .withStrategy(defaultStrategyForSimpleView(123L))
                .withAttributionModel(CampaignAttributionModel.FIRST_CLICK);

        TextCampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(textCampaign);

        oneshot.execute(null, new ReturnProStrategyViewState(campaignInfo.getCampaignId() - 1),
                campaignInfo.getShard());
        BaseCampaign actualCampaign =
                campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                        List.of(campaignInfo.getCampaignId())).get(0);
        assertThat(((CommonCampaign) actualCampaign).getIsSimplifiedStrategyViewEnabled())
                .isEqualTo(false);
    }

    @Test
    public void returnProViewForInvalidStrategyField() {
        TextCampaign textCampaign = fullTextCampaign()
                .withStrategy(defaultAutobudgetMaxImpressionsDbStrategy())
                .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK);

        TextCampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(textCampaign);

        oneshot.execute(null, new ReturnProStrategyViewState(campaignInfo.getCampaignId() - 1),
                campaignInfo.getShard());
        BaseCampaign actualCampaign =
                campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                        List.of(campaignInfo.getCampaignId())).get(0);
        assertThat(((CommonCampaign) actualCampaign).getIsSimplifiedStrategyViewEnabled())
                .isEqualTo(false);
    }

    @Test
    public void notReturnProViewForValidSimpleCampaign() {
        TextCampaign textCampaign = fullTextCampaign()
                .withIsSimplifiedStrategyViewEnabled(true)
                .withStrategy(defaultStrategyForSimpleView(123L))
                .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK);

        TextCampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(textCampaign);

        oneshot.execute(null, new ReturnProStrategyViewState(campaignInfo.getCampaignId() - 1),
                campaignInfo.getShard());
        BaseCampaign actualCampaign =
                campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                        List.of(campaignInfo.getCampaignId())).get(0);
        assertThat(((CommonCampaign) actualCampaign).getIsSimplifiedStrategyViewEnabled())
                .isEqualTo(true);
    }

    @Test
    public void finishOneshotAfterLastCampaign() {
        TextCampaign textCampaign = fullTextCampaign()
                .withIsSimplifiedStrategyViewEnabled(true)
                .withStrategy(defaultStrategyForSimpleView(123L))
                .withAttributionModel(CampaignAttributionModel.FIRST_CLICK);

        TextCampaignInfo campaignInfo = steps.textCampaignSteps().createCampaign(textCampaign);
        ReturnProStrategyViewState state = oneshot.execute(null,
                new ReturnProStrategyViewState(campaignInfo.getCampaignId()),
                campaignInfo.getShard());

        assertThat(state).isNull();
    }
}
