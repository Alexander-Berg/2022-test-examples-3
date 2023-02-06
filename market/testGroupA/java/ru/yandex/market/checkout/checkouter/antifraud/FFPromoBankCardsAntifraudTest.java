package ru.yandex.market.checkout.checkouter.antifraud;

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class FFPromoBankCardsAntifraudTest extends AbstractFFPromoAntifraudTestBase {

    private static final String CARD_NUMBER = "51234567****1234";
    private static final String OTHER_CARD_NUMBER = "51857473****4532";

    public static final int EXPECTED_ORDER_LIMIT = 5;

    @Test
    public void test() {
        jumpToPast(25, ChronoUnit.HOURS);
        createAndPayOrder(CARD_NUMBER); // Out of scope: created more than 24 hours ago
        clearFixed();

        createAndPayOrder(CARD_NUMBER, false); // Out of scope: cancelled payment

        createOrder(true, false, true, newRandomBuyer());
        payForOrder(CARD_NUMBER, true, false); // Out of scope: not FF

        createOrder(true, true, false, newRandomBuyer());
        payForOrder(CARD_NUMBER, true, false); // Out of scope: not promo

        createAndPayOrder(OTHER_CARD_NUMBER); // Out of scope: different card number

        for (int i = 0; i < EXPECTED_ORDER_LIMIT; i++) {
            createAndPayOrder(CARD_NUMBER);
        }
        createAndPayOrder(CARD_NUMBER, true, true); // Exceeds limit of 7
    }

}
