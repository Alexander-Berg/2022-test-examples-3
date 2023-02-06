package ru.yandex.market.pricelabs.tms.yt;

import lombok.Data;

import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;

@Data
@YTreeObject
class TestV2a extends TestV1 {
    String value2;
    long value3;

    TestV2a() {

    }

    TestV2a(int id, String value) {
        super(id, value);
    }

    TestV2a(int id, String value, String value2, long value3) {
        this(id, value);
        this.value2 = value2;
        this.value3 = value3;
    }

}
