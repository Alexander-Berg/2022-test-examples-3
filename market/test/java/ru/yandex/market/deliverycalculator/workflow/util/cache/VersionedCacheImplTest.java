package ru.yandex.market.deliverycalculator.workflow.util.cache;

import java.util.Collection;
import java.util.function.BiConsumer;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VersionedCacheImplTest {
    @Mock
    private BiConsumer<String, VersionedValue<String>> onDeleteFunc;

    private VersionedCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new VersionedCacheImpl<>();
    }

    @Test
    void it_must_get_last_put_value() {
        // Given, When
        cache.putValue(1, "key1", "value1");
        cache.putValue(1, "key1", "value2");

        // Then
        final String expected = "value2";
        final String actual = cache.getValue(1, "key1");
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void it_mast_leave_last_put_value() {
        // Given
        cache.putValue(1, "key1", "value1");
        cache.putValue(1, "key1", "value2");

        // When
        cache.outdateGeneration(1);

        // Then
        final String expected = "value2";
        final String actual = cache.getValue(1, "key1");
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void it_mast_get_null_value() {
        // Given
        cache.putValue(1, "key1", "value1");
        cache.putValue(1, "key1", "value2");

        // When
        cache.deleteValue(1, "key1");

        // Then
        final String actual = cache.getValue(1, "key1");
        Assertions.assertNull(actual);
    }

    @Test
    void it_must_return_value2() {
        // Given, When
        cache.putValue(1, "key1", "value1");
        cache.putValue(1, "key1", "value2");

        // Then
        final Collection<? extends String> allValues = cache.getAllValues(1);
        MatcherAssert.assertThat(allValues, Matchers.hasSize(1));
        MatcherAssert.assertThat(allValues, Matchers.containsInAnyOrder("value2"));
    }

    @Test
    void it_must_return_value2_and_value3() {
        // Given, When
        cache.putValue(1, "key1", "value1");
        cache.putValue(1, "key1", "value2");
        cache.putValue(2, "key2", "value3");

        // Then
        final Collection<? extends String> allValues = cache.getAllValues(2);
        MatcherAssert.assertThat(allValues, Matchers.hasSize(2));
        MatcherAssert.assertThat(allValues, Matchers.containsInAnyOrder("value2", "value3"));
    }

    @Test
    void it_must_return_value3() {
        // Given, When
        cache.putValue(1, "key1", "value1");
        cache.putValue(1, "key1", "value2");
        cache.putValue(2, "key1", "value3");

        // Then
        final Collection<? extends String> allValues = cache.getAllValues(2);
        MatcherAssert.assertThat(allValues, Matchers.hasSize(1));
        MatcherAssert.assertThat(allValues, Matchers.containsInAnyOrder("value3"));
    }

    @Test
    void it_must_return_value1() {
        // Given, When
        cache.putValue(1, "key1", "value1");
        cache.putValue(2, "key1", "value2");
        cache.putValue(2, "key1", "value3");

        // Then
        final Collection<? extends String> allValues = cache.getAllValues(1);
        MatcherAssert.assertThat(allValues, Matchers.hasSize(1));
        MatcherAssert.assertThat(allValues, Matchers.containsInAnyOrder("value1"));
    }

    @Test
    void it_must_return_value1_when_outdate_generation1() {
        // Given
        cache.putValue(1, "key1", "value1");
        cache.putValue(2, "key1", "value2");
        cache.putValue(2, "key1", "value3");

        // When
        cache.outdateGeneration(1);

        // Then
        final String actual = cache.getValue(1, "key1");
        final String expected = "value1";
        Assertions.assertEquals(expected, actual);

        final Collection<? extends String> allValues = cache.getAllValues(1);
        final Collection<? extends String> allValues2 = cache.getAllValues(2);

        MatcherAssert.assertThat(allValues, Matchers.hasSize(1));
        MatcherAssert.assertThat(allValues, Matchers.containsInAnyOrder("value1"));
        MatcherAssert.assertThat(allValues2, Matchers.hasSize(1));
        MatcherAssert.assertThat(allValues2, Matchers.containsInAnyOrder("value3"));
    }

    @Test
    void it_must_return_value2_when_outdate_generation1() {
        // Given
        cache.putValue(1, "key1", "value1");
        cache.putValue(1, "key1", "value2");
        cache.putValue(2, "key1", "value3");

        // When
        cache.outdateGeneration(1);

        // Then
        final String actual = cache.getValue(1, "key1");
        final String expected = "value2";
        Assertions.assertEquals(expected, actual);

        final Collection<? extends String> allValues = cache.getAllValues(1);
        final Collection<? extends String> allValues2 = cache.getAllValues(2);
        MatcherAssert.assertThat(allValues, Matchers.hasSize(1));
        MatcherAssert.assertThat(allValues, Matchers.containsInAnyOrder("value2"));
        MatcherAssert.assertThat(allValues2, Matchers.hasSize(1));
        MatcherAssert.assertThat(allValues2, Matchers.containsInAnyOrder("value3"));
    }

    @Test
    void it_must_return_value3_when_outdate_generation2() {
        // Given
        cache.putValue(1, "key1", "value1");
        cache.putValue(2, "key1", "value2");
        cache.putValue(2, "key1", "value3");

        // When
        cache.outdateGeneration(2);

        // Then
        final String actual = cache.getValue(2, "key1");
        final String expected = "value3";
        Assertions.assertEquals(expected, actual);

        final Collection<? extends String> allValues = cache.getAllValues(1);
        final Collection<? extends String> allValues2 = cache.getAllValues(2);
        MatcherAssert.assertThat(allValues, Matchers.empty());
        MatcherAssert.assertThat(allValues2, Matchers.hasSize(1));
        MatcherAssert.assertThat(allValues2, Matchers.containsInAnyOrder("value3"));
    }

    @Test
    void it_must_return_value3_when_outdate_generation1() {
        // Given
        cache.putValue(1, "key1", "value1");
        cache.putValue(2, "key1", "value2");
        cache.putValue(2, "key1", "value3");

        // When
        cache.outdateGeneration(1);

        // Then
        final String actual = cache.getValue(2, "key1");
        final String expected = "value3";
        Assertions.assertEquals(expected, actual);

        final Collection<? extends String> allValues = cache.getAllValues(1);
        final Collection<? extends String> allValues2 = cache.getAllValues(2);
        MatcherAssert.assertThat(allValues, Matchers.hasSize(1));
        MatcherAssert.assertThat(allValues, Matchers.containsInAnyOrder("value1"));
        MatcherAssert.assertThat(allValues2, Matchers.hasSize(1));
        MatcherAssert.assertThat(allValues2, Matchers.containsInAnyOrder("value3"));
    }

    @Test
    void it_must_return_value1_when_outdate_generation2() {
        // Given
        cache.putValue(1, "key1", "value1");
        cache.putValue(2, "key1", "value2");
        cache.putValue(2, "key1", "value3");

        // When
        cache.outdateGeneration(2);

        // Then
        final String actual = cache.getValue(1, "key1");
        Assertions.assertNull(actual);

        final Collection<? extends String> allValues = cache.getAllValues(1);
        final Collection<? extends String> allValues2 = cache.getAllValues(2);
        MatcherAssert.assertThat(allValues, Matchers.empty());
        MatcherAssert.assertThat(allValues2, Matchers.hasSize(1));
        MatcherAssert.assertThat(allValues2, Matchers.containsInAnyOrder("value3"));
    }

    @Test
    void outdate_cache_remove_all_values_with_on_delete_func() {
        // Given
        cache.putValue(1, "key1", "value1");
        cache.deleteValue(2, "key1");

        // When
        cache.outdateGeneration(2, onDeleteFunc);

        // Then
        InOrder inOrder = inOrder(onDeleteFunc);
        inOrder.verify(onDeleteFunc).accept("key1", new VersionedCacheValueStruct<>(1, "value1", false));
        inOrder.verify(onDeleteFunc).accept("key1", new VersionedCacheValueStruct<>(2, null, true));
    }

    @Test
    void outdate_cache_do_not_call_on_delete_func() {
        // Given
        cache.putValue(1, "key1", "value1-1");
        cache.putValue(1, "key1", "value1-2");
        cache.putValue(1, "key1", "value1-3");
        cache.putValue(2, "key2", "value2-1");
        cache.putValue(2, "key2", "value2-2");
        cache.putValue(3, "key2", "value2-3");

        // When
        cache.outdateGeneration(2, onDeleteFunc);

        // Then
        verify(onDeleteFunc, never()).accept(eq("key1"), any(VersionedCacheValueStruct.class));
        verify(onDeleteFunc, never()).accept(eq("key2"), any(VersionedCacheValueStruct.class));
    }
}