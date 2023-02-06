package ru.yandex.autotests.direct.httpclient.core;

public class BasicDirectRequestParametersBuilder {
    private String ulogin;
    private String retPath;
    private String getVars;
    private String format;

    public BasicDirectRequestParametersBuilder setUlogin(String ulogin) {
        this.ulogin = ulogin;
        return this;
    }

    public BasicDirectRequestParametersBuilder setRetPath(String retPath) {
        this.retPath = retPath;
        return this;
    }

    public BasicDirectRequestParametersBuilder setGetVars(String getVars) {
        this.getVars = getVars;
        return this;
    }

    public BasicDirectRequestParametersBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public BasicDirectRequestParameters createBasicDirectRequestParameters() {
        return new BasicDirectRequestParameters(ulogin, retPath, getVars, format);
    }
}