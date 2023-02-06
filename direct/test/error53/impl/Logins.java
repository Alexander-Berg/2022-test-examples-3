package ru.yandex.autotests.directapi.test.error53.impl;

public final class Logins {
    public static final String UNKNOWN_LOGIN = "$client$";

    public static final String SUPER = ru.yandex.autotests.directapi.model.Logins.SUPER_LOGIN;

    public static final String CLIENT = "at-error53-client";
    public static final String CLIENT_REP = "at-error53-client-rep";
    public static final String CLIENT_WITH_EXPIRED_TOKEN = "at-error53-client-exp-token";
    public static final String ANOTHER_CLIENT = CLIENT_WITH_EXPIRED_TOKEN;

    public static final String AGENCY = "at-error53-agency";
    public static final String AGENCY_CLIENT = "at-error53-ag-client2";
    public static final String AGENCY_CLIENT_WITH_STATUS_BLOCKED = "at-error53-ag-client-blocked";
    public static final String ANOTHER_AGENCY = "at-error53-another-agency";
    public static final String ANOTHER_AGENCY_CLIENT = "at-error53-ag-client";

    public static final String CLIENT_WITH_STATUS_BLOCKED = "at-error53-status-blocked";
    public static final String CLIENT_WITH_BLOCKED_API_ACCESS = "at-error53-api-access-blocked";
    public static final String CLIENT_WITH_NOT_ALLOWED_IP = "at-error53-invalid-ip";
    public static final String YA_AGENCY_CLIENT = "at-error53-ya-agency-client";
    public static final String CLIENT_WITH_CONVERTING_CURRENCY = "at-error53-conv-currency";
}
