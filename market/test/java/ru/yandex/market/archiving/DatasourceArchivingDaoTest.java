package ru.yandex.market.archiving;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.archiving.model.DatasourceArchive;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.archive.model.ArchivingData;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DatasourceArchivingDaoTest extends FunctionalTest {

    @Autowired
    private DatasourceArchivingDao datasourceArchivingDao;

    @Test
    @DisplayName("Создание новое записи в таблице архивации магазинов")
    @DbUnitDataSet(after = "csv/datasourceArchiving.creation.after.csv")
    void testCreate() {
        datasourceArchivingDao.save(2L, new ArchivingData("TEST_DATA"));
    }

    @Test
    @DisplayName("Получение записи об архивации")
    @DbUnitDataSet(before = "csv/datasourceArchiving.get.before.csv")
    void testGet() {
        DatasourceArchive partnerArchive = datasourceArchivingDao.get(1L);
        assertThat(partnerArchive)
                .isEqualTo(new DatasourceArchive(1L, LocalDateTime.of(2017, 1, 2, 0, 0), "TEST_DATA_PARTNER_1"));

        DatasourceArchive contactArchive = datasourceArchivingDao.get(2L);
        assertThat(contactArchive)
                .isEqualTo(new DatasourceArchive(2L, LocalDateTime.of(2017, 1, 3, 0, 0), "TEST_DATA_PARTNER_2"));

        DatasourceArchive badArchive = datasourceArchivingDao.get(999L);
        assertThat(badArchive).isNull();
    }
}
