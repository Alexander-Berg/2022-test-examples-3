package ru.yandex.calendar.definition;

import lombok.Value;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;

@Value
public class LayerPerm {
    String login;
    LayerActionClass perm;
}
