package ru.yandex.travel.element;

import io.qameta.htmlelements.annotation.FindBy;
import io.qameta.htmlelements.annotation.Param;
import io.qameta.htmlelements.element.ExtendedWebElement;
import io.qameta.htmlelements.element.HtmlElement;
import org.openqa.selenium.WebElement;

public interface SletatFormRadio extends ExtendedWebElement<SletatFormRadio>{

    default void select(String name) {
        button(name).waitUntil(WebElement::isDisplayed).click();
    }

    @FindBy(".//button[contains(.,'{{ name }}')]")
    HtmlElement button(@Param("name") String name);

}
