package ru.yandex.market.pricelabs.model;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Slf4j
class AutostrategyTest {

    @Test
    void test() {
        val a1 = new Autostrategy();
        a1.setShop_id(1);
        a1.setFilter_id(1);

        val a2 = new Autostrategy();
        a2.setShop_id(2);
        a2.setFilter_id(2);

        log.info("a1 = {}", a1);
        log.info("a2 = {}", a2);

        assertNotEquals(a1, a2);

        val a3 = new Autostrategy();
        a3.setShop_id(1);
        a3.setFilter_id(1);
        assertEquals(a1, a3);
    }

}
