package ru.yandex.market.core.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachingStringTransformerTest {
    SqlTransformers.Transformer tMock = mock(SqlTransformers.Transformer.class);
    SqlTransformers.Transformer tCaching = SqlTransformers.caching(tMock, 3L);

    @BeforeEach
    void setUp() {
        when(tMock.transform(ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0) + "!");
    }

    @Test
    void transform() {
        // when
        var r10 = tCaching.transform("1");
        var r11 = tCaching.transform("1");
        var r12 = tCaching.transform("1");
        var r2 = tCaching.transform("2");
        tCaching.transform("3"); // cache
        tCaching.transform("4"); // cache

        tCaching.transform("1"); // cache miss
        tCaching.transform("4"); // cache hit
        tCaching.transform("3"); // cache hit
        tCaching.transform("2"); // cache miss

        // then
        assertThat(r10).isEqualTo(r11).isEqualTo(r12).isEqualTo("1!");
        assertThat(r2).isEqualTo("2!");
        verify(tMock, times(2).description("there should be cache miss")).transform("1");
        verify(tMock, times(2).description("ditto")).transform("2");
        verify(tMock, times(1)).transform("3");
        verify(tMock, times(1)).transform("4");
    }

    @Test
    void skipsTransformIfSpecified() {
        var query = "rowid";
        assertThat(tCaching.transform(query))
                .as("ensure that we chosen query to be transformed")
                .isEqualTo(query + "!");
        assertThat(tCaching.transform(SqlTransformers.SKIP_TRANSFORM_PREFIX + query))
                .isEqualTo(SqlTransformers.SKIP_TRANSFORM_PREFIX + query);
    }
}
