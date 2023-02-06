package ru.yandex.market.ff4shops.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Ff4ShopsRoutingDataSourceTest {
    @Mock
    TransactionUtil transactionUtil;

    /**
     * Проверяет, что на readonly = true транзакцию возвращается READ_ONLY ключ
     */
    @Test
    public void testReadOnlyDataSourceReturned() {
        when(transactionUtil.isCurrentTransactionReadOnly()).thenReturn(true);

        Ff4ShopsRoutingDataSource ff4ShopsRoutingDataSource = new Ff4ShopsRoutingDataSource(transactionUtil);

        assertEquals(Ff4ShopsRoutingDataSource.DataSourceKey.READ_ONLY, ff4ShopsRoutingDataSource.determineCurrentLookupKey());
    }

    /**
     * Проверяет, что на readonly = false транзакцию возвращается READ_WRITE ключ
     */
    @Test
    public void testMasterDataSourceReturned() {
        when(transactionUtil.isCurrentTransactionReadOnly()).thenReturn(false);

        Ff4ShopsRoutingDataSource ff4ShopsRoutingDataSource = new Ff4ShopsRoutingDataSource(transactionUtil);

        assertEquals(Ff4ShopsRoutingDataSource.DataSourceKey.READ_WRITE, ff4ShopsRoutingDataSource.determineCurrentLookupKey());
    }
}
