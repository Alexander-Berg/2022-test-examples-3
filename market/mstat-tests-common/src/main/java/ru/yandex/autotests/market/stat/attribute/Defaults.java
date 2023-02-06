package ru.yandex.autotests.market.stat.attribute;

/**
 * Created by kateleb on 21.05.15.
 */
public enum Defaults {
    TOUCH(false),
    SBID(0),
    BID(31),
    HOME_REGION("213"),
    GEO_ID(213),
    PRICE(25),
    OLDPRICE(25),
    OFFER_PRICE(77),
    TEST_TAG("9"),
    HOSTNAME("msh42e"),
    HOST("msh42e.yandex.market"),
    VHOST("market.yandex.ru"),
    SHOW_GEN("20140425_0202"),
    NAV_CAT_ID(1),
    MN_CTR("1"),
    TEST_REQ("test_REQ-Id"),
    TEST_BUCKETS("1007,2,76;1096,45,12"),
    CPA(false),
    WPRID("test_WEB_Parent-Iq"),
    GOODS_TITLE("аааааа-айфон"),
    POF(114277),
    HYPER_CAT_ID(90490),
    QUERY("Тестовый запрос !№%"),
    CONTEXT("Тестовый контекст +_)("),
    CLICK_PHONE_RATIO(0.917),
    ZERO("0"),
    EMPTY(""),
    NONE("-1"),
    STATUS("200"),
    USER_AGENT("\"Mozilla/5.0 (Windows NT 5.0; rv:12.0) Gecko/20100101 Firefox/12.0\""),
    RATING("4"),
    MANAGER_ID("-2"),
    DELIVERY_STATUS("DELIVERY"),
    DELIVERED_STATUS("DELIVERED"),
    BID_TYPE("cbid"),
    CLIENT_ID("market"),
    BRAND_ID(11),
    VENDOR_DS_ID(12),
    VENDOR_PRICE(13),
    VC_BID(14),
    TEST_STATUS("TEST_STATUS"),
    MSTAT_TESTER("MSTAT_TESTER"),
    POF_RAW("{\"clid\":[\"505\"],\"mclid\":null,\"distr_type\":null}"),
    POF_RAW_OPP("{\"clid\":[\"505\"],\"mclid\":null,\"distr_type\":\"1\", \"opp\": \"999\"}");

    private Object mask;

    Defaults(Object mask) {
        this.mask = mask;
    }

    public Object mask() {
        return mask;
    }
}
