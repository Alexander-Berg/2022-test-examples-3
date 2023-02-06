package ru.yandex.travel.element;

import io.qameta.htmlelements.annotation.FindBy;
import io.qameta.htmlelements.element.HtmlElement;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface SletatOperatorChecked extends HtmlElement {

    @FindBy(".//label[contains(@class, 'uis-checkbox__label_checked')]")
    HtmlElement name();

    @FindBy(".//span[@class='sr-currency-rub']")
    HtmlElement price();
}
