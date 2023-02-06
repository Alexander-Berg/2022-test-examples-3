package ru.yandex.market.abo.clch.checker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.clch.ClchTest;

/**
 * @author Ivan Melnikov imelnikov@yandex-team.ru
 */
public class OwnRegionCheckerTest extends ClchTest {

    private static final int SHOP_SECOND = 5570;
    private static final int SHOP_FIRST = 3528;

    @Autowired
    private OwnRegionChecker ownRegionChecker;

    @Test
    public void test() {
        final CheckerDescriptor checkerDescriptor = new CheckerDescriptor(0, "testChecker");
        ownRegionChecker.configure(checkerDescriptor);

        System.out.println(ownRegionChecker.checkShops(SHOP_FIRST, SHOP_SECOND));
    }

}
