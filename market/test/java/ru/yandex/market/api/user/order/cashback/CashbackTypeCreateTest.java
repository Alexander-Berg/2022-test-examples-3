package ru.yandex.market.api.user.order.cashback;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;

import static org.junit.Assert.assertThat;

public class CashbackTypeCreateTest {

    @Test
    public void testCreateFromCheckouter() {
        Set<CashbackType> created = new HashSet<CashbackType>(3) {
            {
                add(CashbackType.fromCheckouter(CashbackOption.EMIT));
                add(CashbackType.fromCheckouter(CashbackOption.SPEND));
                add(CashbackType.fromCheckouter(CashbackOption.UNKNOWN));
                add(CashbackType.fromCheckouter(null));
            }
        };
        assertThat(created, Matchers.containsInAnyOrder(CashbackType.EMIT, CashbackType.SPEND, CashbackType.UNKNOWN, null));

    }

    @Test
    public void testCreateFromLoyalty() {
        Set<CashbackType> created = new HashSet<CashbackType>(2) {
            {
                add(CashbackType.fromLoyalty(ru.yandex.market.loyalty.api.model.CashbackType.EMIT));
                add(CashbackType.fromLoyalty(ru.yandex.market.loyalty.api.model.CashbackType.SPEND));
                add(CashbackType.fromLoyalty(null));
            }
        };
        assertThat(created, Matchers.containsInAnyOrder(CashbackType.EMIT, CashbackType.SPEND, null));
    }

    @Test
    public void testConvertToCheckouter() {
        Set<CashbackOption> created = new HashSet<CashbackOption>(3) {
            {
                add(CashbackType.toCheckouter(CashbackType.EMIT));
                add(CashbackType.toCheckouter(CashbackType.SPEND));
                add(CashbackType.toCheckouter(CashbackType.UNKNOWN));
            }
        };
        assertThat(created, Matchers.containsInAnyOrder(CashbackOption.EMIT, CashbackOption.SPEND, null));
    }
}
