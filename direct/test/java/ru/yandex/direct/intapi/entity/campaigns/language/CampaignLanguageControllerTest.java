package ru.yandex.direct.intapi.entity.campaigns.language;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.campaign.model.ContentLanguage;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.regions.Region;

import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignLanguageControllerTest {
    @Autowired
    private Steps steps;

    @Autowired
    private CampaignLanguageController controller;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    private CampaignInfo campaignInfo;

    @Before
    public void before() throws Exception {
        var clientInfo = steps.clientSteps().createDefaultClient();
        campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
    }

    @Test
    public void setCampaignLanguagePositiveTest() {
        var response = controller.setCampaignContentLanguage(campaignInfo.getCampaignId(), ContentLanguage.RU);

        var campaigns =
                campaignTypedRepository.getStrictly(
                        campaignInfo.getShard(),
                        List.of(campaignInfo.getCampaignId()),
                        TextCampaign.class);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.isSuccessful()).isTrue();
            softly.assertThat(campaigns).hasSize(1);
            softly.assertThat(campaigns.get(0).getContentLanguage()).isEqualTo(ContentLanguage.RU);
        });
    }

    @Test
    public void resetCampaignLanguagePositiveTest() {
        campaignRepository
                .updateCampaignLang(campaignInfo.getShard(), List.of(campaignInfo.getCampaignId()), ContentLanguage.RU);

        var response = controller.setCampaignContentLanguage(campaignInfo.getCampaignId(), null);

        var campaigns =
                campaignTypedRepository.getStrictly(
                        campaignInfo.getShard(),
                        List.of(campaignInfo.getCampaignId()),
                        TextCampaign.class);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.isSuccessful()).isTrue();
            softly.assertThat(campaigns).hasSize(1);
            softly.assertThat(campaigns.get(0).getContentLanguage()).isEqualTo(null);
        });
    }

    @Test
    public void setCampaignLanguageNegativeTest() {
        TextAdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId())
                .withGeo(List.of(Region.RUSSIA_REGION_ID));
        steps.adGroupSteps().createAdGroup(adGroup, campaignInfo);

        var response = controller.setCampaignContentLanguage(campaignInfo.getCampaignId(), ContentLanguage.UA);

        var campaigns =
                campaignTypedRepository.getStrictly(
                        campaignInfo.getShard(),
                        List.of(campaignInfo.getCampaignId()),
                        TextCampaign.class);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.isSuccessful()).isFalse();
            softly.assertThat(campaigns).hasSize(1);
            softly.assertThat(campaigns.get(0).getContentLanguage()).isEqualTo(null);
        });
    }
}
