package ru.yandex.market.core.testing;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Тесты для методов, не изменяющих базу.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "DefaultTestingStatusDaoNonDirtyTest.before.csv")
public class DefaultTestingStatusDaoNonDirtyTest extends FunctionalTest {
    @Autowired
    private DefaultTestingStatusDao testingStatusDao;

    @Test
    @DbUnitDataSet
    public void nullLoadingByShopAndProgram() {
        assertNull(testingStatusDao.load(1, ShopProgram.CPC));
    }

    @Test
    @DbUnitDataSet
    public void successLoadingByShopAndProgram() {
        TestingState testingState = testingStatusDao.load(2, ShopProgram.CPC);

        assertEquals(testingState.getStatus(), TestingStatus.CHECKING);
        assertEquals(testingState.getTestingType(), TestingType.CPC_PREMODERATION);
    }

    @Test
    @DbUnitDataSet
    public void successLoadingByShop() {
        List<TestingState> testingStates = testingStatusDao.load(2);

        assertEquals(testingStates.size(), 2);
    }

    @Test
    @DbUnitDataSet
    public void successLoadingById() {
        TestingState testingState = testingStatusDao.loadById(1000000001);

        assertEquals(testingState.getStatus(), TestingStatus.CHECKING);
        assertEquals(testingState.getTestingType(), TestingType.CPC_PREMODERATION);
    }

    @Test
    @DbUnitDataSet
    public void successLoadingManyByShopProgram() {
        Map<Long, TestingState> stateMap = testingStatusDao.loadMany(Collections.singletonList(2L), ShopProgram.CPA);
        assertEquals(stateMap.size(), 1);

        TestingState testingState = stateMap.get(2L);
        assertEquals(testingState.getStatus(), TestingStatus.CHECKING);
        assertEquals(testingState.getTestingType(), TestingType.CPA_PREMODERATION);
    }

    @Test
    @DbUnitDataSet
    public void successLoadingMany() {
        Map<Long, Collection<TestingState>> stateMap = testingStatusDao.loadMany(Collections.singletonList(2L));
        assertEquals(stateMap.size(), 1);

        Map<ShopProgram, TestingState> testingStates = stateMap.get(2L).stream().
                collect(Collectors.toMap(t -> t.getTestingType().getShopProgram(), Function.identity()));

        assertEquals(testingStates.get(ShopProgram.CPC).getStatus(), TestingStatus.CHECKING);
        assertEquals(testingStates.get(ShopProgram.CPC).getTestingType(), TestingType.CPC_PREMODERATION);

        assertEquals(testingStates.get(ShopProgram.CPA).getStatus(), TestingStatus.CHECKING);
        assertEquals(testingStates.get(ShopProgram.CPA).getTestingType(), TestingType.CPA_PREMODERATION);
    }
}
