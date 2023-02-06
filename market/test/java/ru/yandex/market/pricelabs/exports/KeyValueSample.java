package ru.yandex.market.pricelabs.exports;

import lombok.AllArgsConstructor;
import lombok.Data;

import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.market.pricelabs.misc.ToJsonString;

@AllArgsConstructor
@Data
@YTreeObject
public class KeyValueSample implements ToJsonString {

    private int key;
    private String value;

    public KeyValueSample() {
        //
    }

}
