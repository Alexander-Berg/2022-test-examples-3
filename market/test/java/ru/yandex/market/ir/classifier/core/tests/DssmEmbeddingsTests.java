package ru.yandex.market.ir.classifier.core.tests;

import org.apache.commons.lang.NullArgumentException;
import org.junit.Test;

import ru.yandex.market.ir.classifier.dssm.DssmEmbeddings;

public class DssmEmbeddingsTests {

    @Test(expected = NullArgumentException.class)
    public void dssmEmbeddings_constructor_shouldThrowNullArgumentException_whenTitleEmbeddingIsNull() {
        new DssmEmbeddings(null, new float[0]);
    }

    @Test(expected = NullArgumentException.class)
    public void dssmEmbeddings_constructor_shouldThrowNullArgumentException_whenShopCategoryNameEmbeddingIsNull() {
        new DssmEmbeddings(new float[0], null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void dssmEmbeddings_constructor_shouldThrowIllegalArgumentException_whenEmbeddingsHaveDifferentLengths() {
        new DssmEmbeddings(new float[1], new float[2]);
    }
}
