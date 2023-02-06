package ru.yandex.market.pricelabs.tms.yt;

import lombok.Data;

import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;

@Data
@YTreeObject
class TestV2 extends TestV1 {
    String value2;
    int value3;

    TestV2() {

    }

    TestV2(int id, String value) {
        super(id, value);
    }

    TestV2(int id, String value, String value2, int value3) {
        this(id, value);
        this.value2 = value2;
        this.value3 = value3;
    }

}
