package ru.yandex.autotests.direct.httpclient.core;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.requestParameters.AbstractRequestParameters;

/**
 * @author alex-samo (alex-samo@yandex-team.ru)
 */
public class BasicDirectRequestParameters extends AbstractRequestParameters {
    public static final BasicDirectRequestParameters EMPTY = new BasicDirectRequestParametersBuilder().createBasicDirectRequestParameters();

    public BasicDirectRequestParameters() {
        super();
        ignoreEmptyParameters(true);
    }

    @JsonPath(requestPath = "ulogin")
    private String ulogin;
    @JsonPath(requestPath = "rulogin")
    private String rulogin;
    @JsonPath(requestPath = "retpath")
    private String retPath;
    @JsonPath(requestPath = "rcmd")
    private String rcmd;
    @JsonPath(requestPath = "get_vars")
    private String getVars;

    //для отображения в формате json (format="json") для некоторых не-BEM страниц
    @JsonPath(requestPath = "format")
    private String format;

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

    public String getGetVars() {
        return getVars;
    }

    public void setGetVars(String getVars) {
        this.getVars = getVars;
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

    public BasicDirectRequestParameters(String ulogin, String retPath, String getVars, String format) {
        super();
        this.ulogin = ulogin;
        this.retPath = retPath;
        this.getVars = getVars;
        this.format = format;
        ignoreEmptyParameters(false);
    }
}
