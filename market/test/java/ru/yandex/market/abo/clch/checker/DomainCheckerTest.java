package ru.yandex.market.abo.clch.checker;

import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.abo.clch.model.DataWithSource;
import ru.yandex.market.abo.clch.model.ShopDataSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainCheckerTest {

    @Test
    void compareData() {
        assertTrue(0 < new DomainChecker().compareData(
                Set.of(new DataWithSource<>("www.shop.ru", ShopDataSource.PI)),
                Set.of(new DataWithSource<>("shop.ru", ShopDataSource.SPARK))
        ));
    }
}