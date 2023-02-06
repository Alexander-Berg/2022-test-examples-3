package ru.yandex.autotests.direct.cmd.daybuget.wallet;

import java.math.BigDecimal;

import ru.yandex.autotests.direct.cmd.data.commons.campaign.DayBudget;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampOptionsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.model.User;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

public class WalletDayBudgetHelper {

    public static void clearDayBudget(DirectCmdRule cmdRule, Long walletId, String client) {
        cmdRule.cmdSteps().campaignSteps().setDayBudget(walletId, new DayBudget().withSet(false), client);

        CampaignsRecord expectedCamp =
                new CampaignsRecord().setDayBudget(BigDecimal.valueOf(0, 2)).setDayBudgetShowMode(null);

        CampaignsRecord campRecord = TestEnvironment.newDbSteps().useShardForLogin(client).campaignsSteps()
                .getWallet(Long.valueOf(User.get(client).getClientID()));
        assumeThat("дневной бюджет отключен", campRecord.intoMap(),
                beanDiffer(expectedCamp.intoMap()).useCompareStrategy(onlyExpectedFields()));
        resetDayBudgetCount(walletId, client);
    }

    private static void resetDayBudgetCount(Long walletId, String client) {
        CampOptionsRecord campOptions =
                TestEnvironment.newDbSteps().useShardForLogin(client).campaignsSteps().getCampOptionsById(walletId);
        campOptions.setDayBudgetDailyChangeCount(0);
        TestEnvironment.newDbSteps().useShardForLogin(client).campaignsSteps().updateCampOptions(campOptions);
    }

}
