package ru.yandex.travel.element;

import io.qameta.htmlelements.annotation.FindBy;
import io.qameta.htmlelements.element.ExtendedList;
import io.qameta.htmlelements.element.ExtendedWebElement;
import io.qameta.htmlelements.element.HtmlElement;

public interface SletatSearchForm extends ExtendedWebElement<SletatSearchForm> {

    @FindBy(".//fieldset[contains(@class,'uis-item_city-from')]")
    SletatFormInput fromCity();

    @FindBy(".//fieldset[contains(@class,'uis-item_country-to')]")
    SletatFormInput toCountry();

    @FindBy(".//fieldset[contains(@class,'uis-item_resort')]")
    SletatFormInput resort();

    @FindBy(".//fieldset[contains(@class,'uis-item_hotels')]")
    SletatFormInput hotel();

    @FindBy(".//fieldset[contains(@class,'uis-item_nights')][1]")
    SletatFormSelect minNights();

    @FindBy(".//fieldset[contains(@class,'uis-item_nights')][2]")
    SletatFormSelect maxNights();

    @FindBy(".//fieldset[contains(@class,'uis-item_departure')]")
    SletatFormCalendar date();

    @FindBy(".//article[contains(@class,'uis-item uis-item_tourists')][1]")
    SletatFormRadio adults();

    @FindBy(".//article[contains(@class,'uis-item uis-item_tourists')][2]")
    SletatFormRadio childs();

    @FindBy(".//div[contains(@class,'uis-item_children-age-block')]//input[not(@disabled='')]")
    ExtendedList<HtmlElement> childAge();

    @FindBy(".//button[contains(@class, 'b-uikit-button b-uikit-button_search')]")
    HtmlElement searchButton();

    @FindBy(".//section[@class='slsf-flight-info-container']//label[not(contains(@class, 'uis-checkbox__label_checked'))]")
    ExtendedList<HtmlElement> flightInfoCheckboxes();
}
