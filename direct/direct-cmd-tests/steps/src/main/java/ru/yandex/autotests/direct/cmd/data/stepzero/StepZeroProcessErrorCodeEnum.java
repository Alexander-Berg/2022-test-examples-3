package ru.yandex.autotests.direct.cmd.data.stepzero;

public enum StepZeroProcessErrorCodeEnum {

    CLIENT_OF_ANOTHER_AGENCY_CODE(1),
    NOT_A_CLIENT_ROLE_CODE(2),
    CLIENT_NOT_FOUND_CODE(3),
    NOT_YOUR_AGENCY_CODE(4),
    WRONG_CLIENT_CURRENCY_CODE(10),
    AWAITING_CLIENT_CONFIRMATION(11),
    NEED_CLIENT_CONFIRMATION(12);

    private Integer code;

    StepZeroProcessErrorCodeEnum(int code) {
        this.code = code;
    }

    public String toString() {
        return code.toString();
    }

}
