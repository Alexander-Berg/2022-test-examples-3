package ru.yandex.travel.element;

import io.qameta.htmlelements.annotation.FindBy;
import io.qameta.htmlelements.element.ExtendedWebElement;
import io.qameta.htmlelements.element.HtmlElement;

public interface DestinationTourRow extends ExtendedWebElement<DestinationTourRow> {

    @FindBy(".//div[contains(@class, 'serp-snippet__price-value')]")
    HtmlElement price();

    @FindBy(".//a[contains(@class, 'serp-snippet__title')]")
    HtmlElement title();

}
