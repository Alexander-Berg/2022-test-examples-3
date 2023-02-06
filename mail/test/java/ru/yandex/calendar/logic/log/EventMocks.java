package ru.yandex.calendar.logic.log;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemCreateOrDeleteOperationType;
import com.microsoft.schemas.exchange.services._2006.types.MessageDispositionType;
import lombok.val;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.LocalDateTime;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Either;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.ews.proxy.EwsCallLogEventJson;
import ru.yandex.calendar.frontend.ews.proxy.EwsCallOperation;
import ru.yandex.calendar.frontend.ews.proxy.EwsEventIdLogDataJson;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventNotification;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.MainEvent;
import ru.yandex.calendar.logic.beans.generated.Settings;
import ru.yandex.calendar.logic.beans.generated.TodoItem;
import ru.yandex.calendar.logic.beans.generated.TodoList;
import ru.yandex.calendar.logic.contact.UnivContact;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.ActorId;
import ru.yandex.calendar.logic.event.EventLayers;
import ru.yandex.calendar.logic.event.EventWithRelations;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsMethod;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsRecurrenceId;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsUid;
import ru.yandex.calendar.logic.log.change.EventChangeLogEventJson;
import ru.yandex.calendar.logic.log.change.EventChangeType;
import ru.yandex.calendar.logic.log.change.EventChangesJson;
import ru.yandex.calendar.logic.log.change.ExternalIdChangeLogEventJson;
import ru.yandex.calendar.logic.log.change.changes.EventFieldsChangesJson;
import ru.yandex.calendar.logic.log.change.changes.ExdatesChangesJson;
import ru.yandex.calendar.logic.log.change.changes.LayersChangesJson;
import ru.yandex.calendar.logic.log.change.changes.Participation;
import ru.yandex.calendar.logic.log.change.changes.RepetitionChangesJson;
import ru.yandex.calendar.logic.log.change.changes.RepetitionFieldsChangesJson;
import ru.yandex.calendar.logic.log.change.changes.ResourcesChangesJson;
import ru.yandex.calendar.logic.log.change.changes.UserChangesJson;
import ru.yandex.calendar.logic.log.change.changes.UsersChangesJson;
import ru.yandex.calendar.logic.log.change.changes.field.FieldChange;
import ru.yandex.calendar.logic.mailer.MailerHandlingLogEventJson;
import ru.yandex.calendar.logic.mailer.MailerHandlingResult;
import ru.yandex.calendar.logic.mailer.logbroker.MailAttach;
import ru.yandex.calendar.logic.mailer.model.MailerMail;
import ru.yandex.calendar.logic.notification.Channel;
import ru.yandex.calendar.logic.notification.NotificationLogEventJson;
import ru.yandex.calendar.logic.notification.NotificationSendManager;
import ru.yandex.calendar.logic.notification.NotificationSendResult;
import ru.yandex.calendar.logic.notification.NotificationStatus;
import ru.yandex.calendar.logic.sending.EventMailLogEventJson;
import ru.yandex.calendar.logic.sending.TodoMailLogEventJson;
import ru.yandex.calendar.logic.sending.param.CommonEventMessageParameters;
import ru.yandex.calendar.logic.sending.param.EventLocation;
import ru.yandex.calendar.logic.sending.param.EventMessageInfo;
import ru.yandex.calendar.logic.sending.param.EventMessageTimezone;
import ru.yandex.calendar.logic.sending.param.EventTimeParameters;
import ru.yandex.calendar.logic.sending.param.MessageDestination;
import ru.yandex.calendar.logic.sending.param.MessageOverrides;
import ru.yandex.calendar.logic.sending.param.Recipient;
import ru.yandex.calendar.logic.sending.param.ReplyMessageParameters;
import ru.yandex.calendar.logic.sending.param.Sender;
import ru.yandex.calendar.logic.sending.param.TodoMessageParameters;
import ru.yandex.calendar.logic.sending.real.DestinedMessage;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.sharing.participant.EventParticipants;
import ru.yandex.calendar.logic.todo.TodoMailType;
import ru.yandex.calendar.logic.user.Group;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.logic.user.NameI18n;
import ru.yandex.calendar.logic.user.SettingsInfo;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.logic.user.UserOrMaillist;
import ru.yandex.commune.mail.MailAddress;
import ru.yandex.inside.passport.PassportSid;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

