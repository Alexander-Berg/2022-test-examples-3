package ru.yandex.market.mbo.yt.index.read;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.yt.index.LongKey;
import ru.yandex.market.mbo.yt.index.read.data.TestFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author apluhin
 * @created 11/5/20
 */
@SuppressWarnings("checkstyle:magicnumber")
public class CompositeIndexDeciderTest {

    @Test
    @SuppressWarnings("rawtypes")
    public void testCorrectFullMatch() {
        YtIndexReader r1 = mock(YtIndexReader.class);
        YtIndexReader r2 = mock(YtIndexReader.class);

        when(r1.priority()).thenReturn(10);
        when(r2.priority()).thenReturn(5);

        when(r1.isSupportFilter(any())).thenReturn(false);
        when(r2.isSupportFilter(any())).thenReturn(true);

        List<YtIndexReader<LongKey>> fetchers = new ArrayList<>();
        fetchers.add(r1);
        fetchers.add(r2);

        YtIndexFetcherWrapper result1 = new CompositeIndexDecider<>(fetchers)
                .findReader(new TestFilter()).get();
        Assert.assertEquals(r2, result1.getFetcher());

        when(r1.isSupportFilter(any())).thenReturn(true);

        YtIndexFetcherWrapper result2 = new CompositeIndexDecider<>(fetchers)
                .findReader(new TestFilter()).get();
        Assert.assertEquals(r1, result2.getFetcher());
    }

    @Test
    public void testEmptyFetcher() {
        assertThat(new CompositeIndexDecider(new ArrayList<>()).findReader(
                Mockito.mock(SearchFilter.class)).isPresent()
        ).isFalse();
    }

}
