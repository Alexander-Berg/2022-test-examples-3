package ru.yandex.travel.element;

import io.qameta.htmlelements.annotation.FindBy;
import io.qameta.htmlelements.element.ExtendedList;
import io.qameta.htmlelements.element.ExtendedWebElement;

public interface HotelRoomsSection extends ExtendedWebElement<HotelRoomsSection> {

    @FindBy(".//ul[contains(@class, 'serp__list_type_book')]/li")
    ExtendedList<HotelRoomRow> roomsList();

}