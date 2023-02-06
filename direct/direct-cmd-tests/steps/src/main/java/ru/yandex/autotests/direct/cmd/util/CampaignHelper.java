package ru.yandex.autotests.direct.cmd.util;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;

import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PhrasesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;

import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class CampaignHelper {
    //Для запуска инстанса скрипта, который копирует кампании
    private static final String PAR_ID = "copy";

    private static final ConditionFactory CAMPAIGN_COPY_CONDITION = Awaitility
            // интервал запуска скрипта на ТС - 10 минут (если он не запущен)
            .with().timeout(new Duration(13L, TimeUnit.MINUTES))
            .and().with().pollDelay(Duration.TEN_SECONDS)
            // интервал сна между итерациями скрипта (если нет заданий) - 30 секунд
            .and().with().pollInterval(new Duration(30L, TimeUnit.SECONDS))
            .await("ppcCampQueue processed copy task");

    public static Long copyCampaignsByScript(DirectCmdRule cmdRule, String login, Long campaignId) {
        String conf = cmdRule.darkSideSteps().directEnvSteps().getSettingsName();
        Long clientId = cmdRule.dbSteps().shardingSteps().getClientIdByLogin(login);
        Supplier<Long> newCid = () -> cmdRule.dbSteps().useShardForClientId(clientId)
                .campaignsSteps()
                .findCampaignCopy(clientId, campaignId);
        if (conf.equals("test")) {
            CAMPAIGN_COPY_CONDITION.until(() -> newCid.get() != null);
        } else {
            int shard = cmdRule.darkSideSteps().getClientFakeSteps().getUserShard(login);
            cmdRule.darkSideSteps().getRunScriptSteps().runPpcCampQueue(shard, campaignId.intValue(), PAR_ID);
        }
        Long result = newCid.get();
        assumeThat("Кампания " + campaignId + " успешно скопирована", result, notNullValue());
        return result;
    }

    public static void deleteAdGroupMobileContent(Long campaignId, String ulogin) {
        if (campaignId == null) {
            return;
        }
        List<Long> groupIds =
                TestEnvironment.newDbSteps().useShardForLogin(ulogin).adGroupsSteps().getPhrasesByCid(campaignId)
                        .stream()
                        .map(PhrasesRecord::getPid)
                        .collect(Collectors.toList());
        TestEnvironment.newDbSteps().useShardForLogin(ulogin).adGroupsSteps().deleteAdGroupMobileContent(groupIds);
    }
}
