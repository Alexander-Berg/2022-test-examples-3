package ru.yandex.autotests.direct.cmd.data;

import org.apache.commons.lang.SerializationUtils;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.KeyValueBean;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;

import java.io.Serializable;

@KeyValueBean
public class BasicDirectRequest implements Serializable {

    @SerializeKey("ulogin")
    private String ulogin;

    @SerializeKey("rulogin")
    private String rulogin;

    @SerializeKey("retpath")
    private String retPath;

    @SerializeKey("rcmd")
    private String rcmd;

    @SerializeKey("format")
    private String format;      //для отображения в формате json (format="json") для некоторых не-BEM страниц

    public BasicDirectRequest() {}

    public String getUlogin() {
        return ulogin;
    }
    public void setUlogin(String ulogin) {
        this.ulogin = ulogin;
    }

    public String getRulogin() {
        return rulogin;
    }

    public void setRulogin(String rulogin) {
        this.rulogin = rulogin;
    }

    public String getRetPath() {
        return retPath;
    }

    public void setRetPath(String retPath) {
        this.retPath = retPath;
    }

    public String getRcmd() {
        return rcmd;
    }

    public void setRcmd(String rcmd) {
        this.rcmd = rcmd;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @SuppressWarnings("unchecked")
    public <T extends BasicDirectRequest> T withUlogin(String ulogin) {
        this.ulogin = ulogin;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends BasicDirectRequest> T  withRulogin(String rulogin) {
        this.rulogin = rulogin;
        return  (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends BasicDirectRequest> T  withRetPath(String retPath) {
        this.retPath = retPath;
        return  (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends BasicDirectRequest> T  withRcmd(String rcmd) {
        this.rcmd = rcmd;
        return  (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends BasicDirectRequest> T  withFormat(String format) {
        this.format = format;
        return  (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends BasicDirectRequest> T deepCopy() {
        return (T) SerializationUtils.clone(this);
    }

    @Override
    public String toString() {
        return JsonUtils.toString(this);
    }
}
