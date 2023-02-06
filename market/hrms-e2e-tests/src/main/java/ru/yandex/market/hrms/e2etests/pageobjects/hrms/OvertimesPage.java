package ru.yandex.market.hrms.e2etests.pageobjects.hrms;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;

public class OvertimesPage extends AbstractHrmsPage {

    @FindBy(xpath = "//div[@class='MuiTableContainer-root']")
    private SelenideElement tableContainer;

    @FindBy(xpath = "//button[@type='button']//*[text()='Создать смену']")
    private SelenideElement createShiftButton;

    @FindBy(xpath = "//label[text()='День']/..//input")
    private SelenideElement dayInput;

    public static final String shiftBarXpathPattern = "//button[@title='TIME_PERIOD_PLACEHOLDER']";

    @Override
    protected void checkPageElements() {
        tableContainer.shouldBe(Condition.visible);
        createShiftButton.shouldBe(Condition.visible);
    }

    @Override
    protected String urlCheckRegexp() {
        return "hrms/overtimes";
    }

    @Step("Нажимаем кнопку создания смены")
    public OvertimesCreateShiftPopup createShiftButtonClick() {
        createShiftButton.click();
        return Selenide.page(OvertimesCreateShiftPopup.class);
    }

    @Step("Проверяем, что на странице есть смена с заданным временным промежутком")
    public OvertimesPage checkShiftExists(ZonedDateTime shiftStart, ZonedDateTime shiftEnd) {
        String shiftStartString = shiftStart.format(DateTimeFormatter.ofPattern("dd MMM HH:mm",
                new Locale("ru", "RU")));
        String shiftEndString = shiftEnd.format(DateTimeFormatter.ofPattern("dd MMM HH:mm",
                new Locale("ru", "RU")));

        String period = String.format("%s - %s", shiftStartString, shiftEndString);
        String shiftBarXpath = shiftBarXpathPattern.replaceAll("TIME_PERIOD_PLACEHOLDER", period);

        Allure.addAttachment("Searching by xpath: ", shiftBarXpath);
        highlightElement(Selenide.$(By.xpath(shiftBarXpath)));
        return this;
    }

    @Step("Фильтруем по дню")
    public OvertimesPage filterByDate(ZonedDateTime date) {
        dayInput.setValue(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        return this;
    }
}
