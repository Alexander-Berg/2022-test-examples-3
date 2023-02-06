package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.ingoing;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class AproveClosePuoPage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(AproveClosePuoPage.class);

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//input[@id = '$4kv82r_filterbutton']")
    private HtmlElement filterButton;

    @Name("Поле фильтрации по номеру документа")
    @FindBy(xpath = "//input[@attribute='Sourcekey']")
    private HtmlElement docNumberField;

    @Name("Чекбокс напротив первой строки в таблице")
    @FindBy(xpath = "//input[@id = '$ahinsl_rowChkBox_0']")
    private HtmlElement firstCheckbox;

    public AproveClosePuoPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим номер ПУО приемки")
    public void inputInboundId(String inboundId) {
        docNumberField.sendKeys(inboundId);
        overlayBusy.waitUntilHidden();
    }

    @Step("Запускаем фильтрацию")
    public void filterButtonClick() {
        filterButton.click();
        overlayBusy.waitUntilHidden();
    }

    @Step("Выбираем первый результат в результатах фильтрации")
    public void selectFirstResult() {
        firstCheckbox.click();
    }

}
