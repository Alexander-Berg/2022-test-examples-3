package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.Android.picking;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.Android.AbstractAndroidInputPage;

import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$$;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CartAddingPage extends AbstractAndroidInputPage {
    private static final String ADDED_CART_TPL =
            "//android.view.View[@content-desc='added_cart: ADDED_CART status: Пустой']";

    @FindBy(xpath = "//android.view.View[@content-desc='wms_button']")
    private SelenideElement forwardButton;

    @Step("Добавляем тару для отбора - {cart}")
    public CartAddingPage addCart(String cart) {
        super.performInput(cart);
        return this;
    }

    @Step("Проверяем, что тара {cart} была добавлена для отбора")
    public CartAddingPage verifyCartWasAdded(String cart) {
        final By addedCart = byXpath(ADDED_CART_TPL.replaceFirst("ADDED_CART", cart));
        assertTrue($$(addedCart).size() > 0,
                "Введенная тара не добавилась в назначение");

        return this;
    }

    @Step("Жмем кнопку \"Далее\"")
    public UitInputPage clickForwardButton() {
        hideKeyboard();
        forwardButton.click();
        return new UitInputPage();
    }
}
