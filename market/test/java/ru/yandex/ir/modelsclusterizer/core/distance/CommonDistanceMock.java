package ru.yandex.ir.modelsclusterizer.core.distance;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import ru.yandex.ir.modelsclusterizer.be.FormalizedOffer;
import ru.yandex.ir.modelsclusterizer.core.classify.models.ClassificationModelProvider;

import java.lang.reflect.InvocationTargetException;


/**
 * Общие моки для тестирования классов, связанных с просчетом расстояния.
 *
 * @author Evgeniya Yakovleva, <a href="mailto:ragvena@yandex-team.ru"/>
 */
public class CommonDistanceMock {
    public final static int MIN_SHOP_ID = 10;
    public final static Int2IntMap GROUP_COUNT_BY_SHOP = Int2IntMaps.singleton(MIN_SHOP_ID, 10);
    private final static FormalizedOffer.FormalizedOfferBuilder OFFER_BUILDER = FormalizedOffer.newBuilder();
    private MatrixnetDistanceCalculator distanceCalculator;

    public CommonDistanceMock() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        OFFER_BUILDER.setMinShopId(MIN_SHOP_ID);

        ClusterDistance clusterDistance = new GroupAverageClusterDistance();

        distanceCalculator = new MatrixnetDistanceCalculator();
        distanceCalculator.setClusterDistance(clusterDistance);
        distanceCalculator.reloadFromPool(new ClassificationModelProvider() {
            PairClassificationModel model = pairToClassify -> getFakeNumber(
                pairToClassify.getFirst().getVendorId(),
                pairToClassify.getSecond().getVendorId()
            );

            @Override
            public PairClassificationModel getModel(int categoryId) {
                return model;
            }
        });

    }


    // симметричная функция
    public static int getFakeNumber(int param1, int param2) {
        return param1 * param2;
    }

    public FormalizedOffer getFormalizedOffer(int fakeNum) {
        return OFFER_BUILDER.setVendorId(fakeNum).build();
    }

    public MatrixnetDistanceCalculator getDistanceCalculator() {
        return distanceCalculator;
    }
}
