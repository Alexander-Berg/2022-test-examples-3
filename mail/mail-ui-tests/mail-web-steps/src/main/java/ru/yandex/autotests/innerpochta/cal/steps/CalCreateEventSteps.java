package ru.yandex.autotests.innerpochta.cal.steps;

import org.openqa.selenium.NoSuchElementException;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.cal.pages.blocks.NewEvent;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author a-zoshchuk
 */
public class CalCreateEventSteps {

    private AllureStepStorage user;
    private WebDriverRule webDriverRule;
    private Event event;
    private LinkedList<String> attendees;

    public CalCreateEventSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    public CalCreateEventSteps setStartTime(String time) {
        String regex = "(^[0-9]{2})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(time);
        String timeToScroll = "00";
        if (matcher.find())
        {
            timeToScroll = matcher.group(1);
        }
        int hourNumber = Integer.parseInt(timeToScroll);
        NewEvent createEventElement;
        try {
            user.calPages().home().newEventPage().isDisplayed();
            createEventElement = user.calPages().home().newEventPage();
        }
        catch (NoSuchElementException e) {
            createEventElement = user.calPages().home().newEventPopup();
        }
        user.defaultSteps()
            .deselects(createEventElement.allDayCheckBox())
            .waitInSeconds(2)
            .clicksOn(createEventElement.time())
            .scrollToInvisibleElement(user.calPages().home().timesList().waitUntil(hasSize(greaterThanOrEqualTo(40))).get(hourNumber * 2 - 1))
            .clicksOnElementWithText(user.calPages().home().timesList(), time);
        return this;
    }

    public CalCreateEventSteps setPlace(int index) {
        openOfficesPopup();
        user.defaultSteps()
            .onMouseHoverAndClick(user.calPages().home().officesList().get(index));
        return this;
    }

    public CalCreateEventSteps setBC(String bcName) {
        openOfficesPopup();
        user.defaultSteps().clicksOnElementWithText(user.calPages().home().officesList(), bcName);
        return this;
    }

    private CalCreateEventSteps openOfficesPopup() {
        user.defaultSteps().scrollElementToTopOfView(user.calPages().home().newEventPage().roomsList().get(0))
            .waitInSeconds(2)
            .clicksOn(user.calPages().home().newEventPage().roomsList().get(0).officeResource());
        return this;
    }
}
