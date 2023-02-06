package ru.yandex.market.abo.cpa.order.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.order.model.CpaOrderStatPhone;
import ru.yandex.market.abo.test.TestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author agavrikov
 * @date 07.05.19
 */
public class CpaOrderStatPhoneRepoTest extends EmptyTest {
    @Autowired
    CpaOrderStatPhoneRepo cpaOrderStatPhoneRepo;

    @Test
    public void testRepo() {
        CpaOrderStatPhone cpaOrderStatPhone = new CpaOrderStatPhone(1L, TestHelper.generateShopId(), 2L);
        cpaOrderStatPhoneRepo.save(cpaOrderStatPhone);
        CpaOrderStatPhone dbCpaOrderStatPhone = cpaOrderStatPhoneRepo.findByIdOrNull(cpaOrderStatPhone.getOrderId());

        assertEquals(cpaOrderStatPhone.getOrderId(), dbCpaOrderStatPhone.getOrderId());
        assertEquals(cpaOrderStatPhone.getShopId(), dbCpaOrderStatPhone.getShopId());
        assertEquals(cpaOrderStatPhone.getUserId(), dbCpaOrderStatPhone.getUserId());
    }
}
