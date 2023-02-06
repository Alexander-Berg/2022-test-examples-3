package ru.yandex.market.antifraud.orders.entity.roles;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerIdType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dimkarp93
 */
public class BuyerIdTypeTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void findTypeSuccess() {
        assertThat(BuyerIdType.fromMarketUserIdType(MarketUserId.YANDEX_UID_STR)).isEqualTo(BuyerIdType.YANDEXUID);
    }

    @Test
    public void findTypeFailed() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Unknown type abcd");

        BuyerIdType.fromMarketUserIdType("abcd");
    }
}
