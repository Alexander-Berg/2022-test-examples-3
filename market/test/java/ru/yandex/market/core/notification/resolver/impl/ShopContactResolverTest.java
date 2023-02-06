package ru.yandex.market.core.notification.resolver.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.notification.context.impl.ShopNotificationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link ShopContactResolver}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "ShopContactResolverTest.before.csv")
public class ShopContactResolverTest extends FunctionalTest {
    @Autowired
    private ShopContactResolver shopContactResolver;

    @Test
    void testShopAdmins() {
        assertThat(shopContactResolver.resolveAddresses("ShopAdmins", new ShopNotificationContext(1)))
                .containsExactlyInAnyOrder("admin4eg@yandex.ru", "business@yandex.ru");
    }

    @Test
    void testShopTechnical() {
        assertThat(shopContactResolver.resolveAddresses("ShopSupports", new ShopNotificationContext(1)))
                .containsExactlyInAnyOrder("admin4eg@yandex.ru");
    }
}
