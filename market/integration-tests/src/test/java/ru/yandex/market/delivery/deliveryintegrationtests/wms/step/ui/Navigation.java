package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import io.qameta.allure.Step;
import java.util.List;
import org.openqa.selenium.WebDriver;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport.TasksWithLocationPage;

@Resource.Classpath({"wms/infor.properties"})
public class Navigation {
    private WebDriver driver;
    private final String baseUrl;

    @Property("infor.host")
    private String host;

    @Property("infor.ui")
    private String ui;

    @Property("infor.ui.query")
    private List<String> query;

    public Navigation (WebDriver driver) {
        this.driver = driver;
        PropertyLoader.newInstance().populate(this);

        String urlQuery = String.join("&", this.query);
        baseUrl = host + ui + "?" + urlQuery;
    }

    public MenuPage menu() {
        driver.get(baseUrl);
        return new MenuPage(driver);
    }

    @Step("Переходим на страницу заданий, связанных с перемещением")
    public TasksWithLocationPage selectTasksWithLocation() {
        driver.get(baseUrl);
        return new MenuPage(driver)
                .clickTasksButton();
    }
}
