package ru.yandex.autotests.market.stat.requests;

/**
 * Created by entarrion on 22.11.16.
 */
public class RequestParam {
    private String key;
    private String value;
    private boolean isUrlParam;

    public RequestParam(String key, String value, boolean isUrlParam) {
        setKey(key);
        setValue(value);
        setUrlParam(isUrlParam);
    }

    public RequestParam(RequestParam other) {
        setKey(other.getKey());
        setValue(other.getValue());
        setUrlParam(other.isUrlParam());
    }

    public RequestParam(String key, String value) {
        this(key, value, true);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isUrlParam() {
        return isUrlParam;
    }

    public void setUrlParam(boolean urlParam) {
        isUrlParam = urlParam;
    }

    @Override
    public String toString() {
        return "RequestParam[key: " + getKey() + "; value: " + getValue() + "]";
    }
}