import static java.util.Collections.emptyList;

class EventMocks {
    public static ActionInfo mailActionInfo() {
        return new ActionInfo("somename", ActionSource.MAIL, "111111", MoscowTime.dateTime(2021, 11, 11, 10, 0).toInstant(), false);
    }

    public static EventMailLogEventJson mailLogEvent() {
        val uid = PassportUid.cons(122);
        val uid2 = PassportUid.cons(123);
        val sender = new Sender(Option.of(uid), Option.of("login"), new NameI18n("имя", Option.empty()), Option.empty(), new Email("ad@yandex-team.ru"));
        val recipient = new Recipient(new MailAddress(new Email("recipient@yandex.ru")), Option.of(uid2), Option.of("login"), false);

        val meta = new CommonEventMessageParameters(Language.RUSSIAN, new LocalDateTime("2021-11-11"),
                sender, recipient, new Email("from@yandex-team.ru"), "http://calendar.yandex-team.ru/",
                false, new MessageOverrides(Option.empty(), Option.empty(), Option.empty(), Option.empty()));

        val evTimeParameters = new EventTimeParameters(new LocalDateTime(2021, 4, 28, 16, 54),
                new LocalDateTime(2021, 4, 28, 16, 54), false, false, Option.empty(),
                new EventMessageTimezone(Option.empty(), "+04:00"));
        val ev = new EventMessageInfo(11L, 22L, "13", Option.empty(),
                evTimeParameters, "name", "desc", EventLocation.location("aaaa"),
                PassportSid.BALANCE, Option.empty(), Option.empty());
        val parameters = new ReplyMessageParameters(meta, ev, Decision.MAYBE, Option.empty(), Option.empty(), MailType.APARTMENT_CANCEL);
        return new EventMailLogEventJson(
                parameters, new Email("to@yandex.ru"),
                new DestinedMessage(DestinedMessage.XslSource.NORMAL, MessageDestination.NOT_FOR_EXCHANGE, parameters),
                "199393", "SUCCESSFUL", Optional.of("250 2.0.0 Ok: queued as 1E91620002"));
    }

    public static ActionInfo exchangeActionInfo() {
        return new ActionInfo("somename", ActionSource.EXCHANGE, "111111", MoscowTime.dateTime(2021, 11, 11, 10, 0).toInstant(), false);
    }

    public static EwsCallLogEventJson ewsCallEvent() {
        val uid = PassportUid.cons(122);
        val mainEv = new MainEvent();
        mainEv.setId(3333L);
        mainEv.setExternalId("188929");
        val evIdLogData = new EwsEventIdLogDataJson(uid.toString(), new EventIdLogDataJson(1388L, mainEv, Optional.of(MoscowTime.dateTime(2021, 11, 11, 11, 0).toInstant())));
        return new EwsCallLogEventJson(EwsCallOperation.ACCEPT_MEETING, Optional.of(evIdLogData), Optional.empty(),
                Optional.of(MessageDispositionType.SEND_AND_SAVE_COPY),
                Either.left(CalendarItemCreateOrDeleteOperationType.SEND_ONLY_TO_ALL), Optional.empty());
    }

    private static EventIdLogDataJson createEventIdLogData() {
        val mainEvent = new MainEvent();
        mainEvent.setId(133L);
        mainEvent.setExternalId("111");
        return new EventIdLogDataJson(179L, mainEvent, Optional.of(MoscowTime.dateTime(2021, 11, 21, 13, 0).toInstant()));
    }

    public static ActionInfo caldavActionInfo() {
        return new ActionInfo("somename", ActionSource.CALDAV, "111111", MoscowTime.dateTime(2021, 11, 11, 10, 0).toInstant(), false);
    }

