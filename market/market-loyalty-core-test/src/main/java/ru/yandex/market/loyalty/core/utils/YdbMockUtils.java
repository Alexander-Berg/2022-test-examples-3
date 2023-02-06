package ru.yandex.market.loyalty.core.utils;

import java.util.Collections;

import ru.yandex.market.loyalty.core.dao.ydb.UserPromoDao;
import ru.yandex.market.loyalty.core.dao.ydb.model.UserPromo;

import static org.mockito.Mockito.when;

/**
 * @author : poluektov
 * date: 2019-08-21.
 */
public class YdbMockUtils {
    public static void mockWholeDao(UserPromo promo, UserPromoDao userPromoDao) {
        when(userPromoDao.selectByUid(promo.getUid())).thenReturn(Collections.singletonList(promo));
    }
}
