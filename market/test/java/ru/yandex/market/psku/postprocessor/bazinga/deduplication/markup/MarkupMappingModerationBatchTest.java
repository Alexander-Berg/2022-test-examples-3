package ru.yandex.market.psku.postprocessor.bazinga.deduplication.markup;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.markup3.api.Markup3Api;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;
import ru.yandex.utils.Pair;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.markup3.api.Markup3Api.ModerationTaskSubtype.TO_PSKU;
import static ru.yandex.market.markup3.api.Markup3Api.TaskType.YANG_MAPPING_MODERATION;
import static ru.yandex.market.markup3.api.Markup3Api.YangMappingModerationInput.ModerationTaskType.MAPPING_MODERATION;

@RunWith(JUnit4.class)
public class MarkupMappingModerationBatchTest {

    private static final long groupId = 1L;
    private static final String taskKey = "key";
    private static final Markup3Api.TaskType taskType = YANG_MAPPING_MODERATION;
    private static final double priority = 100D;


    @Test
    public void whenBuildMarkupRequestThenOk() {
        var batch = new MarkupMappingModerationBatch(groupId, 2, TO_PSKU);

        var targetSkuPsku1 = mock(ClusterContent.class);
        long targetSkuId = 100L;
        when(targetSkuPsku1.getType()).thenReturn(ClusterContentType.PSKU);
        when(targetSkuPsku1.getSkuId()).thenReturn(targetSkuId);

        long firstClusterId = 1L;
        var firstCluster = mock(ClusterContent.class);
        when(firstCluster.getId()).thenReturn(firstClusterId);


        var offer1 = SupplierOffer.Offer.newBuilder()
                .setInternalOfferId(2L)
                .setMarketCategoryId(1000L)
                .setMarketCategoryName("name")
                .build();
        batch.addToBatch(targetSkuPsku1, List.of(
                Pair.makePair(firstCluster, offer1),
                Pair.makePair(mock(ClusterContent.class), SupplierOffer.Offer.newBuilder().build())
        ));

        var request = batch.buildMarkupRequest(taskKey, taskType, priority);
        Assert.assertEquals(taskType, request.getTaskTypeIdentity().getType());
        Assert.assertEquals(taskKey, request.getTaskTypeIdentity().getGroupKey());
        Assert.assertEquals(1, request.getTasksCount());

        var task = request.getTasks(0);
        Assert.assertEquals(firstClusterId, Long.parseLong(task.getExternalKey().getValue()));

        var input = task.getInput().getYangMappingModerationInput();
        Assert.assertEquals(priority, input.getPriority(), 0.1);
        Assert.assertEquals(-1, input.getCategoryId());
        Assert.assertEquals(1, input.getCategoryGroupIdsCount());
        Assert.assertEquals(groupId, input.getCategoryGroupIds(0));

        var yangData = input.getData();
        Assert.assertEquals(MAPPING_MODERATION, yangData.getTaskType());
        Assert.assertEquals(TO_PSKU, yangData.getTaskSubtype());
        Assert.assertEquals(2, yangData.getOffersCount());
        Assert.assertEquals(targetSkuId, yangData.getOffers(0).getTargetSkuId().getValue());
        Assert.assertEquals(offer1.getInternalOfferId(), yangData.getOffers(0).getId());
        Assert.assertEquals(String.valueOf(offer1.getInternalOfferId()), yangData.getOffers(0).getOfferId());
        Assert.assertEquals(offer1.getMarketCategoryId(), yangData.getOffers(0).getCategoryId());
        Assert.assertEquals(offer1.getMarketCategoryName(), yangData.getOffers(0).getCategoryName());
    }
}
