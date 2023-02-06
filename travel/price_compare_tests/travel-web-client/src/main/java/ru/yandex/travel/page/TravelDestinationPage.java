package ru.yandex.travel.page;

import io.qameta.htmlelements.WebPage;
import io.qameta.htmlelements.annotation.FindBy;
import ru.yandex.travel.element.DestinationToursSection;

public interface TravelDestinationPage extends WebPage {

    @FindBy("//div[contains(@class, 'serp')]")
    DestinationToursSection toursSection();

}
