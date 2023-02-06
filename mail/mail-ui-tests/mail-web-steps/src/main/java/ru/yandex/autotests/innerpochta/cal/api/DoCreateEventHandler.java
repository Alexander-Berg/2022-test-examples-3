package ru.yandex.autotests.innerpochta.cal.api;

import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.calAccount.CalAccount;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdocreateevent.CreateEventBody;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdocreateevent.Model;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdocreateevent.Params;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdocreateevent.Repetition;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.cal.api.CalApiConfig.apiConfig;
import static ru.yandex.autotests.innerpochta.cal.api.InfoHandler.infoHandler;
import static ru.yandex.autotests.innerpochta.cal.util.handlers.SettingsConsts.HANDLER_DO_CREATE_EVENT;

/**
 * @author cosmopanda
 */
public class DoCreateEventHandler {

    private CreateEventBody body;
    private RestAssuredAuthRule filter;
    private CalAccount accInfo;

    private DoCreateEventHandler() {
    }

    public static DoCreateEventHandler createEvent() {
        return new DoCreateEventHandler();
    }

    public DoCreateEventHandler withAuth(RestAssuredAuthRule auth) {
        accInfo = infoHandler().withFilter(auth).callInfoHandler();
        filter = auth;
        return this;
    }

    public DoCreateEventHandler withEvent(Event event) {
        body = new CreateEventBody().withModels(Collections.singletonList(
            new Model().withName(HANDLER_DO_CREATE_EVENT)
                .withParams(new Params()
                    .withName(event.getName())
                    .withDescription(event.getDescription())
                    .withStart(event.getStartTs())
                    .withEnd(event.getEndTs())
                    .withIsAllDay(event.getIsAllDay())
                    .withLocation(event.getLocation())
                    .withLayerId(event.getLayerId())
                    .withAttendees(event.getAttendeesArray())
                    .withAvailability(event.getAvailability())
                    .withOthersCanView(event.getOthersCanView())
                    .withNotifications(Collections.emptyList())
                    .withParticipantsCanEdit(event.getParticipantsCanEdit())
                    .withParticipantsCanInvite(event.getParticipantsCanInvite())
                )));
        return this;
    }

    public DoCreateEventHandler withRepeatEvent(Event event) {
        body = new CreateEventBody().withModels(Collections.singletonList(
            new Model().withName(HANDLER_DO_CREATE_EVENT)
                .withParams(new Params()
                    .withName(event.getName())
                    .withDescription(event.getDescription())
                    .withStart(event.getStartTs())
                    .withEnd(event.getEndTs())
                    .withIsAllDay(event.getIsAllDay())
                    .withLocation(event.getLocation())
                    .withLayerId(event.getLayerId())
                    .withAttendees(event.getAttendeesArray())
                    .withRepetition(new Repetition()
                        .withType(event.getRepetition().getType())
                        .withWeeklyDays(event.getRepetition().getWeeklyDays())
                        .withEach(event.getRepetition().getEach())
                    )
                    .withOthersCanView(event.getOthersCanView())
                    .withNotifications(Collections.emptyList())
                    .withParticipantsCanEdit(event.getParticipantsCanEdit())
                    .withParticipantsCanInvite(event.getParticipantsCanInvite())
                )));
        return this;
    }

    public DoCreateEventHandler withAttendees(List<String> attendees) {
       body.getModels().get(0).setParams(body.getModels().get(0).getParams().withAttendees(attendees));
        return this;
    }

    public Response callCreateEvent() {
        return apiConfig()
            .docreateevent()
            .withCreateEventBody(body)
            .withReq(req -> req.addFilter(filter).addFilter(log()))
            .withXyandexmayauidHeader(accInfo.getUid())
            .withXyandexmayackeyHeader(accInfo.getCkey())
            .post(Function.identity());
    }
}