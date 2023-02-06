package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.auth;


import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;

@DisplayName("Selenium: Login test")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/test.properties", "wms/infor.properties"})
public class LoginTest extends AbstractUiTest {

    @Property("infor.host")
    private String host;

    @Property("infor.ui")
    private String ui;

    public LoginTest() {
        PropertyLoader.newInstance().populate(this);
    }

    @RetryableTest
    @DisplayName("TSD логин тест")
    @ResourceLock("TSD логин тест")
    public void TsdLoginTest() {
        tsdSteps.Login().PerformLogin();
    }

    @RetryableTest
    @DisplayName("WS логин тест")
    @ResourceLock("WS логин тест")
    public void WsLoginTest() {
        wsSteps.Login().PerformLogin();
    }

    @RetryableTest
    @DisplayName("UI логин тест")
    @ResourceLock("UI логин тест")
    public void UiLoginTest() {
        String url = host + ui;
        uiSteps.Login().PerformLogin(url);
        new MenuPage(driver).inputReceivingPath();
    }
}
