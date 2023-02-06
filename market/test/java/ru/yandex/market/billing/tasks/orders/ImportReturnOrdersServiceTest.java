package ru.yandex.market.billing.tasks.orders;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.order.returns.ReturnOrdersDao;
import ru.yandex.market.core.order.returns.model.ReturnOrdersItem;
import ru.yandex.market.core.util.DateTimes;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link ImportReturnOrdersService}
 */
@DbUnitDataSet(before = "db/ReturnOrdersServiceTest.before.csv")
@ExtendWith(MockitoExtension.class)
public class ImportReturnOrdersServiceTest extends FunctionalTest {
    private static final Instant PROCESSING_TIME = DateTimes.toInstantAtDefaultTz(2019, 1, 1, 10, 15, 25);

    @Autowired
    private ReturnOrdersDao dao;
    @Autowired
    private ReturnOrdersYTDao ytDao;
    @Autowired
    private ImportReturnOrdersService service;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Captor
    private ArgumentCaptor<List<ReturnOrdersItem>> daoStoreRecordsCaptor;

    @DisplayName("Основной тест")
    @DbUnitDataSet(
            before = "db/ReturnOrdersServiceTest.basicTest.before.csv",
            after = "db/ReturnOrdersServiceTest.basicTest.after.csv"
    )
    @Test
    void basicTest() {
        long lastReturnId = dao.loadMaxReturnId();
        assertEquals(2L, lastReturnId);

        service.importData(PROCESSING_TIME);
    }

    @DisplayName("Тест на то, что данные разбиваются по батчам")
    @Test
    void testBatch() {
        ReturnOrdersDao dao = mock(ReturnOrdersDao.class);
        ReturnOrdersYTDao ytDao = mock(ReturnOrdersYTDao.class);
        ImportReturnOrdersService customService = new ImportReturnOrdersService(dao, ytDao, transactionTemplate);

        when(dao.loadMaxReturnId()).thenReturn(0L);
        when(ytDao.loadRecords(0L, PROCESSING_TIME))
                .thenReturn(Stream.generate(() -> mock(ReturnOrdersItem.class))
                        .limit(600)
                        .collect(Collectors.toList()));

        customService.importData(PROCESSING_TIME);

        verify(ytDao).loadRecords(0L, PROCESSING_TIME);
        verify(dao, times(2)).storeRecords(daoStoreRecordsCaptor.capture());

        assertThat(daoStoreRecordsCaptor.getAllValues(), hasSize(2));
        assertThat(daoStoreRecordsCaptor.getAllValues().get(0), hasSize(500));
        assertThat(daoStoreRecordsCaptor.getAllValues().get(1), hasSize(100));
    }

    @DisplayName("Тест на то, что данные не записываются, если их нет")
    @Test
    @DbUnitDataSet(
            before = "db/ReturnOrdersServiceTest.emptyRecords.before.csv",
            after = "db/ReturnOrdersServiceTest.emptyRecords.after.csv"
    )
    void emptyLoadedRecords() {
        service.importData(PROCESSING_TIME);
    }

    @DisplayName("Тест на фильтрацию null по полю item_id")
    @DbUnitDataSet(
            before = "db/ReturnOrdersServiceTest.nullItemId.before.csv",
            after = "db/ReturnOrdersServiceTest.nullItemId.after.csv"
    )
    @Test
    void testNullItemId() {
        service.importData(PROCESSING_TIME);
    }

    @Test
    @DisplayName("Тест на дублирующиеся записи")
    @DbUnitDataSet(
            before = "db/ReturnOrdersServiceTest.duplicate.before.csv",
            after = "db/ReturnOrdersServiceTest.duplicate.after.csv"
    )
    void testDuplicate() {
        service.importData(PROCESSING_TIME);
    }

    @Test
    @DisplayName("Тест на игнор itemIds")
    @DbUnitDataSet(
            before = "db/ReturnOrdersServiceTest.ignoredItemIds.before.csv",
            after = "db/ReturnOrdersServiceTest.ignoredItemIds.after.csv"
    )
    void testIgnoreItemIds() {
        service.importData(PROCESSING_TIME);
    }
}
