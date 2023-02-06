package ru.yandex.market.hrms.e2etests.pageobjects.hrms;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.support.FindBy;

public class ProductionCalendarPage extends AbstractHrmsPage {

    @FindBy(xpath = "//div[contains(@class, 'styles_tableContainer')]")
    SelenideElement tableContainer;

    @Override
    protected void checkPageElements() {
        tableContainer.shouldBe(Condition.visible);
    }

    @Override
    protected String urlCheckRegexp() {
        return "/hrms/production-calendar";
    }
}
