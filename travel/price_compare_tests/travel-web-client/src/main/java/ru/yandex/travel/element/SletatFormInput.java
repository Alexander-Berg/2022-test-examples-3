package ru.yandex.travel.element;

import io.qameta.htmlelements.annotation.FindBy;
import io.qameta.htmlelements.annotation.Param;
import io.qameta.htmlelements.element.ExtendedWebElement;
import io.qameta.htmlelements.element.HtmlElement;

public interface SletatFormInput extends ExtendedWebElement<SletatFormInput>{

    default void select(String name) {
        input().click();
        input().clear();
        input().sendKeys(name);
        suggest(name).click();
    }

    @FindBy(".//fieldset/input")
    HtmlElement input();

    @FindBy(".//div[contains(@class, 'uis-scrollbar')]//ul/li[contains(.,'{{ name }}')]")
    HtmlElement suggest(@Param("name") String name);

}
