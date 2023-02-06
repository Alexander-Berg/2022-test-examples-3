package ru.yandex.direct.jobs.campaign.paused.daybudget;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.Tables;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.Assertions.assertThat;

@JobsTest
@ExtendWith(SpringExtension.class)
class PausedByDayBudgetServiceTest {

    @Autowired
    private PausedByDayBudgetService service;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private Steps steps;

    @Autowired
    DslContextProvider provider;

    static Object[][] getAllowedNotificationTypesParameters() {
        return new Object[][]{
                {"Feature disabled", new PausedByDayBudgetTestUtils.NotificationSendingPermissions(
                        false, true, true)},
                {"Feature enabled. Sms disabled. Can send email. Can't send sms",
                        new PausedByDayBudgetTestUtils.NotificationSendingPermissions(
                                true, true, false)},
                {"Feature enabled. Sms disabled. Can send email. Can send sms",
                        new PausedByDayBudgetTestUtils.NotificationSendingPermissions(
                                true, true, true)},
                {"Feature enabled. Sms enabled. Can't send email. Can send sms",
                        new PausedByDayBudgetTestUtils.NotificationSendingPermissions(
                                true, false, true)},
                {"Feature enabled. Sms enabled. Can send email. Can send sms",
                        new PausedByDayBudgetTestUtils.NotificationSendingPermissions(
                                true, true, true)},
                {"Feature enabled. Sms disabled. Can't send email. Can send sms",
                        new PausedByDayBudgetTestUtils.NotificationSendingPermissions(
                                true, false, true)}
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getAllowedNotificationTypesParameters")
    void testGetAllowedNotificationTypes(String description,
                                         PausedByDayBudgetTestUtils.NotificationSendingPermissions permissions) {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var campaign = steps.campaignSteps().createActiveCampaign(clientInfo);

        PausedByDayBudgetTestUtils.enableFeatures(steps.featureSteps(), clientInfo, permissions);
        PausedByDayBudgetTestUtils.setAllowingSendingNotifications(
                provider.ppc(clientInfo.getShard()),
                campaign.getCampaignId(),
                permissions
        );

        var result = service.getAllowedNotificationTypesById(
                PausedByDayBudgetTestUtils.campaignsByInfos(
                        clientInfo.getShard(),
                        campaignRepository,
                        List.of(campaign)
                ));

        assertThat(result).hasSize(1)
                .containsKey(campaign.getCampaignId());

        var set = result.get(campaign.getCampaignId());

        assertThat(set).containsExactlyInAnyOrder(PausedByDayBudgetTestUtils.typesByPermissionsAsArray(permissions));
    }

    @Test
    void testGetSuitableCampaigns() {

        var clientInfo = steps.clientSteps().createDefaultClient();
        var campaign1 = steps.campaignSteps().createActiveCampaign(clientInfo);

        BigDecimal biggerThanExisting = BigDecimal.TEN.multiply(campaign1.getCampaign().getBalanceInfo().getSum());

        provider.ppc(campaign1.getShard())
                .update(Tables.CAMPAIGNS)
                .set(Tables.CAMPAIGNS.SUM_SPENT, biggerThanExisting)
                .where(Tables.CAMPAIGNS.CID.eq(campaign1.getCampaignId()))
                .execute();

        var campaign2 = steps.campaignSteps().createActiveCampaign(clientInfo);

        Set<Long> campaignIds = service
                .getSuitableCampaigns(
                        clientInfo.getShard(),
                        PausedByDayBudgetTestUtils.campaignsByInfos(
                                clientInfo.getShard(), campaignRepository,
                                List.of(campaign1, campaign2)
                        ))
                .map(Campaign::getId)
                .collect(Collectors.toSet());

        assertThat(campaignIds).containsExactly(campaign2.getCampaignId());
    }
}
