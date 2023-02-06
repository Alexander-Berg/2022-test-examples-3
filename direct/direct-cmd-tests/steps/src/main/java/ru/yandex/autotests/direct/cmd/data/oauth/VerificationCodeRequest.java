package ru.yandex.autotests.direct.cmd.data.oauth;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class VerificationCodeRequest extends BasicDirectRequest {

    @SerializeKey("dev")
    private String dev = "true";

    public String getDev() {
        return dev;
    }

    public VerificationCodeRequest withDev(String dev) {
        this.dev = dev;
        return this;
    }
}