    public static EventChangeLogEventJson eventChangeEvent() {
        val uid = Optional.of(new PassportUid(333L));
        val eventIdLogData = createEventIdLogData();

        val eventFieldsChanges = createEventFieldsChanges(FieldChange.of(Option.empty(), "NEWNAME"),
                FieldChange.of(Option.empty(), "NEWDESCRIPTION"),
                FieldChange.of(Option.empty(), "NEWLOCATION"),
                FieldChange.empty(), FieldChange.empty(), FieldChange.empty(), FieldChange.empty());

        val repetitionChanges = createRepetitionChanges(
                FieldChange.of(Option.empty(), RegularRepetitionRule.DAILY),
                FieldChange.of(Option.empty(), 3),
                FieldChange.of(Option.empty(), MoscowTime.dateTime(2021, 12, 11, 10, 0).toInstant()),
                FieldChange.of(Option.of("tuesday"), "mon"),
                FieldChange.of(Option.empty(), false),
                List.of(MoscowTime.dateTime(2021, 12, 10, 10, 0).toInstant()),
                List.of(MoscowTime.dateTime(2021, 12, 12, 10, 0).toInstant()));

        val resourceChanges = createResourcesChanges(List.of(new ResourcesChangesJson.ResourceIdentJson(133L, new Email("a@yandex-team.ru"))),
                emptyList());

        val removedUsers = Cf.<UserChangesJson>arrayList(new UserChangesJson(Optional.of(PassportUid.cons(11L)),
                new Email("newuser@yandex-team.ru"),
                FieldChange.of(Option.of(Decision.YES), Decision.NO),
                FieldChange.of(Option.empty(), Availability.AVAILABLE),
                FieldChange.of(Option.of(Participation.ORGANIZER), Participation.ATTENDEE)));

        val userChanges = new UsersChangesJson(removedUsers, Cf.arrayList(), Cf.arrayList());

        val layersChanges = createLayersChanges(List.of(new LayersChangesJson.LayerIdentJson(1113, uid), new LayersChangesJson.LayerIdentJson(1112, uid)), emptyList());

        val eventChanges = new EventChangesJson(eventFieldsChanges, repetitionChanges, resourceChanges, userChanges, layersChanges);
        return new EventChangeLogEventJson(ActorId.user(PassportUid.cons(111111)), EventChangeType.CREATE, eventIdLogData, eventChanges);
    }

    public static MailerHandlingLogEventJson mailerHandlingEvent() {
        val uid = PassportUid.cons(122);
        val user = UserOrMaillist.user(uid, new Email("ad@yandex-team.ru"));

        val vevent = new IcsVEvent().addProperty(new IcsUid("12891291")).addProperty(new IcsRecurrenceId(MoscowTime.dateTime(2021, 11, 11, 10, 0).toInstant()));
        val ics = new IcsCalendar().addProperty(IcsMethod.PUBLISH).addComponent(vevent);

        val mail = new MailerMail(MoscowTime.dateTime(2021, 11, 11, 10, 0).toInstant(), "somesubj", "91983",
                new MailAttach(uid, 1812891L, "stid"), new UnivContact(new Email("ad@yandex-team.ru"), "sanya",
                Option.of(uid)), ics);
        return new MailerHandlingLogEventJson(user, mail, MailerHandlingResult.sent());
    }

    public static ActionInfo xivaActionInfo() {
        return new ActionInfo("somename", ActionSource.XIVA, "111111", MoscowTime.dateTime(2021, 11, 11, 10, 0).toInstant(), false);
    }

    public static NotificationLogEventJson notificationEvent() {
        val uid = PassportUid.cons(122);
        val evNotification = new EventNotification();
        evNotification.setId(33L);
        evNotification.setChannel(Channel.EMAIL);
        evNotification.setNextSendTs(MoscowTime.dateTime(2021, 11, 11, 10, 0).toInstant());
        evNotification.setOffsetMinute(3);
        val evUser = new EventUser();
        evUser.setId(23923L);
        val userInfo = new UserInfo(uid, EnumSet.of(Group.APARTMENT_USER), Cf.arrayList(), Cf.arrayList(), false, false);
        val settings = new Settings();
        settings.setUid(uid);
        settings.setTimezoneJavaid("Europe/Moscow");
        val settingsInfo = new SettingsInfo(settings, Option.empty());

        val ev = new Event();
        ev.setId(2992L);
        ev.setMainEventId(29920L);
        ev.setStartTs(MoscowTime.dateTime(2021, 11, 11, 10, 0).toInstant());
        ev.setEndTs(MoscowTime.dateTime(2021, 11, 11, 11, 0).toInstant());
        ev.setIsAllDay(false);
        ev.setRecurrenceId(MoscowTime.dateTime(2021, 11, 12, 10, 0).toInstant());
        val mainEv = new MainEvent();
        mainEv.setTimezoneId("Europe/Moscow");
        mainEv.setExternalId("002020293");
        val participants = new EventParticipants(1234, Cf.arrayList(), Cf.arrayList(), Cf.arrayList(), Cf.arrayList());
        val evLayers = new EventLayers(Cf.arrayList());
        val evWithRel = new EventWithRelations(ev, mainEv, participants, evLayers);

        val repetition = RepetitionInstanceInfo.noRepetition(new InstantInterval(MoscowTime.dateTime(2021, 11, 11, 10, 0).toInstant(),
                MoscowTime.dateTime(2021, 11, 11, 11, 0).toInstant()), DateTimeZone.UTC);

        val notification = new NotificationSendManager.NotificationInfo(evNotification, evUser, userInfo, settingsInfo, evWithRel, repetition, 1);
        return new NotificationLogEventJson(notification, NotificationSendResult.cons(NotificationStatus.FATAL_ERROR));
    }

