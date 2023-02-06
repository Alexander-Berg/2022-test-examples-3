package ru.yandex.market.antifraud.filter.fields;

/**
 * Created by kateleb on 21.05.15.
 */
public enum Defaults {
    TOUCH(false),
    NATIONAL("ru"),
    SBID(0),
    BID(31),
    HOME_REGION(213),
    GEO_ID(213),
    PASSPORTUID("86134981"),
    OLDPRICE("25"),
    OFFER_PRICE("77.7"),
    TEST_TAG("9"),
    HOSTNAME("msh42e"),
    HOST("msh42e.yandex.market"),
    VHOST("market.yandex.ru"),
    SHOW_GEN("20140425_0202"),
    SOURCE_ID("51502"),
    POSITION("1"),
    NAV_CAT_ID(1),
    MN_CTR("1"),
    TEST_REQ("test_REQ-Id"),
    TEST_BUCKETS("1007,2,76;1096,45,12"),
    WPRID("test_WEB_Parent-Iq"),
    GOODS_TITLE("аааааа-айфон"),
    POF(114277),
    HYPER_CAT_ID(90490),
    QUERY("Тестовый запрос !№%"),
    CONTEXT("Тестовый контекст +_)("),
    CLICK_PHONE_RATIO("0.917"),
    NONE("-1"),
    STATUS("200"),
    IS_ENABLED("1"),
    USER_AGENT("\"Mozilla/5.0 (Windows NT 5.0; rv:12.0) Gecko/20100101 Firefox/12.0\""),
    RATING("4"),
    MANAGER_ID("-2"),
    DELIVERY_STATUS("DELIVERY"),
    DELIVERED_STATUS("DELIVERED"),
    BID_TYPE("cbid"),
    CLIENT_ID("market"),
    BRAND_ID("11"),
    VENDOR_DS_ID("12"),
    VENDOR_PRICE("13"),
    VC_BID("14"),
    TEST_STATUS("TEST_STATUS"),
    MSTAT_TESTER("MSTAT_TESTER"),
    NOT_ON_STOCK("0"),
    ON_STOCK("1"),
    AUTOBROKER_ENABLED_TRUE("1"),
    AUTOBROKER_ENABLED_FALSE("0"),
    AUTOBROKER_ENABLED_NONE(""),
    AUTOBROKER_ENABLED_ABSENT(null),
    USER_TYPE_EXTERNAL("0"),
    USER_TYPE_YANDEX("1")
    ;

    private Object value;

    Defaults(Object value) {
        this.value = value;
    }

    public Object value() {
        return value;
    }

    public <T> T value(Class<T> clazz) {
        return (T) value;
    }
}
