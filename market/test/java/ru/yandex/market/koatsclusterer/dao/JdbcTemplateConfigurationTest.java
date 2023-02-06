package ru.yandex.market.koatsclusterer.dao;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import ru.yandex.market.ir.koatsclusterer.be.Offer;
import ru.yandex.market.ir.koatsclusterer.dao.OffersDao;
import ru.yandex.utils.CloseableIterator;
import ru.yandex.utils.jdbc2.DbDialect;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author jkt on 01.02.18.
 */

public class JdbcTemplateConfigurationTest {

    private OffersDao offersDao;

    @Before
    public void initContext() {
        ApplicationContext context = new ClassPathXmlApplicationContext("barcodes-clusterizer.xml");

        offersDao = (OffersDao) context.getBean("offersDao");
    }

    @Test
    public void shouldHavePositiveJdbcTimeout() {
        assertThat(offersDao.getJdbcTemplate().getQueryTimeout()).isPositive();

        CloseableIterator<Offer> offers = offersDao.getOffersByCategoryId(0);

        offers.hasNext();
    }

    @Test
    public void shouldNotFailWithIllegalQueryTimeoutException() {
        CloseableIterator<Offer> offers = offersDao.getOffersByCategoryId(0);

        assertThat(offers.hasNext()).isFalse();
    }
}
