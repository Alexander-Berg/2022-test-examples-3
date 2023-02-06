package ru.yandex.direct.core.testing.data;

import java.math.BigInteger;
import java.util.Collections;

import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;

public class TestMinusKeywordsPacks {

    public static MinusKeywordsPack privateMinusKeywordsPack() {
        return new MinusKeywordsPack()
                .withMinusKeywords(Collections.singletonList("private minus"))
                .withHash(BigInteger.ZERO)
                .withIsLibrary(false);
    }

    public static MinusKeywordsPack libraryMinusKeywordsPack() {
        return new MinusKeywordsPack()
                .withName("test name")
                .withMinusKeywords(Collections.singletonList("library minus"))
                .withHash(BigInteger.ZERO)
                .withIsLibrary(true);
    }

}
