package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common;

import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;

public abstract class ItemContext extends AbstractPage {

    public final Item item;

    public ItemContext(WebDriver driver, Item item) {
        super(driver);
        this.item = item;
    }
}
