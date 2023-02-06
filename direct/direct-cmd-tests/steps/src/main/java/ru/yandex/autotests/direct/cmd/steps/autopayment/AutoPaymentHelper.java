package ru.yandex.autotests.direct.cmd.steps.autopayment;

import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.WalletCampaignsAutopayMode;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AutopaySettingsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.darkside.model.ScriptParams;
import ru.yandex.autotests.directapi.model.User;

import java.math.BigDecimal;

public class AutoPaymentHelper {

    public static void enableAutopaymentDB(Long walletId, String client) {
        TestEnvironment.newDbSteps().useShardForLogin(client).autopaySettingsSteps()
                .saveDefaultCardAutopaySettings(walletId, Long.valueOf(User.get(client).getPassportUID()),
                        "0", BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), 0);

        TestEnvironment.newDbSteps().walletCampaignsSteps()
                .updateWalletCampaigns(walletId, WalletCampaignsAutopayMode.min_balance);
    }

    public static void setAutoPaymentTriesNum(Long walletId, String client, int num) {
        AutopaySettingsRecord autopaySettings = TestEnvironment.newDbSteps().useShardForLogin(client)
                .autopaySettingsSteps().getAutopaySettings(walletId);
        autopaySettings.setTriesNum(num);
        TestEnvironment.newDbSteps().useShardForLogin(client)
                .autopaySettingsSteps().updateAutopaySettings(autopaySettings);
    }

    public static void deleteAutopaymentDB(Long walletId, String client) {
        TestEnvironment.newDbSteps().useShardForLogin(client).autopaySettingsSteps()
                .deleteAutopaySettings(walletId);

        TestEnvironment.newDbSteps().walletCampaignsSteps()
                .updateWalletCampaigns(walletId, WalletCampaignsAutopayMode.none);
    }

    public static void runAutoPaymentScript(DirectCmdRule cmdRule, Long walletId, String client) {
        int shard = TestEnvironment.newDbSteps().shardingSteps().getShardByLogin(client);
        cmdRule.darkSideSteps().getRunScriptSteps()
                .runScript("ppcProcessAutoPayments.pl", new ScriptParams()
                        .withShardId(shard)
                        .withCustomParam("--wallet-cid", walletId.toString()).toString());
    }
}
