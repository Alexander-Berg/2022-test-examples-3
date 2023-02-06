package ru.yandex.market.pricelabs.tms.yt;

import lombok.AllArgsConstructor;
import lombok.Data;

import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeKeyField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;

@Data
@AllArgsConstructor
@YTreeObject
class TestV1a {
    @YTreeKeyField
    long id;
    String value;

    TestV1a() {

    }
}
