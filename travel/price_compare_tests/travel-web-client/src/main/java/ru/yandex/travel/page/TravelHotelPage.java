package ru.yandex.travel.page;

import io.qameta.htmlelements.WebPage;
import io.qameta.htmlelements.annotation.FindBy;
import ru.yandex.travel.element.HotelRoomsSection;
import ru.yandex.travel.element.HotelToursSection;

public interface TravelHotelPage extends WebPage {

    @FindBy("//section[@id='hotel__tours']")
    HotelToursSection toursSection();

    @FindBy("//section[@id='hotel__rooms']")
    HotelRoomsSection roomsSection();

}
