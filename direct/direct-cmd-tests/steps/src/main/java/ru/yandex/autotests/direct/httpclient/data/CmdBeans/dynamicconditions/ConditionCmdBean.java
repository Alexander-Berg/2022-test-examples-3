package ru.yandex.autotests.direct.httpclient.data.CmdBeans.dynamicconditions;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;


public class ConditionCmdBean {

    @JsonPath(requestPath = "kind", responsePath = "kind")
    private String kind;

    @JsonPath(requestPath = "type", responsePath = "type")
    private String type;

    @JsonPath(requestPath = "value", responsePath = "value")
    private String[] value;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String[]  getValue() {
        return value;
    }

    public void setValue(String[]  value) {
        this.value = value;
    }
}