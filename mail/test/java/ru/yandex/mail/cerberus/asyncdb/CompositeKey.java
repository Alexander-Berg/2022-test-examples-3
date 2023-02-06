package ru.yandex.mail.cerberus.asyncdb;

import lombok.Value;

@Value
public class CompositeKey {
    long id;
    String type;
}
