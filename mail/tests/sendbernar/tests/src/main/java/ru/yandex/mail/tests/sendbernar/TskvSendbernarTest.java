package ru.yandex.mail.tests.sendbernar;

import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.execute.Execute;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.properties.IgnoreSshTest;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.mail.common.user_journal.generated.UserJournalType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.mail.things.matchers.TSKVLogMatcher.entry;
import static ru.yandex.mail.things.matchers.TSKVLogMatcher.logEntryShouldMatch;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.timeoutHasExpired;
import static ru.yandex.qatools.htmlelements.matchers.decorators.MatcherDecoratorsBuilder.should;



@Aqua.Test
@Title("[LOGS] TSKV лог для sendbernar")
@Description("tskv лог - " + TskvSendbernarTest.TSKV_LOG_PATH)
@Stories({"user_journal", "TSKV", "logs"})
@IgnoreSshTest
public class TskvSendbernarTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.tskvSendbernar;
    }

    static final String TSKV_LOG_PATH = "var/log/sendbernar/user_journal.tskv";

    @ClassRule
    public static Execute execute = new Execute()
            .withUsername("robot-gerrit")
            .withHost(SendbernarProperties.properties().sendbernarUri())
            .withKey("-----BEGIN RSA PRIVATE KEY-----\n" +
                    "MIIEowIBAAKCAQEAy5konJWOb9XYJtzC1nNOiS8kh0X5zrZoK3uKVOxGSdRV+4ih\n" +
                    "dvC1SL2vz4ttRCcCzjoHXn63xj0vHOiHbTo84bB8W9BLFy+p6TkU4+CQcGecavpJ\n" +
                    "0XfdRX4Pr7yBVJuPCpHs81IriBIZX4WzK2Gr2rnXuu/aO0EafFFDQxoyGDpiDuNf\n" +
                    "2iAKr0SHok+3HjHw80dGFtywvnQbEZSZxel15Z4Lmj8mOzKTg/i+CetzwcgIWjzJ\n" +
                    "BKDvkMRVVwAQy83GFhUETA/65Ksxd57ovORvgA3yIwlADcXbUqUo3UYUv1ujWnBQ\n" +
                    "DQgge4PVCGDp3xuGZ4pVRykrOeWcF0nF3/3u1QIBIwKCAQEAi5w5Guo1yQeqKUbs\n" +
                    "AMQYmJVa4Gp/aSyfM8JtfA+7K0/F7k8PogQkiaakcQ8mWp5oUuX2bK6p5wViInOQ\n" +
                    "EGJy4+a7rKwWO8jpiftBhlDYEpAwvmJ7wtXZjrzXjnKh053ebaXkTxPGFCmq/6wj\n" +
                    "Fm7cPjZKyVQDW9Te9ikYLgNVjPObhbcp0GxSjYrxkbOSAOmZFeTun7P5B4po/wHC\n" +
                    "N9ztMU8nV2Ynz8rZKjMrEb4cVp1+jI3o8rCAp80BrQxrsOfTHjlfBSQCjUWpcJwp\n" +
                    "a3QPvhwYD3Qn2WcRs1pNuLPRmW8setmGUeG53nXsuZNWismmyRT/3lJHtjj9OwLP\n" +
                    "PjAmiwKBgQD2VaYnPiy/uYXUrMRVmL6TuaEL+fM4c2IDxIz2v8pByRoLMjb0IlHs\n" +
                    "8/lifvqO5fM/2lz8Fg4++Oyu4an8GF0rIzMZqz8OMHoQWrsiIu6/8B/OsJpyC4QI\n" +
                    "2vfkyWbA1VS2B++kOWVBmCAmR37pHJwPcQ8dXisj1Llcd7sWMdETDQKBgQDTljzG\n" +
                    "S//9PKloARAsmXQIpGMIyVhXyr2Tg8+9H1JaNJs8XeiNXy4p3R9nZmZQ3YRv8uYu\n" +
                    "jKafoyIEA6P3pskxvCOVY/xo+4DfWKq03UHDRVqAW+VpOouDUh7oES5s6kTMdK4d\n" +
                    "RuRafve1h5XNOBlju+dCxMYJe254/tVBc3v46QKBgQC+B47N0OCxKLeysSm+X+N5\n" +
                    "Rg6FlO7TxrlTXRxJUh+vF3p9qmToN7uK6BgnaUTx5JcUADkaPOZrF8yG6JkS7jk+\n" +
                    "iN5HAHJ4qQ202Ec3l0p2zy564AIddpkOJULjsU9ETMxR6N1wDwT4FkSv0MD8+NAp\n" +
                    "K1TNgyiX+99zN8rWm3VmdwKBgBIi0gJeSSRVqB7bhQsjGJMGxqj7ULcRYLTfaZPe\n" +
                    "HQBq6LwIDJ5nPnid7L+4UemsllK9DGpj08SKU19uBr10sijVnKZnp+u9yTe3JJMo\n" +
                    "6GEx1I6oyoVcyiEy7LTORc7SQGlL1Gjo0b6dKyzRG3f9foTkOGTPCakKlHDFYrx3\n" +
                    "nOlzAoGBAIpOrY2q65sCwvNINN9TUk+/OkbB79N3/rA7VGQQDtQjMdylWFQWZP/B\n" +
                    "bF2bAe+0V6uVL4HpGhnpWbJMZD6fA9G6wEzcqKZH025/XwTzrot33cetTa15eq2V\n" +
                    "QoRETwVyYvwx4vt6bB4vs+cV9sml8a1OYqkvc8QLsMyOJyF7B1ym\n" +
                    "-----END RSA PRIVATE KEY-----");

    @Test
    @Issues({@Issue("MPROTO-1871"), @Issue("MAILDEV-8")})
    @Title("Должны писать message-id при send операции")
    public void shouldReturnMessageIdOnSend() {
        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        waitWith.inbox().subj(subj).waitDeliver();

        List<String> greps = Arrays.asList(getUid(), subj);
        shouldSeeLogLine(entry(UserJournalType.STATE, startsWith("message-id=<")), greps);
    }

    @Step("[SSH]: Должны увидеть в логе {0}, грепая по {1}")
    private static void shouldSeeLogLine(Matcher<Map<? extends String, ? extends String>> matcher,
                                         List<String> greps) {
        long timeoutMs = 3000;
        String path = SendbernarProperties.properties().relativePath(TSKV_LOG_PATH);
        assertThat(
                execute,
                should(
                    logEntryShouldMatch(matcher, path, greps, timeoutMs)
                ).whileWaitingUntil(timeoutHasExpired(timeoutMs).withPollingInterval(500))
        );
    }
}

