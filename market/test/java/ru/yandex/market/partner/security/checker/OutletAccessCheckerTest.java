package ru.yandex.market.partner.security.checker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.partner.security.checker.OutletAccessChecker.OUTLET_ID_PN;


/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(before = "OutletAccessCheckerTest.before.csv")
class OutletAccessCheckerTest extends FunctionalTest {

    @Autowired
    OutletAccessChecker outletAccessChecker;

    @Test
    @DisplayName("Аутлет поставщика. Доступ есть")
    void checkSupplier() {
        MockPartnerRequest request = new MockPartnerRequest(123, 10001, PartnerId.supplierId(1L));
        request.setParam(OUTLET_ID_PN, "100");
        assertTrue(outletAccessChecker.checkTyped(request, new Authority()));
    }

    @Test
    @DisplayName("Аутлет магазина. Доступ есть")
    void checkShop() {
        MockPartnerRequest request = new MockPartnerRequest(123, 10002, PartnerId.supplierId(2L));
        request.setParam(OUTLET_ID_PN, "200");
        assertTrue(outletAccessChecker.checkTyped(request, new Authority()));
    }

    @Test
    @DisplayName("Чужой аутлет. Доступа нет")
    void checkWrong() {
        MockPartnerRequest request = new MockPartnerRequest(123, 10001, PartnerId.supplierId(1L));
        request.setParam(OUTLET_ID_PN, "200");
        assertFalse(outletAccessChecker.checkTyped(request, new Authority()));
    }

    @Test
    @DisplayName("Несуществующий аутлет. Доступа нет")
    void checkNoOutlet() {
        MockPartnerRequest request = new MockPartnerRequest(123, 10001, PartnerId.supplierId(1L));
        request.setParam(OUTLET_ID_PN, "300");
        assertFalse(outletAccessChecker.checkTyped(request, new Authority()));
    }

}
