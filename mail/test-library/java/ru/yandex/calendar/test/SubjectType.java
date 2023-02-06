package ru.yandex.calendar.test;

/**
 * @author ssytnik
 */
public enum SubjectType {
    USER_ORGANIZER,
    USER_ATTENDEE,
    RESOURCE_ORGANIZER,
    ;

    public boolean isResource() { return this == RESOURCE_ORGANIZER; }
    public boolean isAttendee() { return this == USER_ATTENDEE; }
}
