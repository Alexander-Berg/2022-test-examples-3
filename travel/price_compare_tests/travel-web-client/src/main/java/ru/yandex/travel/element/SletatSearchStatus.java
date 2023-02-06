package ru.yandex.travel.element;

import io.qameta.htmlelements.annotation.FindBy;
import io.qameta.htmlelements.element.ExtendedWebElement;
import io.qameta.htmlelements.element.HtmlElement;

public interface SletatSearchStatus extends ExtendedWebElement<SletatSearchStatus> {


    @FindBy(".//span[@class='search-status__text-bold']")
    HtmlElement percents();

    @FindBy(".//input[contains(@class, 'uis-button_search-status')]")
    HtmlElement button();

}
