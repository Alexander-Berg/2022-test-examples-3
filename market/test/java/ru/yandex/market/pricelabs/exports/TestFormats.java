package ru.yandex.market.pricelabs.exports;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;

@Data
@YTreeObject
public class TestFormats {
    private String a;
    private String b;

    @JsonProperty("c")
    private String cc;
    private long d;
    private List<Item> x;

    @Data
    @AllArgsConstructor
    @YTreeObject
    public static class Item {

        private String a;
        private int b;

        public Item() {
            //
        }

    }
}
