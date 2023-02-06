package ru.yandex.market.archiving;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.archiving.step.DatasourceArchivingStepType;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.SingleFileCsvProducer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Тесты для {@link DatasourceInArchivingDao}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DatasourceInArchivingDaoTest extends FunctionalTest {
    @Autowired
    private DatasourceInArchivingDao datasourceInArchivingDao;

    @Test
    @DisplayName("Удаление всех, кроме...")
    @DbUnitDataSet(before = "csv/datasourceInArchivingDao/delete.before.csv", after = "csv/datasourceInArchivingDao" +
            "/delete.after.csv")
    void testDeleteAllExcept() {
        datasourceInArchivingDao.deleteAllExcept(Arrays.asList(1001L, 1002L));
    }

    @Test
    @DisplayName("Удаление выбранных")
    @DbUnitDataSet(
            before = "csv/datasourceInArchivingDao/delete.before.csv",
            after = "csv/datasourceInArchivingDao/delete.after.csv")
    void testDelete() {
        datasourceInArchivingDao.delete(1003L);
    }

    @Test
    @DisplayName("Слияение с новыми магазинами")
    @DbUnitDataSet(
            before = "csv/datasourceInArchivingDao/merge.before.csv",
            after = "csv/datasourceInArchivingDao/merge.after.csv"
    )
    void testMerge() {
        datasourceInArchivingDao.merge(
                List.of(1001L, 1002L, 1005L, 1006L),
                SingleFileCsvProducer.Functions.sysdate().toInstant()
        );
    }

    @Test
    @DisplayName("Обновление шага")
    @DbUnitDataSet(
            before = "csv/datasourceInArchivingDao/set.before.csv",
            after = "csv/datasourceInArchivingDao/set.update_at.after.csv"
    )
    void testSetStepWithUpdatedAt() {
        datasourceInArchivingDao.setStep(
                1002L,
                DatasourceArchivingStepType.SECOND_WARNING,
                SingleFileCsvProducer.Functions.sysdate().toInstant()
        );
    }

    @Test
    @DisplayName("Получение магазинов для шага инициализации")
    void testGetForInit() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> datasourceInArchivingDao.getIdsForStep(DatasourceArchivingStepType.INITIAL, null));
    }

    @Test
    @DisplayName("Получение магазинов для шага нотификации за 14 дней")
    @DbUnitDataSet(before = "csv/datasourceInArchivingDao/get14.before.csv")
    void testGetFor14() {
        List<Long> actual = datasourceInArchivingDao.getIdsForStep(DatasourceArchivingStepType.FIRST_WARNING, null);
        assertThat(actual).containsExactlyInAnyOrder(1001L, 1002L);

        List<Long> withLimit = datasourceInArchivingDao.getIdsForStep(DatasourceArchivingStepType.FIRST_WARNING, 1);
        assertThat(withLimit).singleElement().isEqualTo(1001L);
    }

    @Test
    @DisplayName("Получение магазинов для шага нотификации за 2 дня")
    @DbUnitDataSet(before = "csv/datasourceInArchivingDao/get2.before.csv")
    void testGetFor2() {
        List<Long> actual = datasourceInArchivingDao.getIdsForStep(DatasourceArchivingStepType.SECOND_WARNING, null);
        assertThat(actual).containsExactlyInAnyOrder(1008L, 1005L);
    }
}
