package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.Android.picking;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.Android.AbstractAndroidInputPage;

public class LocationInputPage extends AbstractAndroidInputPage {

    @FindBy(xpath = "//android.view.View[@content-desc='location']")
    private SelenideElement cell;

    @Step("Вводим номер предложенной ячейки")
    public UitInputPage enterCellId() {
        String cellId = StringUtils.substringAfter(cell.getText(), "Ячейка:").trim();
        super.performInput(cellId);
        return new UitInputPage();
    }
}
