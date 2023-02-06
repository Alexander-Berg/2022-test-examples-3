package ru.yandex.market.api.partner.distribution;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.distribution.model.DistributionPartner;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тесты для {@link DistributionPartnerDao}.
 */
class DistributionPartnerDaoTest extends FunctionalTest {

    @Autowired
    private DistributionPartnerDao distributionPartnerDao;

    @Test
    @DisplayName("Полный список партнеров возвращается корректно")
    @DbUnitDataSet(before = "csv/DistributionPartnerDao.get_all.csv")
    void testGetAll() {
        final int expectedResultSize = 3;
        final int actualResultSize = distributionPartnerDao.getAllDistributionPartners().size();

        Assertions.assertEquals(expectedResultSize, actualResultSize);
    }

    @Test
    @DisplayName("Если партнеров в базе нет, то возвращается пустой список")
    @DbUnitDataSet
    void testGetByIdsEmpty() {
        final List<Long> param = Collections.singletonList(1001L);
        final List<DistributionPartner> expected = Collections.emptyList();
        final List<DistributionPartner> actual = distributionPartnerDao.getDistributionPartnersByIds(param);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("При поиске по нескольким id возвращается корректный результат")
    @DbUnitDataSet(before = "csv/DistributionPartnerDao.get_by_ids_found.csv")
    void testGetByIdsMultiple() {
        final List<Long> params = Arrays.asList(1001L, 1002L);
        // result is found for every parameter
        final long expectedResultSize = params.size();
        final long actualResultSize = distributionPartnerDao.getDistributionPartnersByIds(params).size();

        Assertions.assertEquals(expectedResultSize, actualResultSize);
    }

    @Test
    @DisplayName("Если не существует партнера по указанному id, возвращается null")
    @DbUnitDataSet(before = "csv/DistributionPartnerDao.get_by_id_not_found.csv")
    void testGetByIdNotFound() {
        final DistributionPartner result = distributionPartnerDao.getDistributionPartnerById(1004L);
        Assertions.assertNull(result);
    }

    @Test
    @DisplayName("Если существует партнер по указанному id, информация вернется корректно")
    @DbUnitDataSet(before = "csv/DistributionPartnerDao.get_by_id_not_found.csv")
    void testGetByIdFound() {
        final DistributionPartner expected = new DistributionPartner(1001L, "test1", 2001L);
        final DistributionPartner actual = distributionPartnerDao.getDistributionPartnerById(1001L);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Если не существует партнера по указанному id, возвращается null")
    @DbUnitDataSet(before = "csv/DistributionPartnerDao.get_by_uid_not_found.csv")
    void testGetByUidNotFound() {
        final DistributionPartner result = distributionPartnerDao.getDistributionPartnerByUid(2004L);
        Assertions.assertNull(result);
    }

    @Test
    @DisplayName("Если существует партнер по указанному id, информация вернется корректно")
    @DbUnitDataSet(before = "csv/DistributionPartnerDao.get_by_uid_not_found.csv")
    void testGetByUidFound() {
        final DistributionPartner expected = new DistributionPartner(1001L, "test1", 2001L);
        final DistributionPartner actual = distributionPartnerDao.getDistributionPartnerByUid(2001L);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("При поиске по нескольким id возвращается корректный результат")
    @DbUnitDataSet(before = "csv/DistributionPartnerDao.get_by_uids_found.csv")
    void testGetByUidsMultiple() {
        final List<Long> params = Arrays.asList(2001L, 2002L);
        // result is found for every parameter
        final long expectedResultSize = params.size();
        final long actualResultSize = distributionPartnerDao.getDistributionPartnersByUids(params).size();

        Assertions.assertEquals(expectedResultSize, actualResultSize);
    }
}
