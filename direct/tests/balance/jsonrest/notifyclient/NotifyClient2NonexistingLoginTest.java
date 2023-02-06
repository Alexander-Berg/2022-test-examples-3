package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyclient;

import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyClient2JSONRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

/**
 * Created by semkagtn on 06.04.15.
 * https://st.yandex-team.ru/TESTIRT-4898
 */
@Aqua.Test(title = "NotifyClient2 - несуществующий логин")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_CLIENT2)
@Issue("https://st.yandex-team.ru/DIRECT-37012")
public class NotifyClient2NonexistingLoginTest {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected static LogSteps log = LogSteps.getLogger(NotifyClient2NonexistingLoginTest.class);

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Test
    public void notifyClientWithNonexistingLoginTest() {
        log.info("Вызываем метод NotifyClient с несуществующим логином.");
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientExpectErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(11111111111L)
                        .withTimestamp()
                        .withOverdraftLimit(100.0f)
                        .withOverdraftSpent(150.0f),
                200, 0, "ClientID 11111111111 is not known"
        );
    }

    @Test
    public void notifyClientWithNegativeLoginTest() {
        log.info("Вызываем метод NotifyClient с несуществующим логином.");
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientExpectErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(-1L)
                        .withTimestamp()
                        .withOverdraftLimit(100.0f)
                        .withOverdraftSpent(150.0f),
                500, -32603, "ClientID must be greater than 0"
        );
    }
}
