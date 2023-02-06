package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receivingAdmin;

import java.util.Arrays;
import java.util.List;

import com.codeborne.selenide.SelenideElement;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class DiscrepanciesReport extends AbstractPage {

    @FindBy(xpath = "(//span[text()='№ отправления в системе заказчика'])" +
            "[last()]/../../following-sibling::tr/td[3]/span")
    private SelenideElement boxId;
    @FindBy(xpath = "(//span[text()='Атрибут качества'])" +
            "[last()]/../../following-sibling::tr/td[4]/span")
    private SelenideElement discrepancies;
    @FindBy(xpath = "//span[text()='Недостача']")
    private SelenideElement boxShortage;

    public DiscrepanciesReport (WebDriver driver) {
        super(driver);
        wait.until(urlMatches("report/discrepancies"));
    }

    public DiscrepanciesReport checkBoxDiscrepancies(String expectedBoxId, List<String> expectedDiscrepancies) {
        final String actualBoxId = boxId.getText();
        List<String> actualDiscrepanciesList = Arrays.asList(
                discrepancies.getText().split(", ")
        );

        Assertions.assertEquals(expectedBoxId, actualBoxId);
        Assertions.assertEquals(expectedDiscrepancies, actualDiscrepanciesList);

        return this;
    }

    public DiscrepanciesReport verifyNoShortageInReport() {
        boxShortage.shouldBe(hidden);

        return this;
    }

    public DiscrepanciesReport verifyShortageInReport() {
        boxShortage.shouldBe(visible);

        return this;
    }
}
