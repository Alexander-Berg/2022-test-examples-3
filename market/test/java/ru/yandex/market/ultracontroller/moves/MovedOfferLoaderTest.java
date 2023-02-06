package ru.yandex.market.ultracontroller.moves;

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.ultracontroller.dao.ClassifierDao;
import ru.yandex.market.ultracontroller.dao.OfferModelMappingHolder;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.mockito.Mockito.when;

/**
 * @author a-shar.
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class MovedOfferLoaderTest {

    @Mock
    private ClassifierDao classifierDao;

    @Mock
    private OfferModelMappingHolder offerModelMappingHolder;

    private void mockGetMovedObjects(Int2ObjectSortedMap<String> movedGoods) {
        when(classifierDao.getMovedObjects()).thenReturn(movedGoods);
    }

    private void mockOfferMappingsLoad(Set<String> goodIds) {
        when(offerModelMappingHolder.getActualGoodIds()).thenReturn(goodIds);
    }

    @Test
    public void test_nullMap() {
        MovedOfferLoader movedOfferLoader = new MovedOfferLoader();

        mockGetMovedObjects(null);
        mockOfferMappingsLoad(new HashSet<>());

        movedOfferLoader.setClassifierDao(classifierDao);
        movedOfferLoader.setOfferModelMappingHolder(offerModelMappingHolder);
        movedOfferLoader.reload();

        MovedOfferData movedOffers = movedOfferLoader.getMovedOffers(100500);
        Assert.assertTrue(movedOffers.getGoodIdSet().isEmpty());
        Assert.assertEquals(100500, movedOffers.getLastId());
    }

    @Test
    public void test_emptyMap() {
        MovedOfferLoader movedOfferLoader = new MovedOfferLoader();

        mockGetMovedObjects(new Int2ObjectRBTreeMap<>());
        mockOfferMappingsLoad(new HashSet<>());

        movedOfferLoader.setClassifierDao(classifierDao);
        movedOfferLoader.setOfferModelMappingHolder(offerModelMappingHolder);
        movedOfferLoader.reload();

        MovedOfferData movedOffers = movedOfferLoader.getMovedOffers(0);
        Assert.assertTrue(movedOffers.getGoodIdSet().isEmpty());
        Assert.assertEquals(0, movedOffers.getLastId());
    }

    @Test
    public void test_nonEmptyMap() {
        MovedOfferLoader movedOfferLoader = new MovedOfferLoader();

        Int2ObjectSortedMap<String> movedGoods = new Int2ObjectRBTreeMap<>();
        IntStream.range(1, 11).forEach(value -> movedGoods.put(value, String.valueOf(value)));
        mockGetMovedObjects(movedGoods);

        HashSet<String> manuallyMatchedGoodIds = new HashSet<>();
        IntStream.range(1, 16).forEach(v -> manuallyMatchedGoodIds.add(String.valueOf(v)));
        mockOfferMappingsLoad(manuallyMatchedGoodIds);

        movedOfferLoader.setClassifierDao(classifierDao);
        movedOfferLoader.setOfferModelMappingHolder(offerModelMappingHolder);
        movedOfferLoader.reload();

        MovedOfferData movedOffers = movedOfferLoader.getMovedOffers(0);
        Assert.assertEquals(15, movedOffers.getGoodIdSet().size());
        Assert.assertEquals(10, movedOffers.getLastId());

        movedOffers = movedOfferLoader.getMovedOffers(1);
        Assert.assertEquals(15, movedOffers.getGoodIdSet().size());
        Assert.assertEquals(10, movedOffers.getLastId());

        movedOffers = movedOfferLoader.getMovedOffers(10);
        Assert.assertEquals(15, movedOffers.getGoodIdSet().size());
        Assert.assertEquals(10, movedOffers.getLastId());

        movedOffers = movedOfferLoader.getMovedOffers(11);
        Assert.assertEquals(15, movedOffers.getGoodIdSet().size());
        Assert.assertEquals(11, movedOffers.getLastId());
    }
}
