package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.LoginPage;

import java.util.concurrent.TimeUnit;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.SeleniumUtil.clearSession;

@Resource.Classpath({"wms/infor.properties"})
public class Login extends AbstractWSSteps {
    private final LoginPage loginpage;

    private final Logger log = LoggerFactory.getLogger(Login.class);

    @Property("infor.host")
    private String host;

    @Property("infor.workstation")
    private String workstation;

    @Property("infor.equipid")
    private String equipid;

    private final String wsHost;
    private final User defaultUser;

    public Login(WebDriver drvr, User defaultUser) {
        super(drvr);

        wsHost = host+workstation;
        loginpage = new LoginPage(driver);
        this.defaultUser = defaultUser;
    }

    public void PerformLogin() {
        PerformLogin(defaultUser);
    }

    private void PerformLogin(User user) {
        PerformLogin(user.getLogin(), user.getPass());
    }

    private void PerformLogin(String username, String password) {
        PerformLogin(this.wsHost, username, password);
    }

    @Step("Логин на рабочую станцию")
    private void PerformLogin(String host, String username, String password) {

        log.info("Performing login to: {}, user: {}", host, username);

        Retrier.retry(() -> {
            clearSession(driver);
            driver.get(host);
            loginpage.logIn(username, password);

            Assertions.assertTrue(topMenu.isDisplayed());
        },
                Retrier.RETRIES_SMALL,
                10,
                TimeUnit.SECONDS);

        log.info("WS logon successful.");
    }
}
