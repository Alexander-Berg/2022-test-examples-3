package ru.yandex.market.loyalty.admin.utils;

import org.junit.Test;

import ru.yandex.market.loyalty.admin.controller.PromoDescription;
import ru.yandex.market.loyalty.admin.exception.MarketLoyaltyAdminException;
import ru.yandex.market.loyalty.admin.yt.YtPath;
import ru.yandex.market.loyalty.admin.yt.model.DataCampPromoYtDescription;

import java.io.IOException;

import static ru.yandex.market.loyalty.test.ToStringChecker.checkToStringInSameModule;
import static ru.yandex.market.loyalty.test.ToStringChecker.excludeByClasses;

public class CheckToStringTest {
    @Test
    public void checkToStringInAdminModule() throws IOException {
        checkToStringInSameModule(
                excludeByClasses(MarketLoyaltyAdminException.class, PromoDescription.class, YtPath.class,
                        DataCampPromoYtDescription.class)
        );
    }
}
