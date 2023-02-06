package ru.yandex.market.loyalty.core.utils;

import ru.yandex.market.loyalty.api.model.CashbackOptions;
import ru.yandex.market.loyalty.api.model.CashbackPermision;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class CashbackUtils {
    public static void validateAmount(CashbackOptions cashback, BigDecimal expectedAmount) {
        assertThat(cashback.getAmount(), comparesEqualTo(expectedAmount));
        if (cashback.getType() == CashbackPermision.ALLOWED && !cashback.getAmountByPromoKey().isEmpty()) {
            assertThat(
                    cashback.getAmountByPromoKey()
                            .values()
                            .stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add),
                    comparesEqualTo(expectedAmount)
            );
        }
    }
}
