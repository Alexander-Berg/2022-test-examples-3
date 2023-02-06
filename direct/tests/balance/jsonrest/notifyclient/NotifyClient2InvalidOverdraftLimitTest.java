package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyclient;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.common.api45mng.APIPort_PortType;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyClient2JSONRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.User;
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
@Aqua.Test(title = "NotifyClient2 - должен выдавать ошибку при отрицательном/невалидном OverdraftLimit")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.JSON_BALANCE_NOTIFY_CLIENT2)
@Issue("https://st.yandex-team.ru/DIRECT-37012")
public class NotifyClient2InvalidOverdraftLimitTest {

    protected static LogSteps log = LogSteps.getLogger(NotifyClient2InvalidOverdraftLimitTest.class);

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private static Long clientID;

    @ClassRule
    public static ApiSteps api = new ApiSteps().wsdl(APIPort_PortType.class).as(Logins.LOGIN_SUPER);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();


    @BeforeClass
    public static void before() {
        clientID = Long.valueOf(User.get(Logins.LOGIN_YNDX_FIXED).getClientID());
    }


    @Test
    public void notifyClientWithExistOverdraftLimitTest() {
        log.info("Вызываем метод NotifyClient с пустым OverdraftLimit");
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientExpectErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTimestamp(),
                400, 1010, "Overdraft limit from balance: undef, must be greater than zero"
        );
    }

    @Test
    public void notifyClientWithNegativeOverdraftLimitTest() {
        Float negativeOverdraftLimit = -10f;
        String errorText = String.format(
                "Overdraft limit from balance: %s, must be greater than zero", negativeOverdraftLimit.intValue());

        log.info("Вызываем метод NotifyClient с отрицательным OverdraftLimit");
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientExpectErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientID)
                        .withTimestamp()
                        .withOverdraftLimit(negativeOverdraftLimit),
                400, 1010, errorText
        );
    }
}
