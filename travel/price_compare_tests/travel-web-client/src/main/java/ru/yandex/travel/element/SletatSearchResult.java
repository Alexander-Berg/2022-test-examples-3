package ru.yandex.travel.element;

import io.qameta.htmlelements.annotation.FindBy;
import io.qameta.htmlelements.element.ExtendedWebElement;
import io.qameta.htmlelements.element.HtmlElement;

public interface SletatSearchResult extends ExtendedWebElement<SletatSearchResult> {

    @FindBy(".//div[@class='search-result__short-price']//span[contains(@class, 'sr-currency-rub')]")
    HtmlElement price();

    @FindBy(".//img[@class='search-result-operator-logo']")
    HtmlElement operatorLogo();
}
