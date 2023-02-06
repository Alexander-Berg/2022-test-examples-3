package ru.yandex.autotests.innerpochta.cal.steps;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.event.Repetition;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.REPEAT_EVERY_DAY;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.REPEAT_EVERY_WEEK;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author a-zoshchuk
 */
public class CalTouchCreateEventSteps {

    private AllureStepStorage user;
    private WebDriverRule webDriverRule;
    private Event event;
    private LinkedList<String> attendees;

    public CalTouchCreateEventSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    @Step("Создаем событие")
    public CalTouchCreateEventSteps createEventBuilder() {
        event = new Event()
            .withName("")
            .withDescription("")
            .withLayerId(0L)
            .withLocation("")
            .withAvailability("")
            .withIsAllDay(false);
        attendees = new LinkedList<>();
        user.defaultSteps().clicksOn(user.pages().calTouch().addEventButton());
        return this;
    }

    @Step("Добавляем заголовок события: «{0}»")
    public CalTouchCreateEventSteps withTitle(String title) {
        event.setName(title);
        user.defaultSteps().inputsTextInElement(user.pages().calTouch().eventPage().editableTitle(), title);
        return this;
    }

    @Step("Добавляем описание события: «{0}»")
    public CalTouchCreateEventSteps withDescription(String description) {
        event.setDescription(description);
        user.defaultSteps().inputsTextInElement(user.pages().calTouch().eventPage().editableDescription(), description);
        return this;
    }

    @Step("Изменяем слой события на «{0}»")
    public CalTouchCreateEventSteps withLayer(String layer) {
        event.setLayerId(user.apiCalSettingsSteps().getLayerByName(layer).getId());
        user.defaultSteps()
            .clicksOn(user.pages().calTouch().eventPage().editLayer())
            .clicksOnElementWithText(
                user.pages().calTouch().editLayerPage().menuItems().waitUntil(not(empty())),
                layer
            );
        return this;
    }

    @Step("Добавляем место события: «{0}»")
    public CalTouchCreateEventSteps withPlace(String place) {
        event.setLocation(place);
        user.defaultSteps()
            .clicksOn(user.pages().calTouch().eventPage().changePlace())
            .inputsTextInElement(user.pages().calTouch().editPlacePage().input(), place)
            .clicksOn(user.pages().calTouch().editPlacePage().suggested().waitUntil(not(empty())).get(0));
        return this;
    }

    @Step("Добавляем место события: «{0}»")
    public CalTouchCreateEventSteps withCustomPlace(String place) {
        event.setLocation(place);
        user.defaultSteps()
            .clicksOn(user.pages().calTouch().eventPage().changePlace())
            .inputsTextInElement(user.pages().calTouch().editPlacePage().input(), place)
            .clicksOn(user.pages().calTouch().editPlacePage().backToEditEvent());
        return this;
    }

    @Step("Добавляем участников: «{0}»")
    public CalTouchCreateEventSteps withParticipants(String... participants) {
        attendees.addAll(Arrays.asList(participants));
        user.defaultSteps().clicksOn(user.pages().calTouch().eventPage().changeParticipants());
        for (String participant : participants) {
            user.defaultSteps()
                .inputsTextInElement(user.pages().calTouch().editParticipantsPage().input(), participant)
                .clicksOn(
                    user.pages().calTouch().editParticipantsPage().suggested().waitUntil(not(empty())).get(0)
                );
        }
        user.defaultSteps().clicksOn(user.pages().calTouch().editParticipantsPage().save());
        return this;
    }

    @Step("Добавляем участников из популярных контактов под номерами «{0}»")
    public CalTouchCreateEventSteps withPopularParticipants(int... contactsOrders) {
        Pattern emailPattern = Pattern.compile(".*\n([^\n]*)");
        Matcher emailMatcher;
        List<String> contactsToAdd = new LinkedList<>();

        user.defaultSteps().clicksOn(
            user.pages().calTouch().eventPage().changeParticipants(),
            user.pages().calTouch().editParticipantsPage().input()
        );
        for (int contactOrder : contactsOrders) {
            emailMatcher = emailPattern.matcher(
                user.pages().calTouch().editParticipantsPage().suggested().waitUntil(not(empty())).get(contactOrder)
                    .getText()
            );
            emailMatcher.find();
            contactsToAdd.add(emailMatcher.group(1));
        }
        user.defaultSteps().clicksOn(user.pages().calTouch().editParticipantsPage().clearInputBtn());
        attendees.addAll(contactsToAdd);
        for (String contactToAdd : contactsToAdd) {
            user.defaultSteps().clicksOn(user.pages().calTouch().editParticipantsPage().input())
                .clicksOnElementWithText(
                    user.pages().calTouch().editParticipantsPage().suggested().waitUntil(not(empty())),
                    contactToAdd
                );
        }
        user.defaultSteps().clicksOn(user.pages().calTouch().editParticipantsPage().save());
        return this;
    }

    @Step("Изменяем занятость участников события на «{0}»")
    public CalTouchCreateEventSteps withAvailability(String availability) {
        event.setAvailability(availability);
        user.defaultSteps()
            .clicksOn(user.pages().calTouch().eventPage().editAvailability())
            .clicksOnElementWithText(
                user.pages().calTouch().editAvailabilityPage().menuItems().waitUntil(not(empty())),
                availability
            );
        return this;
    }

