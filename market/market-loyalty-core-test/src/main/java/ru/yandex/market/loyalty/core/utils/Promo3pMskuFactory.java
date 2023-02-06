package ru.yandex.market.loyalty.core.utils;

import ru.yandex.market.loyalty.core.model.promo3p.Promo3pMsku;

import java.math.BigDecimal;

public abstract class Promo3pMskuFactory {
    public static final String FIRST_MSKU = "1";
    public static final BigDecimal FIRST_PRICE = BigDecimal.TEN;
    public static final BigDecimal FIRST_OLD_PRICE = BigDecimal.valueOf(25);

    public static final String SECOND_MSKU = "2";
    public static final BigDecimal SECOND_PRICE = BigDecimal.valueOf(20);
    public static final BigDecimal SECOND_OLD_PRICE = BigDecimal.valueOf(25);

    public static final String THIRD_MSKU = "3";
    private static final BigDecimal THIRD_PRICE = BigDecimal.valueOf(30);
    private static final BigDecimal THIRD_OLD_PRICE = BigDecimal.valueOf(35);

    private static final String FOURTH_MSKU = "4";
    private static final BigDecimal FOURTH_PRICE = BigDecimal.valueOf(45);
    private static final BigDecimal FOURTH_OLD_PRICE = BigDecimal.valueOf(60);


    public static final String INVALID_MSKU = "invalidMsku";

    private Promo3pMskuFactory() {
    }


    public static Promo3pMsku.Builder firstMskuBuilder() {
        return Promo3pMsku.builder()
                .setMsku(FIRST_MSKU)
                .setPrice(FIRST_PRICE)
                .setOldPrice(FIRST_OLD_PRICE);
    }

    public static Promo3pMsku.Builder secondMskuBuilder() {
        return Promo3pMsku.builder()
                .setMsku(SECOND_MSKU)
                .setPrice(SECOND_PRICE)
                .setOldPrice(SECOND_OLD_PRICE);
    }

    public static Promo3pMsku.Builder thirdMskuBuilder() {
        return Promo3pMsku.builder()
                .setMsku(THIRD_MSKU)
                .setPrice(THIRD_PRICE)
                .setOldPrice(THIRD_OLD_PRICE);
    }

    public static Promo3pMsku.Builder fourthMskuBuilder() {
        return Promo3pMsku.builder()
                .setMsku(FOURTH_MSKU)
                .setPrice(FOURTH_PRICE)
                .setOldPrice(FOURTH_OLD_PRICE);
    }

    public static Promo3pMsku.Builder invalidMskuBuilder() {
        return Promo3pMsku.builder()
                .setMsku(INVALID_MSKU)
                .setPrice(FIRST_PRICE)
                .setOldPrice(FIRST_OLD_PRICE);
    }
}
