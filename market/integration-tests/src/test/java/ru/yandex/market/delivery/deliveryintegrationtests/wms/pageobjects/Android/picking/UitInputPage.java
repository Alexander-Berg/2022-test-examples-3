package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.Android.picking;

import java.util.List;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.Android.AbstractAndroidInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;

public class UitInputPage extends AbstractAndroidInputPage {
    @FindBy(xpath = "//android.widget.TextView[@content-desc='lot']")
    private SelenideElement lot;
    @FindBy(xpath = "//android.widget.TextView[@content-desc='location']")
    private SelenideElement cell;

    @Step("Вводим уит товара")
    public DropContainerInputPage enterUit(List<String> serialNumbers) {

        String uit = DatacreatorSteps.Items().getItemSerialByLocLot(
                StringUtils.substringAfter(cell.getText(), "Ячейка:").trim(),
                "%" + StringUtils.substringAfter(lot.getText(), "Партия:").trim()
        );
        super.performInput(uit);
        serialNumbers.add(uit);

        return new DropContainerInputPage();
    }
}