    public static TodoMailLogEventJson todoMailEvent() {
        val uid = PassportUid.cons(122);

        val todoList1 = new TodoList();
        todoList1.setTitle("sometitle");
        val todoItem1 = new TodoItem();
        todoItem1.setTitle("sometitle");
        todoItem1.setCompletionTs(MoscowTime.dateTime(2021, 10, 10, 10, 0).toInstant());
        todoItem1.setDueTs(MoscowTime.dateTime(2021, 10, 10, 11, 0).toInstant());
        val mailAddress = new MailAddress(new Email("ab@yandex-team.ru"));
        val cont = new UnivContact(new Email("ab@yandex-team.ru"), "ab", Option.of(uid));
        val parameters = new TodoMessageParameters(LocalDateTime.parse("2021-11-11T10:00:00"), Cf.arrayList(todoList1),
                Cf.arrayList(todoItem1), mailAddress, cont, Language.RUSSIAN, "http://calendar.yandex-team.ru/", TodoMailType.PLANNED);
        return new TodoMailLogEventJson(parameters,
                "19327382", "SUCCESSFUL", Optional.of("250 2.0.0 Ok: queued as 1E91620002"));
    }

    public static ActionInfo webActionInfo() {
        return new ActionInfo("somename", ActionSource.WEB, "111111", MoscowTime.dateTime(2021, 11, 11, 10, 0).toInstant(), true);
    }

    public static ExternalIdChangeLogEventJson externalIdChangeEvent() {
        return new ExternalIdChangeLogEventJson("111", "222");
    }

    private static EventFieldsChangesJson createEventFieldsChanges(FieldChange<String> name, FieldChange<String> description,
                                                                   FieldChange<String> location, FieldChange<String> url,
                                                                   FieldChange<Instant> start, FieldChange<Instant> end,
                                                                   FieldChange<Integer> sequence) {
        return new EventFieldsChangesJson(name, description, location, url, start, end, sequence);
    }

    private static RepetitionChangesJson createRepetitionChanges(FieldChange<RegularRepetitionRule> type,
                                                                 FieldChange<Integer> each,
                                                                 FieldChange<Instant> due,
                                                                 FieldChange<String> weeklyDays,
                                                                 FieldChange<Boolean> monthlyLastweek,
                                                                 List<Instant> added, List<Instant> removed) {
        val repetitionFieldsChanges = new RepetitionFieldsChangesJson(type, each, due, weeklyDays, monthlyLastweek);
        val exdatesChanges = new ExdatesChangesJson(Cf.toArrayList(added), Cf.toArrayList(removed));
        return new RepetitionChangesJson(repetitionFieldsChanges, exdatesChanges);
    }

    private static ResourcesChangesJson createResourcesChanges(List<ResourcesChangesJson.ResourceIdentJson> removed, List<ResourcesChangesJson.ResourceIdentJson> added) {
        return new ResourcesChangesJson(Cf.toArrayList(removed), Cf.toArrayList(added));
    }

    private static LayersChangesJson createLayersChanges(List<LayersChangesJson.LayerIdentJson> addedList, List<LayersChangesJson.LayerIdentJson> removedList) {
        return new LayersChangesJson(Cf.toArrayList(addedList), Cf.toArrayList(removedList));
    }
}
