package ru.yandex.market.core.testing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.shop.ShopActionContext;

/**
 * Тесты для методов, изменяющих базу.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "DefaultTestingStatusDaoDirtyTest.before.csv")
public class DefaultTestingStatusDaoDirtyTest extends FunctionalTest {
    private static final long DATASOURCE_ID = 11;
    private static final long TESTING_ID = 22;
    private static final long ACTION_ID = 33;

    @Autowired
    private DefaultTestingStatusDao testingStatusDao;

    @Test
    @DbUnitDataSet
    public void successMakeNeedTesting() {
        TestingState state = new TestingState();
        state.setDatasourceId(DATASOURCE_ID);
        state.setTestingType(TestingType.CPC_PREMODERATION);
        state.setStatus(TestingStatus.INITED);
        testingStatusDao.makeNeedTesting(new ShopActionContext(ACTION_ID, DATASOURCE_ID), state);
    }

    @Test
    @DbUnitDataSet(before = "DefaultTestingStatusDaoDirtyTest.successRemoveFromTesting.before.csv",
            after = "DefaultTestingStatusDaoDirtyTest.successRemoveFromTesting.after.csv")
    public void successRemoveFromTesting() {
        TestingState state = new TestingState();
        state.setDatasourceId(DATASOURCE_ID);
        state.setTestingType(TestingType.CPC_PREMODERATION);
        state.setStatus(TestingStatus.INITED);
        testingStatusDao.removeFromTesting(ACTION_ID, DATASOURCE_ID, TESTING_ID);
    }

    @Test
    @DbUnitDataSet
    public void successUpdate() {
        TestingState state = new TestingState();
        state.setId(TESTING_ID);
        state.setDatasourceId(DATASOURCE_ID);
        state.setTestingType(TestingType.CPC_LITE_CHECK);
        state.setStatus(TestingStatus.CHECKING);
        testingStatusDao.update(new ShopActionContext(ACTION_ID, DATASOURCE_ID), state);
    }
}
