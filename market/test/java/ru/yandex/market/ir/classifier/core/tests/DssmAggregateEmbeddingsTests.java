package ru.yandex.market.ir.classifier.core.tests;

import org.apache.commons.lang.NullArgumentException;
import org.junit.Test;

import ru.yandex.market.ir.classifier.dssm.DssmAggregateEmbeddings;
import ru.yandex.market.ir.classifier.dssm.DssmEmbeddings;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class DssmAggregateEmbeddingsTests {

    @Test()
    public void dssmAggregateEmbeddings_noItemsAdded_getCount_shouldReturnZero() {
        assertEquals(0, new DssmAggregateEmbeddings().getCount());
    }

    @Test(expected = RuntimeException.class)
    public void dssmAggregateEmbeddings_noItemsAdded_calculateCenter_shouldThrowRuntimeException() {
        new DssmAggregateEmbeddings().calculateCenter();
    }

    @Test()
    public void dssmAggregateEmbeddings_singleItemAdded_getCount_shouldReturnOne() {
        var aggregateEmbeddings = new DssmAggregateEmbeddings();
        aggregateEmbeddings.add(new DssmEmbeddings(new float[0], new float[0]));
        assertEquals(1, aggregateEmbeddings.getCount());
    }

    @Test()
    public void dssmAggregateEmbeddings_singleItemAdded_calculateCenter_shouldReturnTheSameItem() {
        // arrange
        var aggregateEmbeddings = new DssmAggregateEmbeddings();
        var embeddings = new DssmEmbeddings(new float[] { 1, 2, 3 }, new float[] { 4, 5, 6 });
        aggregateEmbeddings.add(embeddings);

        // act
        var center = aggregateEmbeddings.calculateCenter();

        // assert
        assertArrayEquals(embeddings.getTitle(), center.getTitle(), 0f);
        assertArrayEquals(embeddings.getShopCategoryName(), center.getShopCategoryName(), 0f);
    }

    @Test()
    public void dssmAggregateEmbeddings_twoItemsAdded_getCount_shouldReturnTwo() {
        var aggregateEmbeddings = new DssmAggregateEmbeddings();
        aggregateEmbeddings.add(new DssmEmbeddings(new float[0], new float[0]));
        aggregateEmbeddings.add(new DssmEmbeddings(new float[0], new float[0]));
        assertEquals(2, aggregateEmbeddings.getCount());
    }

    @Test()
    public void dssmAggregateEmbeddings_twoItemsAdded_calculateCenter_shouldReturnExpectedValue() {
        // arrange
        var aggregateEmbeddings = new DssmAggregateEmbeddings();
        aggregateEmbeddings.add(new DssmEmbeddings(new float[] { 1, 2, 3 }, new float[] { 4, 5, 6 }));
        aggregateEmbeddings.add(new DssmEmbeddings(new float[] { -1, -2, -3 }, new float[] { 0, 0, 0 }));

        // act
        var center = aggregateEmbeddings.calculateCenter();

        // assert
        assertArrayEquals(new float[] { 0, 0, 0 }, center.getTitle(), 0f);
        assertArrayEquals(new float[] { 2, 2.5f, 3 }, center.getShopCategoryName(), 0f);
    }

    @Test(expected = NullArgumentException.class)
    public void dssmAggregateEmbeddings_addNullEmbeddings_shouldThrowNullArgumentException() {
        new DssmAggregateEmbeddings().add(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void dssmAggregateEmbeddings_addEmbeddingsOfDifferentLengthThanBefore_shouldThrowIllegalArgumentException() {
        var aggregateEmbeddings = new DssmAggregateEmbeddings();
        aggregateEmbeddings.add(new DssmEmbeddings(new float[] { 1, 2, 3 }, new float[] { 4, 5, 6 }));
        aggregateEmbeddings.add(new DssmEmbeddings(new float[] { 1 }, new float[] { 2 }));
    }
}
