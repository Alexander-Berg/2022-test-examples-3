package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking;

import java.util.List;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.qatools.htmlelements.annotations.Name;

public class MoveItemPage extends AbstractInputPage {

    private static final String NUMBER_OF_ITEMS =
            "//span[text()[starts-with(., 'Отобрано')]]";

    @Name("Ячейка")
    @FindBy(xpath = "//span[@data-e2e='from-loc']")
    private SelenideElement cell;

    @Name("Партия")
    @FindBy(xpath = "//span[@data-e2e='info-lot']")
    private SelenideElement batch;

    @Name("Количество товаров")
    @FindBy(xpath = NUMBER_OF_ITEMS)
    private SelenideElement numberOfItems;

    public MoveItemPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим ШК товара")
    public CartInputPage enterSerialNumber(List<String> serialNumbers) {
        String serialNumber = DatacreatorSteps.Items().getItemSerialByLocLot(
                cell.getText().trim(),
                "%" + batch.getText().trim()
        );
        super.performInput(serialNumber);
        serialNumbers.add(serialNumber);
        return new CartInputPage(driver);
    }

    @Override
    protected String getUrl() {
        return "moveItemPage$";
    }

    public int getAmountToPickFromThisCell() {
        return Integer.parseInt(StringUtils.substringAfter(numberOfItems.getText(), "из").trim());
    }
}
