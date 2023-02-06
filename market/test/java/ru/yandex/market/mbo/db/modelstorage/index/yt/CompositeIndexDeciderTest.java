package ru.yandex.market.mbo.db.modelstorage.index.yt;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.HasModelIndexPayload;

import java.util.ArrayList;
import java.util.List;

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

        List<YtIndexReader<HasModelIndexPayload>> fetchers = new ArrayList<>();
        fetchers.add(r1);
        fetchers.add(r2);

        YtIndexFetcherWrapper result1 = new CompositeIndexDecider(fetchers)
            .findFullMatchReader(new MboIndexesFilter()).get();
        Assert.assertEquals(r2, result1.getFetcher());

        when(r1.isSupportFilter(any())).thenReturn(true);

        YtIndexFetcherWrapper result2 = new CompositeIndexDecider(fetchers)
            .findFullMatchReader(new MboIndexesFilter()).get();
        Assert.assertEquals(r1, result2.getFetcher());
    }

    @Test
    public void testEmptyFetcher() {
        assertThat(new CompositeIndexDecider(new ArrayList<>()).findFullMatchReader(
            new MboIndexesFilter()).isPresent()
        ).isFalse();
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testCorrectPartlyByDecider() {
        YtIndexReader r1 = mock(YtIndexReader.class);
        YtIndexReader r2 = mock(YtIndexReader.class);

        when(r1.priority()).thenReturn(10);
        when(r2.priority()).thenReturn(5);

        when(r1.partlySupportRank(any())).thenReturn(5);
        when(r2.partlySupportRank(any())).thenReturn(10);

        List<YtIndexReader<HasModelIndexPayload>> fetchers = new ArrayList<>();
        fetchers.add(r1);
        fetchers.add(r2);

        YtIndexFetcherWrapper result1 = new CompositeIndexDecider(fetchers)
            .findPartlyMatchReader(new MboIndexesFilter()).get();
        Assert.assertEquals(r2, result1.getFetcher());

        when(r2.partlySupportRank(any())).thenReturn(3);

        YtIndexFetcherWrapper result2 = new CompositeIndexDecider(fetchers)
            .findPartlyMatchReader(new MboIndexesFilter()).get();
        Assert.assertEquals(r1, result2.getFetcher());
    }
}
