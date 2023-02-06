package ru.yandex.calendar.definition;

import lombok.Value;
import ru.yandex.mail.cerberus.UserType;

@Value
public class User {
    String login;
    UserType type;
}
