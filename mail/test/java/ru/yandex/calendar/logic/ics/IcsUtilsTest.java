package ru.yandex.calendar.logic.ics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.frontend.web.cmd.run.CommandRunException;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsParameter;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsXParameter;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsAttendee;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsConference;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsImage;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsProperty;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsXProperty;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.dateTime.IcsDateTimeFormats;
import ru.yandex.misc.lang.CharsetUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 * @author Sergey Shinderuk
 */
public class IcsUtilsTest {

    @Test
    public void parseTimestamp() {
        Assert.equals(
                new DateTime(2010, 4, 27, 18, 30, 1, 0, DateTimeZone.UTC).toInstant(),
                IcsUtils.parseTimestamp("20100427T183001Z"));
    }

    @Test
    public void formatDateTimeZ() {
        Assert.equals("19820720T131415Z", IcsDateTimeFormats.formatDateTime(new LocalDateTime(1982, 7, 20, 13, 14, 15, 16).toDateTime(DateTimeZone.UTC).toInstant()));
    }

    /**
     * @url http://tools.ietf.org/html/rfc5545#section-3.1
     * <pre>
     *   When parsing a content line, folded lines MUST first be unfolded
     *   according to the unfolding procedure described above.
     *
     *      Note: It is possible for very simple implementations to generate
     *      improperly folded lines in the middle of a UTF-8 multi-octet
     *      sequence.  For this reason, implementations need to unfold lines
     *      in such a way to properly restore the original sequence.
     * </pre>
     */
    @Test
    public void parseBytesUtf8Boundary() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write("BEGIN:VCALENDAR\nBEGIN:VEVENT\n".getBytes());
        os.write("SUMMARY:".getBytes());
        byte[] yu = "ю".getBytes(CharsetUtils.UTF8_CHARSET);
        Assert.hasSize(2, yu);
        os.write(yu, 0, 1);
        os.write("\n ".getBytes());
        os.write(yu, 1, 1);
        os.write("\nEND:VEVENT\nEND:VCALENDAR\n".getBytes());

