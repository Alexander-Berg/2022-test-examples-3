package ru.yandex.market.ultracontroller.ext;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.ir.util.ImmutableMonitoringResult;
import ru.yandex.market.CategoryTree;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ultracontroller.dao.OfferEntity;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Shamil Ablyazov, <a href="mailto:a-shar@yandex-team.ru"/>.
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class MatcherWorkerTest {
    private static final int OLD_CATEGORY_ID = 15686231;
    private static final int CATEGORY_ID1 = 15686232;
    private static final int CATEGORY_ID2 = 15686233;
    @Mock
    private CategoryTree tree;
    private MatcherWorker matcherWorker;

    @Before
    public void init() {
        CategoryTree.CategoryTreeNode node =
            CategoryTree.newCategoryTreeNodeBuilder()
            .setName("test")
            .setHyperId(OLD_CATEGORY_ID)
            .build();
        node.addLinkedCategory(CATEGORY_ID1);
        node.addLinkedCategory(CATEGORY_ID2);

        when(tree.getByHyperId(any())).thenReturn(node);
        matcherWorker = new MatcherWorker();
        matcherWorker.setCategoryTree(tree);
    }

    @Test
    public void generateMatcherRequest() {
        UltraController.Offer offer = UltraController.Offer.newBuilder()
            .setBarcode("100500")
            .build();
        OfferEntity offerEntity = new OfferEntity(offer, ImmutableMonitoringResult.OK);
        offerEntity.setCategoryId(OLD_CATEGORY_ID);
        MatcherWorker.RequestDataAccumulator requestDataAccumulator
            = matcherWorker.generateMatcherRequest(Collections.singletonList(offerEntity));

        assertEquals(3, requestDataAccumulator.getOffers().size());
    }
}