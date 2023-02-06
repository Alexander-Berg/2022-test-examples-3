package ru.yandex.autotests.direct.cmd.util;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.editAdGroupsPerformance.GetPerformanceGroup;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.direct.httpclient.util.CommonUtils.sleep;
import static ru.yandex.autotests.direct.httpclient.util.beanmapper.BeanMapper.map2;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class PerformanceCampaignHelper {

    private static final ConditionFactory DELETE_CAMPAIGN_CONDITION = Awaitility
            // интервал запуска скрипта на ТС - 10 минут (если он не запущен)
            .with().timeout(new Duration(13L, TimeUnit.MINUTES))
            .and().with().pollDelay(Duration.TEN_SECONDS)
            // интервал сна между итерациями скрипта (если нет заданий) - 30 секунд
            .and().with().pollInterval(new Duration(30L, TimeUnit.SECONDS))
            .await("кампания удалена скриптом ppcCampQueue");

    public static void waitForFeedLoad(DirectCmdSteps cmdSteps, String clientId, String feedId) {
        final int ATTEMPTS = 40;
        final int TIMEOUT = 2_000;
        int currentAttempt = ATTEMPTS;
        DarkSideSteps darkSideSteps = new DarkSideSteps(cmdSteps.context().getProperties());
        int shardId = TestEnvironment.newDbSteps().shardingSteps().getShardByClientID(Long.valueOf(clientId));
        darkSideSteps.getRunScriptSteps().runPpcFeedToBannerLand(shardId, new Long[]{Long.valueOf(feedId)},
                new String[]{clientId});
        while (currentAttempt > 0) {
            FeedsRecord feed = TestEnvironment.newDbSteps().useShard(shardId).feedsSteps()
                    .getFeed(Long.valueOf(feedId), clientId);
            if (FeedsUpdateStatus.New.equals(feed.getUpdateStatus()) || FeedsUpdateStatus.Updating
                    .equals(feed.getUpdateStatus()))
            {
                sleep(TIMEOUT);
            } else {
                break;
            }
            currentAttempt--;
        }
        assertThat("Исчерпано кол-во попыток ожидания обновления фида", currentAttempt, not(lessThanOrEqualTo(0)));
    }

    @Step("Удаляем из базы данные о кампании {2} у клиента {1}")
    public static void runDeleteCampaignScriptAndIgnoreResult(DirectCmdRule cmdRule, Long clientId, Long campaignId) {
        runDeleteCampaignScript(cmdRule, clientId, campaignId, false);
    }

    @Step("Удаляем из базы данные о кампании {2} у клиента {1} и проверяем успешность")
    public static void runDeleteCampaignScriptAndCheckResult(DirectCmdRule cmdRule, Long clientId, Long campaignId) {
        runDeleteCampaignScript(cmdRule, clientId, campaignId, true);
    }

    private static void runDeleteCampaignScript(DirectCmdRule cmdRule, Long clientId, Long campaignId,
                                                boolean checkResult) {
        String conf = cmdRule.darkSideSteps().directEnvSteps().getSettingsName();
        int shardId = cmdRule.dbSteps().shardingSteps().getShardByClientID(clientId);
        DirectJooqDbSteps dbSteps = cmdRule.dbSteps().useShard(shardId);
        BooleanSupplier deleted = () -> dbSteps.campaignsSteps().getCampaignById(campaignId) == null;
        if (conf.equals("test")) {
            if (checkResult) {
                // при массовом прогоне тестов очередь может быть забита, поэтому делаем себе преференцию
                dbSteps.campOperationsQueueSteps().moveBack(campaignId);
                // на ТС скрипт штатно запущен, ждем
                DELETE_CAMPAIGN_CONDITION.until(deleted::getAsBoolean);
            }
        } else {
            cmdRule.darkSideSteps().getRunScriptSteps().runPpcCampQueue(shardId, campaignId.intValue(), "rest");
            if (checkResult) {
                assumeThat("Не удалось дождаться удаления кампании после запуска скрипта PpcCampQueue",
                        deleted.getAsBoolean(), is(true));
            }
        }
    }

    public static void deleteCreativeQuietly(String client, Long creativeId) {
        try {
            TestEnvironment.newDbSteps().useShardForLogin(client).perfCreativesSteps()
                    .deletePerfCreatives(creativeId);
        } catch (Exception ignored) {
        }
    }

    public static List<Group> mapEditGroupResponseToSaveRequest(List<GetPerformanceGroup> response) {
        return response.stream()
                .map(t -> map2(t, new Group()))
                .collect(toList());
    }
}
