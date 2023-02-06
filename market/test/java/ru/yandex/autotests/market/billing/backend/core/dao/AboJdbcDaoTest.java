package ru.yandex.autotests.market.billing.backend.core.dao;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.autotests.market.billing.backend.core.dao.abo.AboDao;
import ru.yandex.autotests.market.billing.backend.core.dao.abo.AboDaoFactory;

/**
 * User: jkt
 * Date: 14.11.12
 * Time: 14:57
 */
public class AboJdbcDaoTest {

    private AboDao dao;

    @Before
    public void initDao() {
        dao = AboDaoFactory.getAboDao();
    }

    @Ignore
    @Test
    public void createDao() {

    }
}
