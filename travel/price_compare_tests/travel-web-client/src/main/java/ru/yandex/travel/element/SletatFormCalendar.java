package ru.yandex.travel.element;

import io.qameta.htmlelements.annotation.FindBy;
import io.qameta.htmlelements.annotation.Param;
import io.qameta.htmlelements.element.ExtendedWebElement;
import io.qameta.htmlelements.element.HtmlElement;
import org.openqa.selenium.WebElement;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

import static java.lang.String.format;

public interface SletatFormCalendar extends ExtendedWebElement<SletatFormCalendar> {

    default void select(String from, String to) {
        input().waitUntil(WebElement::isDisplayed).click();

        day(from).waitUntil(format("Выбираем день «From -> %s»", from),
                WebElementMatchers.isDisplayed(), 10).click();

        day(to).waitUntil(format("Выбираем день «To -> %s»", to),
                WebElementMatchers.isDisplayed(), 10).click();
    }

    @FindBy(".//span[contains(@class, 'uis-text_departure')]")
    HtmlElement input();

    @FindBy(".//div[contains(@class, 'uis-horizontal-calendar__months-scroll-block')]//li[contains(@data-reactid, '{{ date }}')]")
    HtmlElement month(@Param("date") String date);

    @FindBy(".//div[contains(@class, 'uis-horizontal-calendar__days-container')]//li[contains(@id, '{{ date }}')]")
    HtmlElement day(@Param("date") String date);


}
