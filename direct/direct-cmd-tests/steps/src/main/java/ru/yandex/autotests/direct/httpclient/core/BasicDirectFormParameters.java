package ru.yandex.autotests.direct.httpclient.core;

import ru.yandex.autotests.httpclient.lite.core.*;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 * @Deprecated use BasicDirectRequestParameters with @JsonPath(requestPath = "annotationValue") annotation instead
 */

@Deprecated
public class BasicDirectFormParameters extends AbstractFormParameters implements IFormParameters {
    public static final BasicDirectFormParameters EMPTY = new BasicDirectFormParameters();

    public BasicDirectFormParameters() {
        super();
        ignoreEmptyParameters(false);
    }

    @FormParameter("ulogin")
    private String ulogin;
    @FormParameter("retpath")
    private String retPath;
    @FormParameter("get_vars")
    private String getVars;

    //для отображения в формате json (format="json") для некоторых не-BEM страниц
    @FormParameter("format")
    private String format;

    public String getUlogin() {
        return ulogin;
    }

    public void setUlogin(String ulogin) {
        this.ulogin = ulogin;
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

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
