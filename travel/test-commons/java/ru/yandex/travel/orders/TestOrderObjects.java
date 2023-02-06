package ru.yandex.travel.orders;

import lombok.experimental.UtilityClass;

import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.entities.FiscalItemType;
import ru.yandex.travel.orders.entities.MoneyMarkup;

import static ru.yandex.travel.testing.misc.TestBaseObjects.rub;

/**
 * Hope-to-be a central place for your test order building blocks.
 * Introduce more sophisticated methods and/or builders for test instances when needed.
 */
@UtilityClass
public class TestOrderObjects {
    public static FiscalItem fiscalItem(long id, Number totalRub, Number plusPoints, FiscalItemType type) {
        return FiscalItem.builder()
                .id(id)
                .type(type)
                .moneyAmount(rub(totalRub))
                .yandexPlusToWithdraw(rub(plusPoints))
                .build();
    }

    /**
     * The helper isn't defined in MoneyMarkup intentionally.
     * It's to have more explicit and clear markup definitions in the main code.
     * While the shortcuts are ok for tests.
     */
    public static MoneyMarkup moneyMarkup(Number card, Number yandexAccount) {
        return MoneyMarkup.builder()
                .card(rub(card))
                .yandexAccount(rub(yandexAccount))
                .build();
    }
}
