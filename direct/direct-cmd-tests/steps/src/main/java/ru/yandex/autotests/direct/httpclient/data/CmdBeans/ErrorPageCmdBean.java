package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 07.04.15.
 * TESTIRT-5051
 */
public class ErrorPageCmdBean {

    @JsonPath(responsePath = "msg")
    private String msg;

    @JsonPath(responsePath = "hash/return_to/href")
    private String href;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
