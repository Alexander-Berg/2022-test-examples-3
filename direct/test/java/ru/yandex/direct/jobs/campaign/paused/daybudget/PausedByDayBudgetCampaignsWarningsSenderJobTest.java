package ru.yandex.direct.jobs.campaign.paused.daybudget;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsDayBudgetShowMode;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.scheduler.support.DirectShardedJob;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.Tables.WALLET_CAMPAIGNS;

@JobsTest
@ExtendWith(SpringExtension.class)
public class PausedByDayBudgetCampaignsWarningsSenderJobTest extends AbstractPausedByDayBudgetWarningsSenderJobTest {

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider provider;

    @Override
    protected DirectShardedJob initJob(int shard) {
        return new PausedByDayBudgetCampaignsWarningsSenderJob(
                shard,
                pausedByDayBudgetService,
                campaignRepository,
                pausedByDayBudgetSenderService
        );
    }

    public static Object[][] notificationPermissionsTestParameters() {
        return new Object[][]{
                {"test1", new PausedByDayBudgetTestUtils.NotificationSendingPermissions(
                        false, false, false)},
                {"test2", new PausedByDayBudgetTestUtils.NotificationSendingPermissions(
                        true, false, false)},
                {"test3", new PausedByDayBudgetTestUtils.NotificationSendingPermissions(
                        true, true, false)},
                {"test4", new PausedByDayBudgetTestUtils.NotificationSendingPermissions(
                        true, true, true)}
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("notificationPermissionsTestParameters")
    void notificationPermissionsTest(String description,
                                     PausedByDayBudgetTestUtils.NotificationSendingPermissions permissions) {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        PausedByDayBudgetTestUtils.enableFeatures(steps.featureSteps(), clientInfo, permissions);
        PausedByDayBudgetTestUtils.setAllowingSendingNotifications(
                provider.ppc(clientInfo.getShard()), campaignInfo.getCampaignId(), permissions);

        PausedByDayBudgetTestUtils.markCampaignsReadyForNotification(
                provider.ppc(clientInfo.getShard()), List.of(campaignInfo.getCampaignId()));


        initAndExecuteJobs(clientInfo);

        var types = EnumSet.noneOf(PausedByDayBudgetNotificationType.class);

        // Если не разрешена отправка email, то такая кампания не будет рассматриваться на отправку уведомления
        if (permissions.featureEnabled && permissions.allowedSendingEmails) {
            types.add(PausedByDayBudgetNotificationType.EVENT_LOG);
            types.add(PausedByDayBudgetNotificationType.EMAIL);
        }

        if (types.isEmpty()) {
            assertThat(sentNotificationsById).isEmpty();
        } else {
            assertThat(sentNotificationsById)
                    .hasSize(1)
                    .containsEntry(campaignInfo.getCampaignId(), types);
        }

        PausedByDayBudgetTestUtils.removeCampOptions(provider.ppc(clientInfo.getShard()),
                List.of(campaignInfo.getCampaignId()));
    }

    @Test
    void filteringTest() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var campaignInfo1 = steps.campaignSteps().createActiveCampaign(clientInfo);
        var campaignInfo2 = steps.campaignSteps().createWalletCampaign(clientInfo);
        campaignInfo2.getCampaign().getBalanceInfo().setWalletCid(campaignInfo2.getCampaignId());
        var campaignInfo3 = steps.campaignSteps().createCampaignUnderWallet(clientInfo,
                campaignInfo2.getCampaign().getBalanceInfo());

        var permissions = new PausedByDayBudgetTestUtils.NotificationSendingPermissions(true, true, true);

        PausedByDayBudgetTestUtils.enableFeatures(steps.featureSteps(), clientInfo, permissions);
        PausedByDayBudgetTestUtils.setAllowingSendingNotifications(
                provider.ppc(clientInfo.getShard()),
                campaignInfo1.getCampaignId(),
                permissions
        );
        PausedByDayBudgetTestUtils.markCampaignsReadyForNotification(provider.ppc(clientInfo.getShard()),
                List.of(campaignInfo1.getCampaignId(), campaignInfo2.getCampaignId(), campaignInfo3.getCampaignId()));

        steps.campaignSteps().setDayBudget(campaignInfo1, BigDecimal.ZERO, CampaignsDayBudgetShowMode.default_, 1);
        steps.campaignSteps().archiveCampaign(campaignInfo2);

        initAndExecuteJobs(clientInfo);

        assertThat(sentNotificationsById).hasSize(1)
                .containsEntry(campaignInfo3.getCampaignId(),
                        PausedByDayBudgetNotificationType.retainSuitableForCampaignTypes(
                                PausedByDayBudgetTestUtils.typesByPermissions(permissions))
                );
    }

    @Test
    void testCampaignsUnderWallet() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var wallet = steps.campaignSteps().createWalletCampaign(clientInfo);
        provider.ppc(wallet.getShard())
                .insertInto(WALLET_CAMPAIGNS, WALLET_CAMPAIGNS.WALLET_CID)
                .values(wallet.getCampaignId())
                .execute();
        var campaign = steps.campaignSteps().createActiveCampaign(clientInfo);
        provider.ppc(clientInfo.getShard()).update(CAMPAIGNS)
                .set(CAMPAIGNS.WALLET_CID, wallet.getCampaignId())
                .where(CAMPAIGNS.CID.eq(campaign.getCampaignId()))
                .execute();
        var permissions = new PausedByDayBudgetTestUtils.NotificationSendingPermissions(true, true, true);
        PausedByDayBudgetTestUtils.enableFeatures(steps.featureSteps(), clientInfo, permissions);
        PausedByDayBudgetTestUtils.setAllowingSendingNotifications(
                provider.ppc(clientInfo.getShard()),
                List.of(wallet.getCampaignId(), campaign.getCampaignId()),
                permissions
        );
        PausedByDayBudgetTestUtils.markCampaignsReadyForNotification(
                provider.ppc(clientInfo.getShard()),
                List.of(campaign.getCampaignId())
        );

        initAndExecuteJobs(clientInfo);

        assertThat(sentNotificationsById)
                .hasSize(1).containsEntry(
                        campaign.getCampaignId(),
                        PausedByDayBudgetNotificationType.retainSuitableForCampaignTypes(
                                PausedByDayBudgetTestUtils.typesByPermissions(permissions))
                );

    }

}
