package ru.yandex.market.core.contact.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "LinkDaoTest.before.csv")
class LinkDaoTest extends FunctionalTest {
    @Autowired
    private LinkDao linkDao;

    @Test
    void countZeroContactLinks() {
        assertThat(linkDao.countContactLinks(-1)).isEqualTo(0);
    }

    @Test
    void countOneContactLinks() {
        assertThat(linkDao.countContactLinks(2)).isEqualTo(1);
    }
}
