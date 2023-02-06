package ru.yandex.ir.common.yt;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.ir.common.features.relevance.base.TSignedPosting;

class JsonUtilsTest {

    @Test
    void parseInvertedIndex() {
        String str = "{\"220\":[{\"Break\":2,\"Word\":6,\"Relev\":3,\"Nform\":0}],\"next\":[{\"Break\":2,\"Word\":2," +
                "\"Relev\":3,\"Nform\":0}],\"mattress\":[{\"Break\":2,\"Word\":1,\"Relev\":3,\"Nform\":0}]," +
                "\"матрас\":[{\"Break\":1,\"Word\":1,\"Relev\":3,\"Nform\":0}],\"mr\":[{\"Break\":1,\"Word\":2," +
                "\"Relev\":3,\"Nform\":0}],\"line\":[{\"Break\":2,\"Word\":3,\"Relev\":3,\"Nform\":0}]," +
                "\"x\":[{\"Break\":2,\"Word\":5,\"Relev\":3,\"Nform\":0}],\"190\":[{\"Break\":2,\"Word\":4," +
                "\"Relev\":3,\"Nform\":0}]}";

        Map<String, List<TSignedPosting>> stringListMap = JsonUtils.parseInvertedIndex(str);
        Assertions.assertEquals(8, stringListMap.size());
        Assertions.assertEquals(1, stringListMap.get("220").size());
        Assertions.assertEquals(2, stringListMap.get("220").get(0).Break);
        Assertions.assertEquals(6, stringListMap.get("220").get(0).Word);
        Assertions.assertEquals(3, stringListMap.get("220").get(0).Relev);
        Assertions.assertEquals(0, stringListMap.get("220").get(0).Nform);

    }
}
