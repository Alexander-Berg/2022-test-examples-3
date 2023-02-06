package ru.yandex.market.analytics.platform.admin.facade.partner;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.analytics.platform.admin.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.exception.ErrorCode;
import ru.yandex.market.vendors.analytics.core.exception.badrequest.BadRequestException;
import ru.yandex.market.vendors.analytics.core.jpa.entity.partner.ShopPropertiesEntity;

/**
 * @author sergeymironov.
 * <p>
 * Тесты для {@link PartnerFacade}
 */
@DbUnitDataSet(before = "ShopPropertiesTest.before.csv")
public class PartnerFacadeTest extends FunctionalTest {

    private static final long PARTNER_ID = 774;

    @Autowired
    private PartnerFacade partnerFacade;

    @Test
    void getProperties() {
        var actual = partnerFacade.getProperties(PARTNER_ID);
        var expected = new ShopPropertiesEntity(PARTNER_ID, false, false);
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    void setProperties() {
        partnerFacade.setProperties(new ShopPropertiesEntity(PARTNER_ID, true, true));
        var actual = partnerFacade.getProperties(PARTNER_ID);
        var expected = new ShopPropertiesEntity(PARTNER_ID, true, true);
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    void setPropertiesForBadShop() {
        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> partnerFacade.setProperties(
                        new ShopPropertiesEntity(PARTNER_ID + 1, true, true)
                )
        );
        Assertions.assertEquals(
                ErrorCode.BAD_REQUEST,
                exception.getErrorCode()
        );
        var expected = "Shop doesn't exist";
        Assertions.assertEquals(
                expected,
                exception.getMessage()
        );
    }
}
