package ru.yandex.autotests.innerpochta.cal.steps;

import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.steps.beans.event.Repetition;
import ru.yandex.autotests.innerpochta.steps.beans.layer.Layer;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Step;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.REPEAT_EVERY_DAY;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.REPEAT_EVERY_WEEK;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.USER_TYPE;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.YELLOW_COLOR;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author cosmopanda
 */
public class SettingsSteps {

    @Step("Формируем событие на сегодня")
    public Event formDefaultEvent(Long layerID) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        return new Event().withName(Utils.getRandomName())
            .withDescription(Utils.getRandomName())
            .withIsAllDay(false)
            .withStartTs(dateFormat.format(date).split("[.]")[0])
            .withEndTs(dateFormat.format(date.plusHours(3)).split("[.]")[0])
            .withLayerId(layerID)
            .withLocation("")
            .withOthersCanView(false)
            .withParticipantsCanInvite(false)
            .withParticipantsCanEdit(false);
    }

    @Step("Формируем событие через 3 месяца")
    public Event formEventInFuture(Long layerID) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        date = date.plusMonths(3);
        return new Event().withName(Utils.getRandomName())
            .withDescription(Utils.getRandomName())
            .withIsAllDay(false)
            .withStartTs(dateFormat.format(date).split("[.]")[0])
            .withEndTs(dateFormat.format(date.plusHours(3)).split("[.]")[0])
            .withLayerId(layerID)
            .withLocation("")
            .withOthersCanView(false)
            .withParticipantsCanInvite(false)
            .withParticipantsCanEdit(false);
    }

    @Step("Формируем событие с началом {1} дней назад")
    public Event formEventInPast(Long layerID, int daysBefore) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        return new Event().withName(Utils.getRandomName())
            .withDescription(Utils.getRandomName())
            .withIsAllDay(false)
            .withStartTs(dateFormat.format(date.minusDays(daysBefore)).split("[.]")[0])
            .withEndTs(dateFormat.format(date.minusDays(daysBefore).plusHours(1)).split("[.]")[0])
            .withLayerId(layerID)
            .withLocation("")
            .withOthersCanView(false)
            .withParticipantsCanInvite(false)
            .withParticipantsCanEdit(false);
    }

    @Step("Формируем событие с началом через {1} дней")
    public Event formEventAfterNDays(Long layerID, int daysAfter) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        return new Event().withName(Utils.getRandomName())
            .withDescription(Utils.getRandomName())
            .withIsAllDay(false)
            .withStartTs(dateFormat.format(date.plusDays(daysAfter)).split("[.]")[0])
            .withEndTs(dateFormat.format(date.plusDays(daysAfter).plusHours(1)).split("[.]")[0])
            .withLayerId(layerID)
            .withLocation("")
            .withOthersCanView(false)
            .withParticipantsCanInvite(false)
            .withParticipantsCanEdit(false);
    }

    @Step("Формируем событие на сегодня на весь день")
    public Event formDefaultAllDayEvent(Long layerID) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return new Event().withName(Utils.getRandomName())
            .withDescription(Utils.getRandomName())
            .withIsAllDay(true)
            .withStartTs(dateFormat.format(date).split("[.]")[0])
            .withEndTs(dateFormat.format(date.plusDays(1)).split("[.]")[0])
            .withLayerId(layerID)
            .withLocation("")
            .withOthersCanView(false)
            .withParticipantsCanInvite(false)
            .withParticipantsCanEdit(false);
    }

    @Step("Формируем повторяющееся событие")
    public Event formDefaultRepeatingEvent(Long layerID) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        return new Event().withName(Utils.getRandomName())
            .withDescription(Utils.getRandomName())
            .withIsAllDay(false)
            .withStartTs(dateFormat.format(date).split("[.]")[0])
            .withEndTs(dateFormat.format(date.plusHours(3)).split("[.]")[0])
            .withLayerId(layerID)
            .withLocation("")
            .withOthersCanView(false)
            .withParticipantsCanInvite(false)
            .withParticipantsCanEdit(false)
            .withRepetition(
                new Repetition()
                    .withWeeklyDays(REPEAT_EVERY_DAY)
                    .withType(REPEAT_EVERY_WEEK)
                    .withEach(1L)
            );
    }

    @Step("Формируем повторяющееся событие на весь день")
    public Event formDefaultRepeatingAllDayEvent(Long layerID) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        return new Event().withName(Utils.getRandomName())
                .withDescription(Utils.getRandomName())
                .withIsAllDay(true)
                .withStartTs(dateFormat.format(date).split("[.]")[0])
                .withEndTs(dateFormat.format(date.plusHours(3)).split("[.]")[0])
                .withLayerId(layerID)
                .withLocation("")
                .withOthersCanView(false)
                .withParticipantsCanInvite(false)
                .withParticipantsCanEdit(false)
                .withRepetition(
                        new Repetition()
                                .withWeeklyDays(REPEAT_EVERY_DAY)
                                .withType(REPEAT_EVERY_WEEK)
                                .withEach(1L)
                );
    }


    @Step("Формируем повторяющееся событие с началом {1} дней назад")
    public Event formDefaultRepeatingEventInPast(Long layerID, int daysBefore) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        return new Event().withName(Utils.getRandomName())
            .withDescription(Utils.getRandomName())
            .withIsAllDay(false)
            .withStartTs(dateFormat.format(date.minusDays(daysBefore)).split("[.]")[0])
            .withEndTs(dateFormat.format(date.minusDays(daysBefore).plusHours(1)).split("[.]")[0])
            .withLayerId(layerID)
            .withLocation("")
            .withOthersCanView(false)
            .withParticipantsCanInvite(false)
            .withParticipantsCanEdit(false)
            .withRepetition(
                new Repetition()
                    .withWeeklyDays(REPEAT_EVERY_DAY)
                    .withType(REPEAT_EVERY_WEEK)
                    .withEach(1L)
            );
    }

    @Step("Формируем обычный слой")
    public Layer formDefaultLayer() {
        return new Layer()
            .withName(getRandomName())
            .withColor(YELLOW_COLOR)
            .withType(USER_TYPE)
            .withIsClosed(false)
            .withIsDefault(false)
            .withAffectsAvailability(true)
            .withIsEventsClosedByDefault(true)
            .withIsOwner(true);
    }
}
