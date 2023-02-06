package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.tsd;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common.WarningDialog;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.login.EquipSelectPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.login.LoginPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.login.WHSelectPage;

import java.util.concurrent.TimeUnit;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.SeleniumUtil.clearSession;

@Resource.Classpath({"wms/infor.properties"})
public class Login {
    private WebDriver driver;
    private LoginPage loginpage;
    private WHSelectPage whSelectPage;
    private EquipSelectPage equipSelectPage;
    private WarningDialog warningDialog;

    private final Logger log = LoggerFactory.getLogger(Login.class);

    @Property("infor.host")
    private String host;

    @Property("infor.tsd")
    private String tsd;

    @Property("infor.equipid")
    private String equipid;

    private String tsdHost;
    private final User defaultUser;

    public Login(WebDriver drvr, User defaultUser) {
        PropertyLoader.newInstance().populate(this);

        this.driver = drvr;

        tsdHost = host+ tsd;
        this.defaultUser = defaultUser;

        loginpage = new LoginPage(driver);
        whSelectPage = new WHSelectPage(driver);
        equipSelectPage = new EquipSelectPage(driver);
        warningDialog = new WarningDialog(driver);
    }

    public void PerformLogin() {
        PerformLogin(defaultUser);
    }

    private void PerformLogin(User user) {
        PerformLogin(this.tsdHost, user.getLogin(), user.getPass());
    }

    @Step("Логинимся на ТСД")
    private void PerformLogin(String host, String username, String password) {

        log.info("Performing login to: {}, username: {}", host, username);

        Retrier.retry(() -> {
            clearSession(driver);
            driver.get(host);
            loginpage.logIn(username, password);
            Assertions.assertTrue(whSelectPage.isDisplayed());
        },
                Retrier.RETRIES_SMALL,
                10,
                TimeUnit.SECONDS);
        whSelectPage.SelectWarehouse();

        if (warningDialog.IsPresent("Вы работаете более 24-х часов. Хотите сначала выйти из системы?")) {
            log.info("Found 24h login warning");
            warningDialog.clickOk();
        }

        if (warningDialog.IsPresent("Предупреждение")) {
            log.info("Found warning dialog");
            warningDialog.clickOk();
        }

        equipSelectPage.EnterEquipId(equipid);

        log.info("RF logon successful.");
    }
}
