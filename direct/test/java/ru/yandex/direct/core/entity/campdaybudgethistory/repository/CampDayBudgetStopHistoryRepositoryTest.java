package ru.yandex.direct.core.entity.campdaybudgethistory.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.jooq.DSLContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbschema.ppc.tables.records.CampDayBudgetStopHistoryRecord;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.exception.RollbackException;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_DAY_BUDGET_STOP_HISTORY;

/**
 * Тесты на репозиторий
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampDayBudgetStopHistoryRepositoryTest {

    private static final int SHARD = 1;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private CampDayBudgetStopHistoryRepository campDayBudgetStopHistoryRepository;

    /**
     * Тестируем, что метод не возвращает не устаревшие данные.
     */
    @Test
    public void getIdsOfExpiredRecords_withNotExpiredRecord_emptyList() {
        runWithEmptyCampDayBudgetStopHistoryTable(dsl -> {
            CampDayBudgetStopHistoryRecord record = makeRecord();
            dsl.insertInto(CAMP_DAY_BUDGET_STOP_HISTORY)
                    .set(record)
                    .execute();

            List<Long> actual = campDayBudgetStopHistoryRepository.getIdsOfExpiredRecords(dsl,
                    LocalDateTime.now().minusDays(1), 10);

            assertThat(actual).isEmpty();
        });
    }

    /**
     * Тестируем, что метод возвращает устаревшие данные.
     */
    @Test
    public void getIdsOfExpiredRecords_withExpiredRecord_listWithRecord() {
        runWithEmptyCampDayBudgetStopHistoryTable(dsl -> {
            CampDayBudgetStopHistoryRecord record = makeRecord();
            dsl.insertInto(CAMP_DAY_BUDGET_STOP_HISTORY)
                    .set(record)
                    .execute();

            List<Long> actual = campDayBudgetStopHistoryRepository.getIdsOfExpiredRecords(dsl,
                    LocalDateTime.now().plusDays(1), 10);

            assertThat(actual).containsOnly(1L);
        });
    }

    /**
     * Тестируем, что метод возвращает не больше, чем limit записей.
     */
    @Test
    public void getIdsOfExpiredRecords_withMoreRecordsThenLimit_listOfLimitedSize() {
        runWithEmptyCampDayBudgetStopHistoryTable(dsl -> {
            CampDayBudgetStopHistoryRecord record1 = makeRecord();
            CampDayBudgetStopHistoryRecord record2 = makeRecord();
            record2.setId(2L);
            dsl.insertInto(CAMP_DAY_BUDGET_STOP_HISTORY)
                    .set(record1)
                    .set(record2)
                    .execute();

            List<Long> actual = campDayBudgetStopHistoryRepository.getIdsOfExpiredRecords(dsl,
                    LocalDateTime.now().plusDays(1), 1);

            assertThat(actual).hasSize(1);
        });
    }


    /**
     * Тестируем, что из пустой таблицы ничего не удаляется.
     */
    @Test
    public void delete_noRecordsInTable_notDelete() {
        runWithEmptyCampDayBudgetStopHistoryTable(dsl -> {
            int deleted = campDayBudgetStopHistoryRepository.delete(dsl, List.of(1L));

            assertThat(deleted).isEqualTo(0);
        });
    }

    /**
     * Тестируем, что по пустому списку ключей из таблицы ничего не удаляется.
     */
    @Test
    public void delete_noIds_notDeleted() {
        runWithEmptyCampDayBudgetStopHistoryTable(dsl -> {
            CampDayBudgetStopHistoryRecord record = makeRecord();
            dsl.insertInto(CAMP_DAY_BUDGET_STOP_HISTORY)
                    .set(record)
                    .execute();

            int deleted = campDayBudgetStopHistoryRepository.delete(dsl, Collections.emptyList());

            assertThat(deleted).isEqualTo(0);
        });
    }

    /**
     * Тестируем, что метод удаляет запись с ключом из переданного списка.
     */
    @Test
    public void delete_withId_recordDeleted() {
        runWithEmptyCampDayBudgetStopHistoryTable(dsl -> {
            CampDayBudgetStopHistoryRecord record = makeRecord();
            dsl.insertInto(CAMP_DAY_BUDGET_STOP_HISTORY)
                    .set(record)
                    .execute();

            int deleted = campDayBudgetStopHistoryRepository.delete(dsl, List.of(1L));

            assertThat(deleted).isEqualTo(1);
        });
    }

    /**
     * Тестируем, что записи из таблицы, чьи ключи мы не передали на удаление, остаются неудаленными.
     */
    @Test
    public void delete_withDifferentId_recordNotDeleted() {
        runWithEmptyCampDayBudgetStopHistoryTable(dsl -> {
            CampDayBudgetStopHistoryRecord record = makeRecord();
            dsl.insertInto(CAMP_DAY_BUDGET_STOP_HISTORY)
                    .set(record)
                    .execute();

            int deleted = campDayBudgetStopHistoryRepository.delete(dsl, List.of(2L));

            assertThat(deleted).isEqualTo(0);
        });
    }

    @QueryWithoutIndex("Удаление по всей таблице без ключей")
    private void runWithEmptyCampDayBudgetStopHistoryTable(Consumer<DSLContext> test) {
        try {
            dslContextProvider.ppcTransaction(SHARD, configuration -> {
                DSLContext dsl = configuration.dsl();
                dsl.deleteFrom(CAMP_DAY_BUDGET_STOP_HISTORY).execute();

                test.accept(dsl);

                throw new RollbackException();
            });
        } catch (RollbackException ignored) {
        }
    }

    private CampDayBudgetStopHistoryRecord makeRecord() {
        CampDayBudgetStopHistoryRecord record = new CampDayBudgetStopHistoryRecord();
        record.setId(1L);
        record.setCid(1L);
        record.setStopTime(LocalDateTime.now());
        return record;
    }
}
