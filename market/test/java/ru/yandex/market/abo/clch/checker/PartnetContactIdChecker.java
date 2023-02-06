package ru.yandex.market.abo.clch.checker;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.clch.ClchTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 *         @date 03.04.2008
 */
public class PartnetContactIdChecker extends ClchTest {

    @Autowired
    private SimpleSetChecker partnerContactIdChecker;

    @Test
    @Disabled
    public void testCompare() {
        final CheckerDescriptor checkerDescriptor = new CheckerDescriptor(0, "testChecker");
        partnerContactIdChecker.configure(checkerDescriptor);
        final CheckerResult result1 = partnerContactIdChecker.checkShops(211, 6111);
        assertEquals("Checker=0: shopId1=211 shopId2=6111 value1=[394] value2=[448] result=0.0",
                result1.toString());
        final CheckerResult result2 = partnerContactIdChecker.checkShops(6170, 1603);
        assertEquals("Checker=0: shopId1=6170 shopId2=1603 value1=[181] value2=[181] result=1.0",
                result2.toString());
    }
}
