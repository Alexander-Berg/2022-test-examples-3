package ru.yandex.ir.modelsclusterizer.core.distance;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.ir.modelsclusterizer.be.CategoryInfo;
import ru.yandex.ir.modelsclusterizer.be.FormalizedOffer;
import ru.yandex.ir.modelsclusterizer.be.FormalizedOffersGroup;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static ru.yandex.ir.modelsclusterizer.core.distance.CommonDistanceMock.GROUP_COUNT_BY_SHOP;

/**
 * Актуальный тест, должен работать
 *
 * @author Evgeniya Yakovleva, <a href="mailto:ragvena@yandex-team.ru"/>
 */
public class MatrixnetDistanceCalculatorTest {
    private CommonDistanceMock commonDistanceMock;
    private MatrixnetDistanceCalculator distanceCalculator;
    private CategoryInfo categoryInfo;

    @Before
    public void mock() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        commonDistanceMock = new CommonDistanceMock();
        distanceCalculator = commonDistanceMock.getDistanceCalculator();

        categoryInfo = Mockito.mock(CategoryInfo.class);
        when(categoryInfo.getCategoryId()).thenReturn(42);
    }

    @Test
    public void weightMatrixConsistencyTest() throws NoSuchMethodException {
        int vectorSize = 40;

        List<FormalizedOffer> allFormalizedOffers = new ArrayList<>();
        List<FormalizedOffer> formalizedOffersFiletred = new ArrayList<>();
        List<FormalizedOffersGroup> formalizedOffersGroups = new ArrayList<>();
        Random random = new Random(42);
        for (int position = 0; position < vectorSize; position++) {
            FormalizedOffer formalizedOffer = commonDistanceMock.getFormalizedOffer(random.nextInt(200000));
            allFormalizedOffers.add(formalizedOffer);
            if (position % 2 == 0 && position % 3 == 0) {
                formalizedOffersFiletred.add(formalizedOffer);
                formalizedOffersGroups.add(FormalizedOffersGroup.ofPositionWithoutGuruModel(position));
            }
        }

        double[][] offerDistance = distanceCalculator.getDistancesMatrix(
            formalizedOffersFiletred, categoryInfo, GROUP_COUNT_BY_SHOP
        );
        DistanceMatrix groupDistance = distanceCalculator.getDistancesMatrix(
            formalizedOffersGroups, allFormalizedOffers, categoryInfo, GROUP_COUNT_BY_SHOP
        );

        assertEquals("getDistancesVector for FormalizedOffer and FormalizedOffersGroup should be equal",
            true, Arrays.deepEquals(offerDistance, groupDistance.getGroupwiseDistances()));

        assertEquals("getDistancesVector for FormalizedOffer and FormalizedOffersGroup should be equal",
            true, Arrays.deepEquals(offerDistance, groupDistance.getOfferwiseDistance()));
    }
}
