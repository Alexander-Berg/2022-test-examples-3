package ru.yandex.direct.core.entity.campaign.repository.modify;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getExpectedTextCampaign;
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getTextCampaignModelChanges;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TextCampaignModifyRepositoryUpdateTest {

    private static final List<String> SSP_LIST = List.of("ImSSP");

    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;
    @Autowired
    public Steps steps;

    private CampaignInfo textCampaignInfo;
    private TextCampaign updatingTextCampaign;
    private TextCampaign newTextCampaign;
    private List<Long> metrikaCounters;

    @Before
    public void before() {
        textCampaignInfo = steps.campaignSteps().createActiveTextCampaign();
        steps.sspPlatformsSteps().addSspPlatforms(SSP_LIST);

        updatingTextCampaign = new TextCampaign()
                .withId(textCampaignInfo.getCampaignId())
                .withName(textCampaignInfo.getCampaign().getName());

        newTextCampaign = new TextCampaign()
                .withId(textCampaignInfo.getCampaignId())
                .withName("newName" + RandomStringUtils.randomAlphabetic(7));

        metrikaCounters = List.of((long) RandomNumberUtils.nextPositiveInteger());

    }

    @Test
    public void update() {
        TextCampaign expectedCampaign = updateTextCampaign(updatingTextCampaign, null, newTextCampaign);

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(textCampaignInfo.getShard(),
                        Collections.singletonList(textCampaignInfo.getCampaignId()));
        List<TextCampaign> textCampaigns = mapList(typedCampaigns, TextCampaign.class::cast);
        TextCampaign actualCampaign = textCampaigns.get(0);

        assertThat(actualCampaign).is(matchedBy(beanDiffer(expectedCampaign)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    public TextCampaign updateTextCampaign(TextCampaign updatingTextCampaign,
                                           List<Long> metrikaCounters,
                                           TextCampaign newTextCampaign) {
        ModelChanges<TextCampaign> textCampaignModelChanges =
                getTextCampaignModelChanges(metrikaCounters, newTextCampaign);

        AppliedChanges<TextCampaign> textCampaignAppliedChanges =
                textCampaignModelChanges.applyTo(updatingTextCampaign);

        RestrictedCampaignsUpdateOperationContainer updateParameters =
                RestrictedCampaignsUpdateOperationContainer.create(
                textCampaignInfo.getShard(),
                textCampaignInfo.getUid(),
                textCampaignInfo.getClientId(),
                textCampaignInfo.getUid(),
                textCampaignInfo.getUid());

        campaignModifyRepository.updateCampaigns(updateParameters,
                Collections.singletonList(textCampaignAppliedChanges));

        return getExpectedTextCampaign(textCampaignInfo, textCampaignModelChanges);
    }
}
