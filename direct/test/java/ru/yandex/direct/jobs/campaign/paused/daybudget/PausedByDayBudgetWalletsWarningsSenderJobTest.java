package ru.yandex.direct.jobs.campaign.paused.daybudget;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.scheduler.support.DirectShardedJob;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.Tables.WALLET_CAMPAIGNS;

@JobsTest
@ExtendWith(SpringExtension.class)
class PausedByDayBudgetWalletsWarningsSenderJobTest extends AbstractPausedByDayBudgetWarningsSenderJobTest {

    @Autowired
    private DslContextProvider provider;

    @Autowired
    private Steps steps;

    private final PausedByDayBudgetTestUtils.NotificationSendingPermissions permissions =
            new PausedByDayBudgetTestUtils.NotificationSendingPermissions(true, true, true);

    @Override
    protected DirectShardedJob initJob(int shard) {
        return new PausedByDayBudgetWalletsWarningsSenderJob(
                shard,
                pausedByDayBudgetService,
                pausedByDayBudgetSenderService,
                campaignRepository
        );
    }

    private static BiConsumer<DSLContext, ClientInfo> emptyClientBiConsumer() {
        return PausedByDayBudgetTestUtils.emptyConsumer(DSLContext.class, ClientInfo.class);
    }

    private static BiConsumer<DSLContext, CampaignInfo> emptyCampaignBiConsumer() {
        return PausedByDayBudgetTestUtils.emptyConsumer(DSLContext.class, CampaignInfo.class);
    }

    static Object[][] oneClient_oneWalletReady_parameters() {
        return new Object[][]{
                {
                        "Уведомление должно послаться",
                        emptyCampaignBiConsumer(),
                        emptyCampaignBiConsumer(),
                        true
                },
                {
                        "Для любых типов кроме INTERNAL_FREE и INTERNAL_DISTRIB должно прийти уведомление",
                        emptyCampaignBiConsumer(),
                        (BiConsumer<DSLContext, CampaignInfo>) (context, campaignInfo) ->
                                context.update(CAMPAIGNS)
                                        .set(CAMPAIGNS.TYPE, CampaignsType.dynamic)
                                        .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                                        .execute(),
                        true
                },
                {
                        "Для INTERNAL_FREE не должно прийти",
                        emptyCampaignBiConsumer(),
                        (BiConsumer<DSLContext, CampaignInfo>) (context, campaignInfo) ->
                                context.update(CAMPAIGNS)
                                        .set(CAMPAIGNS.TYPE, CampaignsType.internal_free)
                                        .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                                        .execute(),
                        false
                },
                {
                        "Для INTERNAL_DSITRIB не должно прийти",
                        emptyCampaignBiConsumer(),
                        (BiConsumer<DSLContext, CampaignInfo>) (context, campaignInfo) ->
                                context.update(CAMPAIGNS)
                                        .set(CAMPAIGNS.TYPE, CampaignsType.internal_distrib)
                                        .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                                        .execute(),
                        false
                },
                {
                        "Если дневной бюджет ноль, то уведомление не должно прийти",
                        (BiConsumer<DSLContext, CampaignInfo>) (context, campaignInfo) ->
                                context.update(CAMPAIGNS)
                                        .set(CAMPAIGNS.DAY_BUDGET, BigDecimal.ZERO)
                                        .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                                        .execute(),
                        emptyCampaignBiConsumer(),
                        false
                }
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("oneClient_oneWalletReady_parameters")
    void oneClient_oneWalletReady(String description,
                                  @NotNull BiConsumer<DSLContext, CampaignInfo> walletPropertyChanger,
                                  @NotNull BiConsumer<DSLContext, CampaignInfo> campaignPropertyChanger,
                                  boolean needSendNotification) {
        var client = steps.clientSteps().createDefaultClient();
        var wallet = steps.campaignSteps().createWalletCampaign(client);
        provider.ppc(wallet.getShard())
                .insertInto(WALLET_CAMPAIGNS, WALLET_CAMPAIGNS.WALLET_CID)
                .values(wallet.getCampaignId())
                .execute();

        walletPropertyChanger.accept(provider.ppc(client.getShard()), wallet);
        var campaign = steps.campaignSteps().createActiveCampaign(client);
        campaignPropertyChanger
                .andThen((context, campaignInfo) ->
                        context.update(CAMPAIGNS)
                                .set(CAMPAIGNS.WALLET_CID, wallet.getCampaignId())
                                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                                .execute())
                .accept(provider.ppc(client.getShard()), campaign);

        PausedByDayBudgetTestUtils.enableFeatures(steps.featureSteps(), client, permissions);
        PausedByDayBudgetTestUtils.setAllowingSendingNotifications(
                provider.ppc(client.getShard()),
                List.of(wallet.getCampaignId(), campaign.getCampaignId()),
                permissions
        );
        PausedByDayBudgetTestUtils.markCampaignsReadyForNotification(
                provider.ppc(client.getShard()),
                List.of(wallet.getCampaignId(), campaign.getCampaignId())
        );

        initAndExecuteJobs(client);

        if (needSendNotification) {
            assertThat(sentNotificationsById)
                    .hasSize(1)
                    .containsEntry(wallet.getCampaignId(), PausedByDayBudgetTestUtils.typesByPermissions(permissions));
        } else {
            assertThat(sentNotificationsById).isEmpty();
        }

        provider.ppc(wallet.getShard())
                .delete(WALLET_CAMPAIGNS)
                .where(WALLET_CAMPAIGNS.WALLET_CID.eq(wallet.getCampaignId()))
                .execute();
    }
}
