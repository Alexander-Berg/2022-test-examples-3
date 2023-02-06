package ru.yandex.mail.cerberus.asyncdb;

import lombok.Value;
import lombok.With;
import ru.yandex.mail.cerberus.asyncdb.annotations.Id;
import ru.yandex.mail.cerberus.asyncdb.annotations.Serial;

@With
@Value
public class EntityWithCompositeKey {
    @Id @Serial Long id;
    @Id String type;
    String data;
}
