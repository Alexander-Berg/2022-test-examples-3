package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wave.execution.wavemanagement;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class SortingStationSelectPage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(SortingStationSelectPage.class);

    @Name("Кнопка Отправить")
    @FindBy(xpath = "//div[@id = 'A2niw55']")
    private HtmlElement sendButton;

    public SortingStationSelectPage(WebDriver driver) {
        super(driver);
    }

    @Step("Жмем кнопку Отправить")
    public void sendButtonClick() {
        switchToSubWindow();
        sendButton.click();
        switchToMainWindow();
        overlayBusy.waitUntilHidden();
    }

    @Step("Выбираем сортировочную станцию")
    public SortingStationSelectPage selectSortingStation(String sortingStation) {
        String sortingStationCheckboxXpath = "//span[text() = 'sorting_station_placeholder']" +
                "/parent::td/parent::tr/td/input[@type = 'checkbox']";

        sortingStationCheckboxXpath = sortingStationCheckboxXpath
                .replaceFirst("sorting_station_placeholder", sortingStation);

        switchToSubWindow();
        driver.findElement(By.xpath(sortingStationCheckboxXpath)).click();

        return this;
    }
}
