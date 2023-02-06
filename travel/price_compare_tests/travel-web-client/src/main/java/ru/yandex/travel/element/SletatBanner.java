package ru.yandex.travel.element;

import io.qameta.htmlelements.annotation.FindBy;
import io.qameta.htmlelements.element.ExtendedWebElement;
import io.qameta.htmlelements.element.HtmlElement;

public interface SletatBanner extends ExtendedWebElement<SletatBanner> {

    @FindBy(".//span[contains(@class,'uis-popup__close')]")
    HtmlElement closeButton();

}
