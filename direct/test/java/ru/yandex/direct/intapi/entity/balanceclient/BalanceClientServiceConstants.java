package ru.yandex.direct.intapi.entity.balanceclient;

public class BalanceClientServiceConstants {
    public static final int CRITICAL_ERROR = -32603;

    public static final String BALANCE_CLIENT_PREFIX = "/BalanceClient";
    public static final String NOTIFY_CLIENT2_PREFIX = BALANCE_CLIENT_PREFIX + "/NotifyClient2";
    public static final String NOTIFY_PROMOCODE_PREFIX = BALANCE_CLIENT_PREFIX + "/NotifyPromocode";
    public static final String GET_HOSTINGS_PREFIX = BALANCE_CLIENT_PREFIX + "/GetHostings";
    public static final String PING_PREFIX = BALANCE_CLIENT_PREFIX + "/Ping";
    public static final String NOTIFY_CLIENT_CASHBACK_PREFIX = BALANCE_CLIENT_PREFIX + "/NotifyClientCashback";

    public static final int DIRECT_SERVICE_ID = 7;
    public static final int BAYAN_SERVICE_ID = 77;
    public static final int BANANA_SERVICE_ID = 67;
    public static final int YANDEX_AGENCY_SERVICE_ID = 177;

    private BalanceClientServiceConstants() {
    }
}
