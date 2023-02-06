package ru.yandex.market.abo.clch.regular.loaders;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 */
class DbShopForRegularClchLoadersTest extends EmptyTest {

    @Autowired
    private List<ShopForRegularClchLoader> dbLoaders;

    @Test
    void checkQueryFormatTest() {
        dbLoaders.forEach(ShopForRegularClchLoader::loadShopsForClch);
    }
}
