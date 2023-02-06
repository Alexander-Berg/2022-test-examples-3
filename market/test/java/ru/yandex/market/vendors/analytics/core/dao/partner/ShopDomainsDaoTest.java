package ru.yandex.market.vendors.analytics.core.dao.partner;

import java.util.Comparator;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.model.partner.shop.ShopDataSourceType;
import ru.yandex.market.vendors.analytics.core.model.partner.shop.ShopDomain;

/**
 * Функциональные тесты для {@link ShopDomainsDao}.
 *
 * @author ogonek
 */
@DbUnitDataSet(before = "ShopDomainsDaoTest.before.csv")
public class ShopDomainsDaoTest extends FunctionalTest {

    @Autowired
    private ShopDomainsDao shopDomainsDao;

    @Test
    void getBrandNamesTest() {
        List<ShopDomain> expected = List.of(
                new ShopDomain(774, ShopDataSourceType.METRIKA, 1, "beru.ru"),
                new ShopDomain(774, ShopDataSourceType.METRIKA, 1, "xxx.ru")
        );
        List<ShopDomain> actual = shopDomainsDao.getPartnerBrandDomains();

        Comparator<ShopDomain> comparator = (l, r) -> String.CASE_INSENSITIVE_ORDER.compare(l.domain(), r.domain());

        expected = StreamEx.of(expected).sorted(comparator).toList();
        actual = StreamEx.of(actual).sorted(comparator).toList();

        Assertions.assertEquals(expected, actual);
    }
}