        IcsCalendar calendar = IcsUtils.parseBytes(os.toByteArray(), "utf-8");
        Assert.equals("ю", calendar.getEvents().single().getSummary().get());
    }

    @Test
    public void parseBytesInGuessedEncodingUtf8Boundary() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write("BEGIN:VCALENDAR\nBEGIN:VEVENT\n".getBytes());
        os.write("SUMMARY:".getBytes());
        byte[] yu = "ю".getBytes(CharsetUtils.UTF8_CHARSET);
        Assert.hasSize(2, yu);
        os.write(yu, 0, 1);
        os.write("\n ".getBytes());
        os.write(yu, 1, 1);
        os.write("\nEND:VEVENT\nEND:VCALENDAR\n".getBytes());

        IcsCalendar calendar = IcsUtils.parseBytesInGuessedEncoding(os.toByteArray());
        Assert.equals("ю", calendar.getEvents().single().getSummary().get());
    }

    @Test
    public void parseAllDayEvent() {
        String allDayEventIcs =
                "BEGIN:VCALENDAR\r\n" +
                        "BEGIN:VEVENT\r\n" +
                        "UID:2036723985743aaa\r\n" +
                        "DTSTAMP:20110423T123432Z\r\n" +
                        "DTSTART;VALUE=DATE:20110425\r\n" +
                        "DTEND;VALUE=DATE:20110425\r\n" +
                        "SUMMARY:parseAllDayEvent\r\n" +
                        "END:VEVENT\r\n" +
                        "END:VCALENDAR";
        IcsVEvent allDayEvent = IcsUtils.parseBytesInGuessedEncoding(allDayEventIcs.getBytes()).getEvents().single();
        Assert.isTrue(allDayEvent.getDtStart().get().isDate());
        Assert.isTrue(allDayEvent.getDtEnd().get().isDate());
    }

    @Test
    public void stripUtf8Bom() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf});  // UTF-8 BOM
        os.write("BEGIN:VCALENDAR\nBEGIN:VEVENT\nSUMMARY:event\nEND:VEVENT\nEND:VCALENDAR\n".getBytes());

        IcsCalendar calendar = IcsUtils.parseBytes(os.toByteArray(), "UTF-8");
        Assert.equals("event", calendar.getEvents().single().getSummary().get());
    }

    @Test(expected = CommandRunException.class)
    public void notCompleteIcsFile() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final String s = "BEGIN:VCALENDAR\n" +
                "PRODID:-//Events Calendar//iCal4j 1.0//EN\n" +
                "CALSCALE:GREGORIAN\n" +
                "VERSION:2.0\n" +
                "METHOD:REQUEST\n" +
                "LAST-MODIFIED:20210128T064719Z\n" +
                "BEGIN:VEVENT\n" +
                "DTSTAMP:20210128T064719Z\n" +
                "DTSTART:20210128T073000Z\n" +
                "DTEND:20210128T083000Z\n" +
                "SUMMARY:ВКС.проведение заседания Штаба по строительству социально-значимых объектов ЯО под " +
                "председательством Неженца ВС\n" +
                "UID:1614b98c-31db-425d-9349-0bf87d335097@mindsrv.yarregion.ru\n" +
                "ORGANIZER:noreply@mindsrv.yarregion.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=587798@mail.ru;PARTSTAT=NEEDS-ACTION;" +
                "RSVP=TRUE:mailto:587798@mail.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=9051360105@mail.ru;PARTSTAT=NEEDS-ACTION;" +
                "RSVP=TRUE:mailto:9051360105@mail.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=\"ТГК-2 ЯТС Абабков М.Н.\";PARTSTAT=NEEDS-ACTION;" +
                "RSVP=TRUE:mailto:ababkovmn@tgc-2.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=\"ГО Ярославль\";PARTSTAT=NEEDS-ACTION;" +
                "RSVP=TRUE:mailto:yachmenkovve@city-yar.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=\"АО \"ГК \"ЕКС\"\";PARTSTAT=NEEDS-ACTION;" +
                "RSVP=TRUE:mailto:a.prosoedova@aoeks.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=\"Ермакова Н.В.\";PARTSTAT=NEEDS-ACTION;" +
                "RSVP=TRUE:mailto:aa_martinov@r76.rosreestr.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=borodulin@borg.adm.yar.ru;PARTSTAT=NEEDS-ACTION;" +
                "RSVP=TRUE:mailto:borodulin@borg.adm.yar.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=gku.esz@mail.com;PARTSTAT=NEEDS-ACTION;" +
                "RSVP=TRUE:mailto:gku.esz@mail.com\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=\"Смирнова Татьяна Александровна\";" +
                "PARTSTAT=NEEDS-ACTION;RSVP=TRUE:mailto:glava@admrmr.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=\"Гаврилов-Ямский МР\";PARTSTAT=NEEDS-ACTION;" +
                "RSVP=TRUE:mailto:gy-ois@adm.yar.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=\"Администрация Гаврилов-Ямского МР\";" +
                "PARTSTAT=NEEDS-ACTION;RSVP=TRUE:mailto:gyammr@adm.yar.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=info@ves76.ru;PARTSTAT=NEEDS-ACTION;" +
                "RSVP=TRUE:mailto:info@ves76.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=\"ГО Рыбинск\";PARTSTAT=NEEDS-ACTION;" +
                "RSVP=TRUE:mailto:invest@rybadm.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=\"ЦУ Ростехнадзора И.Н. Колесников\";" +
                "PARTSTAT=NEEDS-ACTION;RSVP=TRUE:mailto:kolesnikovenergy@gmail.com\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=\"Рыбинский МР Кругликова Татьяна Юрьевна\";" +
                "PARTSTAT=NEEDS-ACTION;RSVP=TRUE:mailto:kruglikova@admrmr.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=\"ГКУ ЯО \"ЕСЗ\"\";PARTSTAT=NEEDS-ACTION;" +
                "RSVP=TRUE:mailto:mironmobile@gmail.com\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=\"Некрасовский МР\";PARTSTAT=NEEDS-ACTION;" +
                "RSVP=TRUE:mailto:nekr@adm.yar.ru\n" +
                "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVI";
        os.write(s.getBytes(StandardCharsets.UTF_8));

        IcsCalendar calendar = IcsUtils.parseBytes(os.toByteArray(), "utf-8");
    }

    @Test(expected = CommandRunException.class)
    public void emptyLine() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final String s = "BEGIN:VCALENDAR\n" +
                "\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20201029T180000Z\n" +
                "DTEND:20201029T185959Z\n" +
                "LOCATION:https://event.webcasts.com/starthere.jsp?ei=1374390&tp_key=92f8ff46f0\n" +
                "DESCRIPTION:Thank you for registering to attend the Web Broadcast. Login will be available 30 minutes prior to the event start time.  We look forward to your participation in this broadcast.\n" +
                "SUMMARY:IIoT/Cloud series, Part 4: Machine learning and pattern recognition\n" +
                "PRIORITY:3\n" +
                "BEGIN:VALARM\n" +
                "ACTION:DISPLAY\n" +
                "TRIGGER:-PT15M\n" +
                "END:VALARM\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR\\r";
        os.write(s.getBytes(StandardCharsets.UTF_8));

        IcsCalendar calendar = IcsUtils.parseBytes(os.toByteArray(), "utf-8");
    }

    @Test
    public void parsingTimezone() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final String s = "\uFEFFBEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "PRODID:-//Custis/Modeus//Schedule Calendar App//RU\n" +
                "METHOD:REFRESH\n" +
                "BEGIN:VEVENT\n" +
                "UID:14e16181-8d57-4d09-a4c5-80bf431c80c4\n" +
                "SEQUENCE:0\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\n" +
                "DTSTAMP:20201219T084440\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201221T083000\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201221T100000\n" +
                "SUMMARY:Методика преподавания истории и обществознания / индивидуальная консультация / МетодикаПИиО-К-20И172\n" +
                "LOCATION:Microsoft Teams\n" +
                "DESCRIPTION:Консультация\\n\\nПреподаватель: Бородулина Елена Викторовна\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%2214e16181-8d57-4d09-a4c5-80bf431c80c4%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-21T08:30:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/14e16181-8d57-4d09-a4c5-80bf431c80c4\\n\n" +
                "CATEGORIES:Консультация\n" +
                "CATEGORIES:Методика ПИиО\n" +
                "END:VEVENT\n" +
                "BEGIN:VEVENT\n" +
                "UID:1f19e4c4-69b9-47fe-87ca-b77bf56ced36\n" +
                "SEQUENCE:0\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\n" +
                "DTSTAMP:20201219T084440\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201226T101500\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201226T114500\n" +
                "SUMMARY:Правосудие и развитие правосознания в Императорской России / Судебная реформа 1864 г. на окраинах Российской империи / ПРПИР Л-01\n" +
                "LOCATION:Microsoft Teams\n" +
                "DESCRIPTION:Лекционное занятие / Информационная лекция\\n\\nПреподаватель: Крестьянников Евгений Адольфович\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%221f19e4c4-69b9-47fe-87ca-b77bf56ced36%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-26T10:15:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/1f19e4c4-69b9-47fe-87ca-b77bf56ced36\\n\n" +
                "CATEGORIES:Лекционное занятие\n" +
                "CATEGORIES:ПРПИР\n" +
                "END:VEVENT\n" +
                "BEGIN:VEVENT\n" +
                "UID:23998622-a7bc-4a2a-829c-007efc775180\n" +
                "SEQUENCE:0\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\n" +
                "DTSTAMP:20201219T084440\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201221T101500\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201221T114500\n" +
                "SUMMARY:Психология и педагогика в школьной практике / Психологический портрет личности педагога  / ПиПвШП-П-20И172\n" +
                "LOCATION:Microsoft Teams\n" +
                "DESCRIPTION:Практическое занятие / Проектный семинар\\n\\nПреподаватель: Верховцев Константин Николаевич\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%2223998622-a7bc-4a2a-829c-007efc775180%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-21T10:15:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/23998622-a7bc-4a2a-829c-007efc775180\\n\n" +
                "CATEGORIES:Практическое занятие\n" +
                "CATEGORIES:ПиПвШП\n" +
                "END:VEVENT\n" +
                "BEGIN:VEVENT\n" +
                "UID:4a6ad941-ff5c-4c22-9b1c-086330020db0\n" +
                "SEQUENCE:0\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\n" +
                "DTSTAMP:20201219T084440\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201226T120000\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201226T133000\n" +
                "SUMMARY:Правосудие и развитие правосознания в Императорской России / Судебная реформа 1864 г. на окраинах Российской империи: региональная власть и общество / ПРПИР П-01\n" +
                "LOCATION:Microsoft Teams\n" +
                "DESCRIPTION:Практическое занятие / Семинар\\n\\nПреподаватель: Крестьянников Евгений Адольфович\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%224a6ad941-ff5c-4c22-9b1c-086330020db0%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-26T12:00:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/4a6ad941-ff5c-4c22-9b1c-086330020db0\\n\n" +
                "CATEGORIES:Практическое занятие\n" +
                "CATEGORIES:ПРПИР\n" +
                "END:VEVENT\n" +
                "BEGIN:VEVENT\n" +
                "UID:778028a4-f489-4081-ba76-11b4cdb13f30\n" +
                "SEQUENCE:0\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\n" +
                "DTSTAMP:20201219T084440\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201226T140000\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201226T153000\n" +
                "SUMMARY:Правосудие и развитие правосознания в Императорской России / Консультация / ПРПИР К-01\n" +
                "LOCATION:Microsoft Teams\n" +
                "DESCRIPTION:Консультация\\n\\nПреподаватель: Крестьянников Евгений Адольфович\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%22778028a4-f489-4081-ba76-11b4cdb13f30%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-26T14:00:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/778028a4-f489-4081-ba76-11b4cdb13f30\\n\n" +
                "CATEGORIES:Консультация\n" +
                "CATEGORIES:ПРПИР\n" +
                "END:VEVENT\n" +
                "BEGIN:VEVENT\n" +
                "UID:c2820c0e-f193-4572-b715-14f074ef8efe\n" +
                "SEQUENCE:0\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\n" +
                "DTSTAMP:20201219T084440\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201221T120000\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201221T133000\n" +
                "SUMMARY:Методика преподавания истории и обществознания / Проблемный метод обучения на уроках истории и обществознания. Проектная деятельность в обучении истории и обществознания. / МетодикаПИиО-П-20И172\n" +
                "LOCATION:Microsoft Teams\n" +
                "DESCRIPTION:Практическое занятие / Проектный семинар\\n\\nПреподаватель: Бородулина Елена Викторовна\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%22c2820c0e-f193-4572-b715-14f074ef8efe%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-21T12:00:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/c2820c0e-f193-4572-b715-14f074ef8efe\\n\n" +
                "CATEGORIES:Практическое занятие\n" +
                "CATEGORIES:Методика ПИиО\n" +
                "END:VEVENT\n" +
                "BEGIN:VEVENT\n" +
                "UID:ea746a8a-153a-4aa8-94b9-1dcae46428c6\n" +
                "SEQUENCE:0\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\n" +
                "DTSTAMP:20201219T084440\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201221T140000\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201221T153000\n" +
                "SUMMARY:Методика преподавания истории и обществознания / Инновации в обучении истории и обществознания. Интерактивное обучение. / МетодикаПИиО-Л-20И\n" +
                "LOCATION:Microsoft Teams\n" +
                "DESCRIPTION:Лекционное занятие / Информационная лекция\\n\\nПреподаватель: Бородулина Елена Викторовна\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%22ea746a8a-153a-4aa8-94b9-1dcae46428c6%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-21T14:00:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/ea746a8a-153a-4aa8-94b9-1dcae46428c6\\n\n" +
                "CATEGORIES:Лекционное занятие\n" +
                "CATEGORIES:Методика ПИиО\n" +
                "END:VEVENT\n" +
                "BEGIN:VEVENT\n" +
                "UID:f93d73f0-1dd5-4343-84c7-8aa075626904\n" +
                "SEQUENCE:0\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\n" +
                "DTSTAMP:20201219T084440\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201225T120000\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201225T133000\n" +
                "SUMMARY:История и героический эпос / Превращение героического эпоса в рыцарский роман / ИГЭ Л-01\n" +
                "LOCATION:Microsoft Teams\n" +
                "DESCRIPTION:Лекционное занятие / Проблемная лекция\\n\\nПреподаватель: Козлов Сергей Александрович\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%22f93d73f0-1dd5-4343-84c7-8aa075626904%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-25T12:00:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/f93d73f0-1dd5-4343-84c7-8aa075626904\\n\n" +
                "CATEGORIES:Лекционное занятие\n" +
                "CATEGORIES:ИГЭ\n" +
                "END:VEVENT\n" +
                "BEGIN:VEVENT\n" +
                "UID:fd356739-cf30-4b13-b957-2363cbb799c2\n" +
                "SEQUENCE:0\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\n" +
                "DTSTAMP:20201219T084440\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201225T140000\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201225T153000\n" +
                "SUMMARY:История и героический эпос / Песнь о моем Сиде / ИГЭ П-01\n" +
                "LOCATION:Microsoft Teams\n" +
                "DESCRIPTION:Практическое занятие / Деловая /ролевая игра \\n\\nПреподаватель: Козлов Сергей Александрович\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%22fd356739-cf30-4b13-b957-2363cbb799c2%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-25T14:00:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/fd356739-cf30-4b13-b957-2363cbb799c2\\n\n" +
                "CATEGORIES:Практическое занятие\n" +
                "CATEGORIES:ИГЭ\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";
        os.write(s.getBytes(StandardCharsets.UTF_8));

        IcsCalendar calendar = IcsUtils.parseBytes(os.toByteArray(), "utf-8");
    }

    @Test
    public void withConference() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final String s = "BEGIN:VCALENDAR\n" +
                "CALSCALE:GREGORIAN\n" +
                "VERSION:2.0\n" +
                "X-WR-CALNAME:Test\n" +
                "METHOD:PUBLISH\n" +
                "PRODID:-//Apple Inc.//Mac OS X 10.15.7//EN\n" +
                "BEGIN:VTIMEZONE\n" +
                "TZID:Europe/Moscow\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+1417\n" +
                "DTSTART:20010101T000000\n" +
                "TZNAME:GMT+3\n" +
                "TZOFFSETTO:+1417\n" +
                "END:STANDARD\n" +
                "END:VTIMEZONE\n" +
                "BEGIN:VEVENT\n" +
                "TRANSP:OPAQUE\n" +
                "DTEND;TZID=Europe/Moscow:20201125T233000\n" +
                "ATTENDEE;CN=\"1.Blowfish\";CUTYPE=ROOM;EMAIL=\"conf_rr_1_30@yandex-team.ru\"\n" +
                " ;PARTSTAT=ACCEPTED:mailto:conf_rr_1_30@yandex-team.ru\n" +
                "ATTENDEE;CN=\"2.Балтика(232)\";CUTYPE=ROOM;EMAIL=\"conf_spb_baltika@yandex-\n" +
                " team.ru\";PARTSTAT=ACCEPTED:mailto:conf_spb_baltika@yandex-team.ru\n" +
                "ATTENDEE;CN=\"Александр Селиванов\";CUTYPE=INDIVIDUAL;EMAIL=\"selivanov@yan\n" +
                " dex-team.ru\";PARTSTAT=ACCEPTED:mailto:selivanov@yandex-team.ru\n" +
                "ATTENDEE;CN=\"Заведующий Встречами\";CUTYPE=INDIVIDUAL;EMAIL=\"robot-meetin\n" +
                " gs-admin@yandex-team.ru\";PARTSTAT=ACCEPTED:mailto:robot-meetings-admin@y\n" +
                " andex-team.ru\n" +
                "ORGANIZER;CN=\"Заведующий Встречами\":mailto:robot-meetings-admin@yandex-t\n" +
                " eam.ru\n" +
                "UID:xlUcSxwV2yandex.ru\n" +
                "DTSTAMP:20201125T115405Z\n" +
                "LOCATION: 2.Балтика(232) (Benua-2-232-1)\\, 1.Blowfish (КР 1-30)\n" +
                "DESCRIPTION:Телефоны переговорок:\\n2.Балтика(232) (Benua-2-232-1): видео\n" +
                "  18865\\n1.Blowfish (КР 1-30): 18063\\, видео 18063\n" +
                "SEQUENCE:2\n" +
                "CONFERENCE;FEATURE=AUDIO;LABEL=Attendee dial-in:https://telemost.yandex.\n" +
                " ru/j/74759603282293\n" +
                "CATEGORIES:Александр Селиванов\n" +
                "X-APPLE-TRAVEL-ADVISORY-BEHAVIOR:AUTOMATIC\n" +
                "SUMMARY:Test\n" +
                "LAST-MODIFIED:20201125T163327Z\n" +
                "DTSTART;TZID=Europe/Moscow:20201125T231000\n" +
                "CREATED:20201125T135721Z\n" +
                "BEGIN:VALARM\n" +
                "UID:2CB2853A-5481-42BF-918D-E9887AB43B7A\n" +
                "ACTION:AUDIO\n" +
                "TRIGGER:PT0S\n" +
                "X-APPLE-LOCAL-DEFAULT-ALARM:TRUE\n" +
                "ATTACH;VALUE=URI:Chord\n" +
                "X-WR-ALARMUID:2CB2853A-5481-42BF-918D-E9887AB43B7A\n" +
                "X-APPLE-DEFAULT-ALARM:TRUE\n" +
                "END:VALARM\n" +
                "BEGIN:VALARM\n" +
                "UID:1796334A-B126-4A21-9981-A009DE7C151C\n" +
                "X-APPLE-DEFAULT-ALARM:TRUE\n" +
                "TRIGGER:-PT5M\n" +
                "ACTION:AUDIO\n" +
                "ATTACH;VALUE=URI:Chord\n" +
                "X-WR-ALARMUID:1796334A-B126-4A21-9981-A009DE7C151C\n" +
                "END:VALARM\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        os.write(s.getBytes(StandardCharsets.UTF_8));

        IcsCalendar calendar = IcsUtils.parseBytes(os.toByteArray(), "utf-8");

        Assert.isTrue(calendar.getEvents().getO(0).get().hasProperty("CONFERENCE"));
        Assert.isTrue(calendar.getEvents().getO(0).get().getPropertyValue("CONFERENCE").get().equals("https://telemost.yandex.ru/j/74759603282293"));
    }

    @Test
    public void propertiesAndParametersTest() {
        String s =
                "BEGIN:VCALENDAR\r\n" +
                        "BEGIN:VEVENT\r\n" +
                        "UID:2036723985743aaa\r\n" +
                        "DTSTAMP:20110423T123432Z\rw\n" +
                        "DTSTART;VALUE=DATE:20110425\r\n" +
                        "DTEND;VALUE=DATE:20110425\r\n" +
                        "ATTENDEE;CN=Cyrus Daboo;EMAIL=cyrus@example.com:mailto:opaque-toke\n" +
                        " n-1234@example.com\n" +
                        "CONFERENCE;FEATURE=AUDIO;LABEL=Attendee dial-in:https://telemost.yandex.ru/j/74759603282293\r\n" +
                        "ACKNOWLEDGED:20090604T084500Z\n" +
                        "COLOR:turquoise\n" +
                        "IMAGE;VALUE=URI;DISPLAY=BADGE;FMTTYPE=image/png:h\n" +
                        " ttp://example.com/images/party.png\n" +
                        "SOURCE;VALUE=URI:https://example.com/holidays.ics\n" +
                        "REFRESH-INTERVAL;VALUE=DURATION:P1W\n" +
                        "SUMMARY:parseAllDayEvent\r\n" +
                        "END:VEVENT\r\n" +
                        "END:VCALENDAR\r\n";

        IcsCalendar calendar = IcsCalendar.parseString(s);
        IcsVEvent vevent = calendar.getEvents().get(0);
        Assert.equals("https://telemost.yandex.ru/j/74759603282293", vevent.getPropertyValue("CONFERENCE").get());
        IcsConference conference = (IcsConference) vevent.getProperty("CONFERENCE").get();
        Assert.equals("AUDIO", conference.getParameter("FEATURE").get().getValue());
        Assert.equals("Attendee dial-in", conference.getParameter("LABEL").get().getValue());

        Assert.equals("20090604T084500Z", vevent.getPropertyValue("ACKNOWLEDGED").get());
        Assert.equals("turquoise", vevent.getPropertyValue("COLOR").get());
        Assert.equals("http://example.com/images/party.png", vevent.getPropertyValue("IMAGE").get());
        IcsImage image = (IcsImage) vevent.getProperty("IMAGE").get();
        Assert.equals("BADGE", image.getParameter("DISPLAY").get().getValue());

        Assert.equals("https://example.com/holidays.ics", vevent.getPropertyValue("SOURCE").get());
        Assert.equals("P1W", vevent.getPropertyValue("REFRESH-INTERVAL").get());

        IcsAttendee attendee = (IcsAttendee) vevent.getProperty("ATTENDEE").get();
        Assert.equals("cyrus@example.com", attendee.getParameter("EMAIL").get().getValue());

    }

    @Test
    public void serializeTest() throws IOException {
        final String expected = "BEGIN:VCALENDAR\r\n" +
                "VERSION:2.0\r\n" +
                "PRODID:-//Custis/Modeus//Schedule Calendar App//RU\r\n" +
                "METHOD:REFRESH\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:14e16181-8d57-4d09-a4c5-80bf431c80c4\r\n" +
                "SEQUENCE:0\r\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\r\n" +
                "DTSTAMP:20201219T084440\r\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201221T083000\r\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201221T100000\r\n" +
                "SUMMARY:Методика преподавания истории и обществознания / индивидуальная консультация / МетодикаПИиО-К-20И172\r\n" +
                "LOCATION:Microsoft Teams\r\n" +
                "DESCRIPTION:Консультация\\n\\nПреподаватель: Бородулина Елена Викторовна\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%2214e16181-8d57-4d09-a4c5-80bf431c80c4%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-21T08:30:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/14e16181-8d57-4d09-a4c5-80bf431c80c4\\n\r\n" +
                "CATEGORIES:Консультация\r\n" +
                "CATEGORIES:Методика ПИиО\r\n" +
                "END:VEVENT\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:1f19e4c4-69b9-47fe-87ca-b77bf56ced36\r\n" +
                "SEQUENCE:0\r\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\r\n" +
                "DTSTAMP:20201219T084440\r\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201226T101500\r\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201226T114500\r\n" +
                "SUMMARY:Правосудие и развитие правосознания в Императорской России / Судебная реформа 1864 г. на окраинах Российской империи / ПРПИР Л-01\r\n" +
                "LOCATION:Microsoft Teams\r\n" +
                "DESCRIPTION:Лекционное занятие / Информационная лекция\\n\\nПреподаватель: Крестьянников Евгений Адольфович\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%221f19e4c4-69b9-47fe-87ca-b77bf56ced36%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-26T10:15:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/1f19e4c4-69b9-47fe-87ca-b77bf56ced36\\n\r\n" +
                "CATEGORIES:Лекционное занятие\r\n" +
                "CATEGORIES:ПРПИР\r\n" +
                "END:VEVENT\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:23998622-a7bc-4a2a-829c-007efc775180\r\n" +
                "SEQUENCE:0\r\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\r\n" +
                "DTSTAMP:20201219T084440\r\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201221T101500\r\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201221T114500\r\n" +
                "SUMMARY:Психология и педагогика в школьной практике / Психологический портрет личности педагога  / ПиПвШП-П-20И172\r\n" +
                "LOCATION:Microsoft Teams\r\n" +
                "DESCRIPTION:Практическое занятие / Проектный семинар\\n\\nПреподаватель: Верховцев Константин Николаевич\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%2223998622-a7bc-4a2a-829c-007efc775180%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-21T10:15:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/23998622-a7bc-4a2a-829c-007efc775180\\n\r\n" +
                "CATEGORIES:Практическое занятие\r\n" +
                "CATEGORIES:ПиПвШП\r\n" +
                "END:VEVENT\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:4a6ad941-ff5c-4c22-9b1c-086330020db0\r\n" +
                "SEQUENCE:0\r\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\r\n" +
                "DTSTAMP:20201219T084440\r\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201226T120000\r\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201226T133000\r\n" +
                "SUMMARY:Правосудие и развитие правосознания в Императорской России / Судебная реформа 1864 г. на окраинах Российской империи: региональная власть и общество / ПРПИР П-01\r\n" +
                "LOCATION:Microsoft Teams\r\n" +
                "DESCRIPTION:Практическое занятие / Семинар\\n\\nПреподаватель: Крестьянников Евгений Адольфович\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%224a6ad941-ff5c-4c22-9b1c-086330020db0%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-26T12:00:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/4a6ad941-ff5c-4c22-9b1c-086330020db0\\n\r\n" +
                "CATEGORIES:Практическое занятие\r\n" +
                "CATEGORIES:ПРПИР\r\n" +
                "END:VEVENT\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:778028a4-f489-4081-ba76-11b4cdb13f30\r\n" +
                "SEQUENCE:0\r\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\r\n" +
                "DTSTAMP:20201219T084440\r\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201226T140000\r\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201226T153000\r\n" +
                "SUMMARY:Правосудие и развитие правосознания в Императорской России / Консультация / ПРПИР К-01\r\n" +
                "LOCATION:Microsoft Teams\r\n" +
                "DESCRIPTION:Консультация\\n\\nПреподаватель: Крестьянников Евгений Адольфович\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%22778028a4-f489-4081-ba76-11b4cdb13f30%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-26T14:00:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/778028a4-f489-4081-ba76-11b4cdb13f30\\n\r\n" +
                "CATEGORIES:Консультация\r\n" +
                "CATEGORIES:ПРПИР\r\n" +
                "END:VEVENT\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:c2820c0e-f193-4572-b715-14f074ef8efe\r\n" +
                "SEQUENCE:0\r\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\r\n" +
                "DTSTAMP:20201219T084440\r\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201221T120000\r\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201221T133000\r\n" +
                "SUMMARY:Методика преподавания истории и обществознания / Проблемный метод обучения на уроках истории и обществознания. Проектная деятельность в обучении истории и обществознания. / МетодикаПИиО-П-20И172\r\n" +
                "LOCATION:Microsoft Teams\r\n" +
                "DESCRIPTION:Практическое занятие / Проектный семинар\\n\\nПреподаватель: Бородулина Елена Викторовна\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%22c2820c0e-f193-4572-b715-14f074ef8efe%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-21T12:00:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/c2820c0e-f193-4572-b715-14f074ef8efe\\n\r\n" +
                "CATEGORIES:Практическое занятие\r\n" +
                "CATEGORIES:Методика ПИиО\r\n" +
                "END:VEVENT\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:ea746a8a-153a-4aa8-94b9-1dcae46428c6\r\n" +
                "SEQUENCE:0\r\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\r\n" +
                "DTSTAMP:20201219T084440\r\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201221T140000\r\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201221T153000\r\n" +
                "SUMMARY:Методика преподавания истории и обществознания / Инновации в обучении истории и обществознания. Интерактивное обучение. / МетодикаПИиО-Л-20И\r\n" +
                "LOCATION:Microsoft Teams\r\n" +
                "DESCRIPTION:Лекционное занятие / Информационная лекция\\n\\nПреподаватель: Бородулина Елена Викторовна\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%22ea746a8a-153a-4aa8-94b9-1dcae46428c6%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-21T14:00:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/ea746a8a-153a-4aa8-94b9-1dcae46428c6\\n\r\n" +
                "CATEGORIES:Лекционное занятие\r\n" +
                "CATEGORIES:Методика ПИиО\r\n" +
                "END:VEVENT\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:f93d73f0-1dd5-4343-84c7-8aa075626904\r\n" +
                "SEQUENCE:0\r\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\r\n" +
                "DTSTAMP:20201219T084440\r\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201225T120000\r\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201225T133000\r\n" +
                "SUMMARY:История и героический эпос / Превращение героического эпоса в рыцарский роман / ИГЭ Л-01\r\n" +
                "LOCATION:Microsoft Teams\r\n" +
                "DESCRIPTION:Лекционное занятие / Проблемная лекция\\n\\nПреподаватель: Козлов Сергей Александрович\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%22f93d73f0-1dd5-4343-84c7-8aa075626904%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-25T12:00:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/f93d73f0-1dd5-4343-84c7-8aa075626904\\n\r\n" +
                "CATEGORIES:Лекционное занятие\r\n" +
                "CATEGORIES:ИГЭ\r\n" +
                "END:VEVENT\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:fd356739-cf30-4b13-b957-2363cbb799c2\r\n" +
                "SEQUENCE:0\r\n" +
                "ORGANIZER;CN=Modeus:MAILTO:help@modeus.org\r\n" +
                "DTSTAMP:20201219T084440\r\n" +
                "DTSTART;TZID=Asia/Yekaterinburg:20201225T140000\r\n" +
                "DTEND;TZID=Asia/Yekaterinburg:20201225T153000\r\n" +
                "SUMMARY:История и героический эпос / Песнь о моем Сиде / ИГЭ П-01\r\n" +
                "LOCATION:Microsoft Teams\r\n" +
                "DESCRIPTION:Практическое занятие / Деловая /ролевая игра \\n\\nПреподаватель: Козлов Сергей Александрович\\n\\nПосмотреть в полной версии: https://utmn.modeus.org/schedule-calendar/my?selectedEvent=%7B%22eventId%22:%22fd356739-cf30-4b13-b957-2363cbb799c2%22%7D&calendar=%7B%22view%22:%22agendaWeek%22\\,%22date%22:%222020-12-25T14:00:00.000Z%22%7D\\nПосмотреть в мобильной версии: https://m-utmn.modeus.org/#/event/fd356739-cf30-4b13-b957-2363cbb799c2\\n\r\n" +
                "CATEGORIES:Практическое занятие\r\n" +
                "CATEGORIES:ИГЭ\r\n" +
                "END:VEVENT\r\n" +
                "END:VCALENDAR\r\n";

        String actual = IcsCalendar.parseString(expected).serializeToString();
        Assert.equals(expected, actual);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(expected.getBytes(StandardCharsets.UTF_8));
        byte[] actualBytes = IcsUtils.parseBytes(os.toByteArray(), "utf-8").serializeToBytes();
        Assert.arraysEquals(expected.getBytes(StandardCharsets.UTF_8), actualBytes);

    }

    @Test
    public void serializeAndParseXAppleLocation() {
        String paramVal = "New;Jersey";
        String propVal = "geo:40.110132,-74.655620;\\;\\,\\\\";

        IcsParameter param = new IcsXParameter("X-TITLE", paramVal);
        IcsProperty prop = new IcsXProperty("X-APPLE-STRUCTURED-LOCATION", propVal, Cf.list(param));

        String serialized = new IcsCalendar(Cf.list(), Cf.list(prop)).serializeToString();

        Assert.assertContains(serialized, paramVal);
        Assert.assertContains(serialized, propVal);

        IcsCalendar parsed = IcsUtils.parse(new StringReader(serialized));

        IcsProperty parsedProp = parsed.getProperty(prop.getName()).get();
        IcsParameter parsedParam = parsedProp.getParameter(param.getName()).get();

        Assert.equals(paramVal, parsedParam.getValue());
        Assert.equals(propVal, parsedProp.getValue());
    }

} //~
