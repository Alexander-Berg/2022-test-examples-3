package ru.yandex.reminders.api.reminder;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.misc.test.Assert;
import ru.yandex.reminders.logic.event.EventData;
import ru.yandex.reminders.logic.reminder.Channel;
import ru.yandex.reminders.logic.reminder.Reminder;

/**
 * @author Eugene Voytitsky
 */
public class ReminderDataConverterTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(DateTimeZone.UTC);

    // tests for CAL-6536 and CAL-6537

    @Test
    public void convertToEventData_Internal() {
        EventData eventData = ReminderDataConverter
                .convertToEventData(createReminderData(), Option.some(Source.INTERNAL), "not-yandex-cid");
        checkEventData(eventData, Source.INTERNAL, true, false, true, false, true);
    }

    @Test
    public void convertToEventData_Internal_ByCid() {
        EventData eventData = ReminderDataConverter
                .convertToEventData(createReminderData(), Option.<Source>none(), "yandex-cid");
        checkEventData(eventData, Source.INTERNAL, true, false, true, false, true);
    }

    @Test
    public void convertToEventData_External() {
        EventData eventData = ReminderDataConverter
                .convertToEventData(createReminderData(), Option.some(Source.EXTERNAL), "yandex-cid");
        checkEventData(eventData, Source.EXTERNAL, false, false, true, true, true);
    }

    @Test
    public void convertToEventData_External_ByCid() {
        EventData eventData = ReminderDataConverter
                .convertToEventData(createReminderData(), Option.<Source>none(), "not-yandex-cid");
        checkEventData(eventData, Source.EXTERNAL, false, false, true, true, true);
    }

    @Test
    public void convertToEventData_Internal_Nezabudka() {
        EventData eventData = ReminderDataConverter
                .convertToEventData(createReminderData(), Option.some(Source.INTERNAL), "yandex-nezabudka");
        checkEventData(eventData, Source.INTERNAL, false, false, true, true, true);
    }

    @Test
    public void convertToEventData_External_Nezabudka() {
        EventData eventData = ReminderDataConverter
                .convertToEventData(createReminderData(), Option.some(Source.EXTERNAL), "yandex-nezabudka");
        checkEventData(eventData, Source.EXTERNAL, false, false, true, true, true);
    }

    @Test
    public void convertToEventData_Internal_ByCid_Nezabudka() {
        EventData eventData = ReminderDataConverter
                .convertToEventData(createReminderData(), Option.<Source>none(), "yandex-nezabudka");
        checkEventData(eventData, Source.INTERNAL, false, false, true, true, true);
    }

    private ReminderData createReminderData() {
        ChannelsData channels = new ChannelsData(
                Option.of(new ChannelsData.Sms(Option.of("sms-text"))),
                Option.of(new ChannelsData.Mail(Option.empty(),
                        Option.of("email-subject"), Option.of("email-bodyHtml"), Option.of("email-bodyText"))),
                Option.of(new ChannelsData.Panel(Option.empty(), Option.empty(), Option.of("panel-message"))),
                Option.empty(), Option.empty());

        ReminderData result = new ReminderData(Option.some("name"), Option.some("desc"),
                Option.some(DateTime.parse("2013-12-15T13:15:32Z")), channels, Option.<JsonObject>none());
        return result;
    }

    private void checkEventData(EventData eventData, Source source,
            boolean smsPresent, boolean smsEmpty, boolean emailPresent, boolean emailEmpty, boolean panelPresent)
    {
        Assert.some("name", eventData.getName());
        Assert.some("desc", eventData.getDescription());
        Assert.some("desc", eventData.getDescription());
        Assert.some(source, eventData.getSource());

        for (Reminder r : eventData.getReminders()) {
            Assert.equals(parse("2013-12-15 13:15:32"), r.getSendDate());
        }

        ListF<Reminder> smss = eventData.getReminders().filter(Reminder.channelIsF(Channel.SMS));
        if (smsPresent) {
            Assert.sizeIs(1, smss);
            if (smsEmpty) {
                Assert.none(smss.first().getText());
            } else {
                Assert.some("sms-text", smss.first().getText());
            }
        } else {
            Assert.isEmpty(smss);
        }

        ListF<Reminder> emails = eventData.getReminders().filter(Reminder.channelIsF(Channel.EMAIL));
        if (emailPresent) {
            Assert.sizeIs(1, emails);
            if (emailEmpty) {
                Assert.none(emails.first().getSubject());
                Assert.none(emails.first().getBodyText());
                Assert.none(emails.first().getBodyHtml());
            } else {
                Assert.some("email-subject", emails.first().getSubject());
                Assert.some("email-bodyText", emails.first().getBodyText());
                Assert.some("email-bodyHtml", emails.first().getBodyHtml());
            }
        } else {
            Assert.isEmpty(emails);
        }

        ListF<Reminder> panels = eventData.getReminders().filter(Reminder.channelIsF(Channel.PANEL));
        if (panelPresent) {
            Assert.sizeIs(1, panels);
            Assert.some("panel-message", panels.first().getMessage());
        } else {
            Assert.isEmpty(panels);
        }
    }

    private DateTime parse(String s) {
        return FORMATTER.parseDateTime(s);
    }

}
