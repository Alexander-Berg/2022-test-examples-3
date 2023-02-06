package ru.yandex.travel.element;

import io.qameta.htmlelements.annotation.FindBy;
import io.qameta.htmlelements.element.ExtendedList;
import io.qameta.htmlelements.element.ExtendedWebElement;
import io.qameta.htmlelements.element.HtmlElement;

public interface DestinationToursSection extends ExtendedWebElement<DestinationToursSection> {

    @FindBy(".//ul[contains(@class, 'serp__list_type_search')]//li")
    ExtendedList<DestinationTourRow> toursList();

    @FindBy(".//div[contains(@class, 'serp__progress_in-progress_yes')]")
    HtmlElement inProgress();

}
