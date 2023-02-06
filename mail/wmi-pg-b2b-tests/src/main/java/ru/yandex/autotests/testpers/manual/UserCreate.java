package ru.yandex.autotests.testpers.manual;

import com.jayway.restassured.RestAssured;
import org.apache.log4j.LogManager;
import ru.yandex.autotests.passport.api.tools.registration.RegUser;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;

/**
 * @author lanwen (Merkushev Kirill)
 *         Date: 03.06.15
 */
public class UserCreate {
    static {
        RestAssured.port = 80;
    }

    @Step("Включаем прокси через comtest.zombie.yandex-team.ru")
    public static void enableProxy() {
        System.setProperty("proxySet", "true");
        System.setProperty("http.proxyHost", "comtest.zombie.yandex-team.ru");
        System.setProperty("http.proxyPort", "3128");
    }

    @Step("Выключаем прокси")
    public static void disableProxy() {
        System.setProperty("proxySet", "false");
        System.setProperty("http.proxyHost", "");
        System.setProperty("http.proxyPort", "");
    }

    public static void enableProxyIf(boolean cond) {
        if (cond) {
            enableProxy();
        }
    }

    public static void disableProxyIf(boolean cond) {
        if (cond) {
            disableProxy();
        }
    }

    public static RegUser createNewUser() throws InterruptedException {
        int tryCount = 2;
        int attempt = 0;

        Throwable whatHappens = null;

        while (attempt < tryCount) {
            try {
                enableProxyIf(props().isLocalDebug());
                attempt++;
                return RegUser.register().common();
            } catch (Throwable t) {
                LogManager.getLogger(UserCreate.class)
                        .error(format("Юзер не создан с %s/%s попытки", attempt, tryCount));
                LogManager.getLogger(UserCreate.class).error(t);
                t.printStackTrace();
                
                Thread.sleep(TimeUnit.SECONDS.toMillis(3));
                whatHappens = t;
            } finally {
                disableProxyIf(props().isLocalDebug());
            }
        }
        throw new RuntimeException(format("Не смогли создать пользователя за %s попыток(ки)", tryCount), whatHappens);
    }

    public static RegUser createNewUserInTestEnvironment(String login, String suffix) throws InterruptedException {
        System.setProperty("base.scheme", "test");
        RegUser regUser = createNewUser();
        System.setProperty("base.scheme", "");
        return regUser;
    }
}
