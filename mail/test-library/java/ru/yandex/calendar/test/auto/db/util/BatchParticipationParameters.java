package ru.yandex.calendar.test.auto.db.util;

import ru.yandex.calendar.logic.sharing.Decision;

/**
 * @author Stepan Koltsov
 */
public class BatchParticipationParameters {

    private final long eventId;
    private final TestUserInfo invitee;
    private final long inviteeLayerId;
    private final Decision decision;
    private final boolean organizer;

    public BatchParticipationParameters(long eventId, TestUserInfo invitee, long inviteeLayerId, Decision decision, boolean organizer) {
        this.eventId = eventId;
        this.invitee = invitee;
        this.inviteeLayerId = inviteeLayerId;
        this.decision = decision;
        this.organizer = organizer;
    }

    public long getEventId() {
        return eventId;
    }

    public TestUserInfo getInvitee() {
        return invitee;
    }

    public long getInviteeLayerId() {
        return inviteeLayerId;
    }

    public Decision getDecision() {
        return decision;
    }

    public boolean isOrganizer() {
        return organizer;
    }

} //~
