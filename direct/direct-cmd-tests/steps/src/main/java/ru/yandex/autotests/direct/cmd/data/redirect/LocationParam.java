package ru.yandex.autotests.direct.cmd.data.redirect;

public enum LocationParam {

    CID("cid"),
    ULOGIN("ulogin"),
    CMD("cmd"),
    ERROR_CODE("error_code"),
    ERROR("error"),
    ACCESS_TOKEN("access_token"),
    RESULT_NEW_CID("result_new_cid"),
    PURCHASE_TOKEN("purchase_token"),

    CAMPAIGNS_IDS("campaigns-ids");

    private LocationParam(String name) {
        this.name = name;
    }

    private String name;

    @Override
    public String toString() {
        return name;
    }
}
