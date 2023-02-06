package ru.yandex.market.psku.postprocessor.bazinga.deduplication.markup;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import ru.yandex.utils.Pair;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.markup3.api.Markup3Api.ModerationTaskSubtype.TO_PSKU;
import static ru.yandex.market.markup3.api.Markup3Api.ModerationTaskSubtype.UNDEFINED;

@RunWith(JUnit4.class)
public class MarkupMappingModerationBatchesHolderTest {

    private static final long groupId = 1L;

    @Test
    public void baseOverflow() {
        var holder = new MarkupMappingModerationBatchesHolder(groupId, 2);

        var targetSkuPsku1 = mock(ClusterContent.class);
        when(targetSkuPsku1.getType()).thenReturn(ClusterContentType.PSKU);
        var cc1 = mock(ClusterContent.class);
        when(cc1.getClusterMetaId()).thenReturn(1L);
        holder.add(targetSkuPsku1, List.of(
                Pair.makePair(cc1, SupplierOffer.Offer.newBuilder().build())
        ));

        var targetSkuPsku2 = mock(ClusterContent.class);
        when(targetSkuPsku2.getType()).thenReturn(ClusterContentType.PSKU);
        var cc2 = mock(ClusterContent.class);
        when(cc2.getClusterMetaId()).thenReturn(2L);

        var cc3 = mock(ClusterContent.class);
        when(cc3.getClusterMetaId()).thenReturn(2L);

        holder.add(targetSkuPsku2, List.of(
                Pair.makePair(cc2, SupplierOffer.Offer.newBuilder().build()),
                Pair.makePair(cc3, SupplierOffer.Offer.newBuilder().build())
        ));

        holder.summarize();
        Assert.assertEquals(2, holder.getPreparedBatches().size());
        Assert.assertTrue(
                holder.getPreparedBatches()
                        .stream()
                        .map(b -> b.moderationTaskSubtype)
                        .allMatch(s -> s == TO_PSKU)
        );
        Assert.assertEquals(2, holder.getPreparedBatches().get(0).getClusterContents().size());
        Assert.assertEquals(2, holder.getPreparedBatches().get(0).targetSkuOffers.size());
        Assert.assertEquals(1, holder.getPreparedBatches().get(1).getClusterContents().size());
        Assert.assertEquals(1, holder.getPreparedBatches().get(1).targetSkuOffers.size());

        Assert.assertEquals(1, holder.getPreparedBatches().get(0).getProcessedClusters().size());
        Assert.assertEquals(1, holder.getPreparedBatches().get(1).getProcessedClusters().size());
    }

    @Test
    public void overflowAfterFullBatch() {
        var holder = new MarkupMappingModerationBatchesHolder(groupId, 2);

        var targetSkuPsku1 = mock(ClusterContent.class);
        when(targetSkuPsku1.getType())
                .thenReturn(ClusterContentType.PSKU);
        holder.add(targetSkuPsku1, List.of(
                Pair.makePair(mock(ClusterContent.class), SupplierOffer.Offer.newBuilder().build()),
                Pair.makePair(mock(ClusterContent.class), SupplierOffer.Offer.newBuilder().build())
        ));

        var targetSkuPsku2 = mock(ClusterContent.class);
        when(targetSkuPsku2.getType())
                .thenReturn(ClusterContentType.PSKU);
        holder.add(targetSkuPsku2, List.of(
                Pair.makePair(mock(ClusterContent.class), SupplierOffer.Offer.newBuilder().build())
        ));

        holder.summarize();
        Assert.assertEquals(2, holder.getPreparedBatches().size());
        Assert.assertTrue(
                holder.getPreparedBatches()
                        .stream()
                        .map(b -> b.moderationTaskSubtype)
                        .allMatch(s -> s == TO_PSKU)
        );
        Assert.assertEquals(2, holder.getPreparedBatches().get(0).getClusterContents().size());
        Assert.assertEquals(2, holder.getPreparedBatches().get(0).targetSkuOffers.size());
        Assert.assertEquals(1, holder.getPreparedBatches().get(1).getClusterContents().size());
        Assert.assertEquals(1, holder.getPreparedBatches().get(1).targetSkuOffers.size());
    }

    @Test
    public void multiType() {
        var holder = new MarkupMappingModerationBatchesHolder(groupId, 2);
        var targetSkuPsku = mock(ClusterContent.class);
        when(targetSkuPsku.getType())
                .thenReturn(ClusterContentType.PSKU);

        var targetSkuOther = mock(ClusterContent.class);
        when(targetSkuOther.getType())
                .thenReturn(ClusterContentType.DSBS);

        holder.add(targetSkuPsku, List.of(
                Pair.makePair(mock(ClusterContent.class), SupplierOffer.Offer.newBuilder().build())
        ));
        holder.add(targetSkuOther, List.of(
                Pair.makePair(mock(ClusterContent.class), SupplierOffer.Offer.newBuilder().build()),
                Pair.makePair(mock(ClusterContent.class), SupplierOffer.Offer.newBuilder().build())
        ));

        holder.summarize();
        Assert.assertEquals(2, holder.getPreparedBatches().size());
        Assert.assertTrue(holder.getPreparedBatches().stream().map(b -> b.moderationTaskSubtype).collect(Collectors.toList()).containsAll(List.of(TO_PSKU, UNDEFINED)));

        var toPsku =
                holder.getPreparedBatches().stream().filter(b -> b.moderationTaskSubtype == TO_PSKU).findAny().get();
        Assert.assertEquals(1, toPsku.getClusterContents().size());
        Assert.assertEquals(1, toPsku.targetSkuOffers.size());

        var undefined =
                holder.getPreparedBatches().stream().filter(b -> b.moderationTaskSubtype == UNDEFINED).findAny().get();
        Assert.assertEquals(2, undefined.getClusterContents().size());
        Assert.assertEquals(1, undefined.targetSkuOffers.size());

    }

}
