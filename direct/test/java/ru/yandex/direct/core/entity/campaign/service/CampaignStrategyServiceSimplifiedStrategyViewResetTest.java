package ru.yandex.direct.core.entity.campaign.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualSearchStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.simpleStrategy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignStrategyServiceSimplifiedStrategyViewResetTest {
    @Autowired
    public Steps steps;

    @Autowired
    public CampaignTypedRepository campaignTypedRepository;

    @Autowired
    public CampaignStrategyService campaignStrategyService;

    private UserInfo defaultUser;
    private TextCampaignInfo textCampaignInfo;

    @Before
    public void before() {
        defaultUser = steps.userSteps().createDefaultUser();
        TextCampaign textCampaign =
                defaultTextCampaignWithSystemFields(defaultUser.getClientInfo())
                        .withStrategy(simpleStrategy())
                        .withIsSimplifiedStrategyViewEnabled(true);

        textCampaignInfo = steps.textCampaignSteps().createCampaign(defaultUser.getClientInfo(), textCampaign);
    }

    @Test
    public void updateTextCampaignStrategy_UpdateStrategyName_SimplifiedStrategyViewFlagReset() {
        MassResult<Long> result = campaignStrategyService.updateTextCampaignStrategy(textCampaignInfo.getId(),
                manualSearchStrategy(),
                textCampaignInfo.getUid(),
                UidAndClientId.of(textCampaignInfo.getUid(), textCampaignInfo.getClientId()), false);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(textCampaignInfo.getShard(),
                        List.of(textCampaignInfo.getId()));

        assertThat(((TextCampaign) typedCampaigns.get(0)).getIsSimplifiedStrategyViewEnabled()).isFalse();
    }

    @Test
    public void updateTextCampaignStrategy_AddNewProProperty_SimplifiedStrategyViewFlagReset() {
        DbStrategy newStrategy = simpleStrategy();
        newStrategy.getStrategyData().setAvgCpa(BigDecimal.TEN);
        MassResult<Long> result = campaignStrategyService.updateTextCampaignStrategy(textCampaignInfo.getId(),
                newStrategy,
                textCampaignInfo.getUid(),
                UidAndClientId.of(textCampaignInfo.getUid(), textCampaignInfo.getClientId()), false);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();


        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(textCampaignInfo.getShard(),
                        List.of(textCampaignInfo.getId()));

        assertThat(((TextCampaign) typedCampaigns.get(0)).getIsSimplifiedStrategyViewEnabled()).isTrue();
    }

    @Test
    public void updateTextCampaignStrategy_UpdateStrategySum_SimplifiedStrategyViewFlagNotReset() {
        DbStrategy newStrategy = simpleStrategy();
        newStrategy.getStrategyData().withSum(newStrategy.getStrategyData().getSum().add(BigDecimal.ONE));
        MassResult<Long> result = campaignStrategyService.updateTextCampaignStrategy(textCampaignInfo.getId(),
                newStrategy,
                textCampaignInfo.getUid(),
                UidAndClientId.of(textCampaignInfo.getUid(), textCampaignInfo.getClientId()), false);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();


        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(textCampaignInfo.getShard(),
                        List.of(textCampaignInfo.getId()));

        assertThat(((TextCampaign) typedCampaigns.get(0)).getIsSimplifiedStrategyViewEnabled()).isTrue();
    }

    @Test
    public void updateTextCampaignStrategy_UpdatePlatformToBoth_SimplifiedStrategyViewFlagNotReset() {
        DbStrategy newStrategy = simpleStrategy();
        newStrategy.setPlatform(CampaignsPlatform.BOTH);

        MassResult<Long> result = campaignStrategyService.updateTextCampaignStrategy(textCampaignInfo.getId(),
                newStrategy,
                textCampaignInfo.getUid(),
                UidAndClientId.of(textCampaignInfo.getUid(), textCampaignInfo.getClientId()), false);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();


        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(textCampaignInfo.getShard(),
                        List.of(textCampaignInfo.getId()));

        assertThat(((TextCampaign) typedCampaigns.get(0)).getIsSimplifiedStrategyViewEnabled()).isTrue();
    }

    @Test
    public void updateTextCampaignStrategy_UpdatePlatformToSearchBoth_SimplifiedStrategyViewFlagNotReset() {
        DbStrategy newStrategy = simpleStrategy();
        newStrategy.setPlatform(CampaignsPlatform.SEARCH);

        MassResult<Long> result = campaignStrategyService.updateTextCampaignStrategy(textCampaignInfo.getId(),
                newStrategy,
                textCampaignInfo.getUid(),
                UidAndClientId.of(textCampaignInfo.getUid(), textCampaignInfo.getClientId()), false);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();


        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(textCampaignInfo.getShard(),
                        List.of(textCampaignInfo.getId()));

        assertThat(((TextCampaign) typedCampaigns.get(0)).getIsSimplifiedStrategyViewEnabled()).isTrue();
    }

}
