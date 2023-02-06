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
public class PartnerIpCheckerTest extends ClchTest {

    @Autowired
    private SimpleSetChecker partnerIpChecker;

    @Test
    @Disabled
    public void testCompare() {
        final CheckerDescriptor checkerDescriptor = new CheckerDescriptor(0, "testChecker");
        partnerIpChecker.configure(checkerDescriptor);
        final CheckerResult result1 = partnerIpChecker.checkShops(5322, 5462);
        assertEquals("Checker=0: shopId1=5322 shopId2=5462 value1=[194.88.210.84, 194.88.211.130, 212.16.19.186, " +
                        "212.192.233.231, 212.45.24.40, 212.45.24.41, 217.172.29.66, 81.13.89.203, 81.13.89.223, " +
                        "81.200.21.159, 89.191.241.225, null] value2=[194.88.210.84, 194.88.211.130, 212.16.19.186, " +
                        "212.192.233.231, 217.172.29.66, 81.13.89.203, 81.13.89.223, 81.200.21.159, null] result=1.0",
                result1.toString());
        final CheckerResult result2 = partnerIpChecker.checkShops(5982, 3189);
        assertEquals("Checker=0: shopId1=5982 shopId2=3189 value1=[124.157.226.196, 194.85.143.194, 212.45.28.202, " +
                        "217.174.104.34, 217.174.104.35, 217.174.104.36, 78.106.181.159, 78.106.181.234, 78.106.212.188, " +
                        "78.106.214.195, 79.120.123.68, 79.120.41.50, 79.120.55.7, 80.86.244.167, 80.86.253.99, " +
                        "80.86.254.189, 81.13.20.102, 81.211.17.62, 89.222.153.34, 89.222.154.44, 89.222.157.228] " +
                        "value2=[124.157.226.196, 194.85.143.194, 212.45.28.202, 217.174.104.34, 217.174.104.35, " +
                        "217.174.104.36, 78.106.181.159, 78.106.181.234, 78.106.212.188, 78.106.214.195, 79.120.123.68, " +
                        "79.120.41.50, 79.120.55.7, 80.86.244.167, 80.86.253.99, 80.86.254.189, 81.13.20.102, 81.211.17.62, " +
                        "89.222.153.34, 89.222.154.44, 89.222.157.228] result=1.0",
                result2.toString());
    }
}
