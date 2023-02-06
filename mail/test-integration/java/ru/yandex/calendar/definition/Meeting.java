package ru.yandex.calendar.definition;

import lombok.Value;

import java.util.List;
import java.util.Optional;

@Value
public class Meeting {
    String name;
    String organizer;
    List<String> attendees;
    Optional<List<String>> optionalAttendees;
    Optional<String> data;
}