    @Step("Выставляем чекбокс «Весь день» в положение {0}")
    public CalTouchCreateEventSteps withAllDayCheckbox(Boolean isAllDay) {
        event.setIsAllDay(isAllDay);
        user.defaultSteps().turnTrue(user.pages().calTouch().eventPage().allDayCheckBox());
        return this;
    }

    @Step ("Выставляем повторение события «каждый день»")
    public CalTouchCreateEventSteps withEveryDayRepetition() {
        event.withRepetition(new Repetition()
            .withWeeklyDays(REPEAT_EVERY_DAY)
            .withType(REPEAT_EVERY_WEEK)
            .withEach(1L)
        );
        user.defaultSteps().clicksOn(
            user.pages().calTouch().eventPage().editEventRepetition()
        ).clicksOn(
            user.pages().calTouch().editRepetitionPage().menuItems().waitUntil(not(empty())).get(1)
        );
        return this;
    }

    @Step("Сохраняем событие")
    public CalTouchCreateEventSteps submit() {
        user.defaultSteps().clicksOn(user.pages().calTouch().eventPage().submitForm())
            .shouldNotSee(user.pages().calTouch().eventPage().submitForm());
        return this;
    }

    @Step("Открываем событие")
    public CalTouchCreateEventSteps openEvent() {
        user.defaultSteps().clicksOnElementWithText(
                user.pages().calTouch().events().waitUntil(not(empty())),
                !event.getName().equals("") ? event.getName() : "Без названия"
            );
        return this;
    }

    @Step("Открываем событие в расписании")
    public CalTouchCreateEventSteps openEventInSchedule() {
        user.defaultSteps().clicksOnElementWithText(
            user.pages().calTouch().eventsShedule().waitUntil(not(empty())),
            !event.getName().equals("") ? event.getName() : "Без названия"
        );
        return this;
    }

    @Step("Открываем событие и проверяем, что оно создалось верно, затем возвращаемся в сетку")
    public CalTouchCreateEventSteps thenCheck() {
        openEvent();
        checkFields();
        return this;
    }

    @Step("Проверяем свойства события")
    public CalTouchCreateEventSteps checkFields() {
        user.defaultSteps()
            .shouldContainText(
                user.pages().calTouch().eventPage().title(),
                !event.getName().equals("") ? event.getName() : "Без названия"
            )
            .shouldContainText(
                user.pages().calTouch().eventPage().eventDate(),
                event.getIsAllDay() ? "Весь день" : ":"
            )
            .shouldContainText(
                user.pages().calTouch().eventPage().availability(),
                !event.getAvailability().equals("") ? event.getAvailability() : "Занят"
            );
        if (!event.getDescription().equals(""))
            user.defaultSteps().shouldContainText(
                user.pages().calTouch().eventPage().description(),
                event.getDescription()
            );
        if (!event.getLocation().equals(""))
            user.defaultSteps().shouldContainText(user.pages().calTouch().eventPage().place(), event.getLocation());
        if (attendees.size() != 0)
            for (String eventParticipant : attendees)
                user.defaultSteps().shouldSeeElementInList(
                    user.pages().calTouch().eventPage().members(),
                    eventParticipant
                );
        if (event.getLayerId() != 0L)
            user.defaultSteps().shouldContainText(
                user.pages().calTouch().eventPage().layer(),
                user.apiCalSettingsSteps().getLayerById(event.getLayerId()).getName()
            );
        if (event.getRepetition() != null && event.getRepetition().getWeeklyDays().equals(REPEAT_EVERY_DAY)
            && event.getRepetition().getType().equals(REPEAT_EVERY_WEEK) && event.getRepetition().getEach() == 1L) {
            user.defaultSteps().shouldContainText(
                user.pages().calTouch().eventPage().eventRepetition(),
                "Повторяется ежедневно"
            );
        }
        user.defaultSteps().clicksOn(user.pages().calTouch().eventPage().cancelEdit());
        return this;
    }

    @Step("Создаем событие в расписании")
    public CalTouchCreateEventSteps createEventBuilderShedule() {
        event = new Event()
            .withName("")
            .withDescription("")
            .withLayerId(0L)
            .withLocation("")
            .withAvailability("")
            .withIsAllDay(false);
        attendees = new LinkedList<>();
        user.defaultSteps().clicksOn(user.pages().calTouch().addEventButtonShedule());
        return this;
    }

    @Step("Изменяем дату на «{1}»")
    public CalTouchCreateEventSteps setDate(MailElement element, String date) {
        user.defaultSteps().executesJavaScript("var input = arguments[0];\n" +
                "var date = arguments[1];\n" +
                "\n" +
                "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, " +
                "\"value\").set;\n" +
                "nativeInputValueSetter.call(input, date);\n" +
                "\n" +
                "var ev2 = new Event('input', { bubbles: true});\n" +
                "input.dispatchEvent(ev2);",
            element.getWrappedElement(), date);
        return this;
    }
}
