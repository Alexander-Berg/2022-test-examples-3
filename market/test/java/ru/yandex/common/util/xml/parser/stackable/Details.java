package ru.yandex.common.util.xml.parser.stackable;

import java.util.Map;

public class Details {
    public Map<String, String> params;

    @Override
    public String toString() {
        return "Details{" +
            "params=" + params +
            "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof Details)) return false;

        Details details = (Details) o;

        if(params != null ? !params.equals(details.params) : details.params != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return params != null ? params.hashCode() : 0;
    }
}
