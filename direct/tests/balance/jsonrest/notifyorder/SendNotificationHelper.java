package ru.yandex.autotests.directintapi.tests.balance.jsonrest.notifyorder;

import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.money.MoneyFormat;

public abstract class SendNotificationHelper {
    static final DateTimeFormatter NOTIFICATION_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    static final String LOCALE = "ru";

    /**
     * Количество знаков после запятой для округления до минимальной суммы денег (копеек, центов и т.п.)
     * Подробнее тут: https://st.yandex-team.ru/DIRECT-73142
     */
    private static final int MONEY_CENT_SCALE = 2;

    static String getSumWithoutVatAsString(Money sum, Currency currency) {
        return String.format("%s %s (без НДС)",
                sum.subtractVAT().setScale(MONEY_CENT_SCALE, RoundingMode.DOWN)
                        .stringValue(MoneyFormat.TWO_DIGITS_POINT_SEPARATED),
                MoneyCurrency.get(currency).getAbbreviation(LOCALE));
    }
}
