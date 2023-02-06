package ru.yandex.travel.element;

import io.qameta.htmlelements.annotation.FindBy;
import io.qameta.htmlelements.element.ExtendedWebElement;
import io.qameta.htmlelements.element.HtmlElement;

public interface HotelTourRow extends ExtendedWebElement<HotelTourRow> {

    @FindBy(".//div[@class='serp-snippet__operator-name']")
    HtmlElement operator();

    @FindBy(".//div[contains(@class, 'serp-snippet__price-value')]")
    HtmlElement price();

}
