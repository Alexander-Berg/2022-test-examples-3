package ru.yandex.travel.element;

import io.qameta.htmlelements.annotation.FindBy;
import io.qameta.htmlelements.element.ExtendedWebElement;
import io.qameta.htmlelements.element.HtmlElement;

public interface HotelRoomRow extends ExtendedWebElement<HotelTourRow> {

    @FindBy(".//div[@class='serp-snippet__partner-name']")
    HtmlElement partner();

    @FindBy(".//div[@class='serp-snippet__price-value']")
    HtmlElement price();

}
