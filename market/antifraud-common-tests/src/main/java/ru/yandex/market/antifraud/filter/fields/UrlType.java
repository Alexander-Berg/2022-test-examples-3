package ru.yandex.market.antifraud.filter.fields;


import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by entarrion on 05.10.16.
 */
public enum UrlType {
    U_EXTERNAL_URL(0),        // encrypted clickdaemon url
    U_SIGNED_URL(1),          // url for partner-api, deprecated
    U_GEO_OUTLET_URL(2),      // url to single outlet with offer
    U_GEO_OUTLET_INFO_URL(3), // url to popup outlet in map
    U_GEO_URL(4),             // in old logic: complex, in new logic (prime+): url to offer in map
    U_PHONE_URL(5),           // in old logic: call or show phone, in new logic: just call
    U_CPA_URL(6),             // url of Buy button
    U_SHOW_PHONE_URL(7),      // url to show phone
    U_OFFERCARD_URL(8),       // url to offer card
    U_DIRECT_URL(9),          // in new logic only: just offer url
    U_PLAIN_URL(10),          // in new logic only: decrypted clickdaemon url
    U_BOOK_NOW_INCUT_URL(11); // url for book now

    private int value;

    UrlType(int value) {
        this.value = value;
    }

    public static int getRandomValue() {
        return values()[ThreadLocalRandom.current().nextInt(values().length)].value;
    }

    public int getValue() {
        return value;
    }
}
