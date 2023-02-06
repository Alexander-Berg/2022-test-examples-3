package ru.yandex.market.archiving;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link DatasourceArchivingQueueDao}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DatasourceArchivingQueueDaoTest extends FunctionalTest {

    @Autowired
    private DatasourceArchivingQueueDao datasourceArchivingQueueDao;

    @Test
    @DisplayName("Получение списка магазинов для архивации. Без лимита")
    @DbUnitDataSet(before = "csv/datasourceArchivingQueueDao/get.before.csv")
    void testGetDatasourcesWithoutLimit() {
        List<Long> partnerIds = datasourceArchivingQueueDao.getIds(60, null);
        assertThat(partnerIds).containsExactlyInAnyOrder(1012L, 1011L);
    }

    @Test
    @DisplayName("Получение списка магазинов для архивации. С лимитом")
    @DbUnitDataSet(before = "csv/datasourceArchivingQueueDao/get.before.csv")
    void testGetDatasourcesWithLimit() {
        List<Long> partnerIds = datasourceArchivingQueueDao.getIds(60, 1);
        assertThat(partnerIds).singleElement().isEqualTo(1012L);
    }

    @Test
    @DisplayName("Получение списка магазинов для архивации. Принудительная архивация")
    @DbUnitDataSet(before = "csv/datasourceArchivingQueueDao/get.force.before.csv")
    void testGetForceDatasources() {
        // Берем все магазины, которые были в тестах выше. Добавляем им параметр FORCE_ARCHIVING
        // Параметр должен повлиять только на mock-магазины и на тех, кто проходил проверку
        // То есть, флаг не должен позволять архивировать красные магазины, либо магазины с деньгами и тд
        List<Long> partnerIds = datasourceArchivingQueueDao.getIds(60, null);
        assertThat(partnerIds).containsExactlyInAnyOrder(1012L, 1011L, 1014L, 1003L, 1004L, 1005L, 1006L, 1007L, 1008L, 1010L);
    }
}
