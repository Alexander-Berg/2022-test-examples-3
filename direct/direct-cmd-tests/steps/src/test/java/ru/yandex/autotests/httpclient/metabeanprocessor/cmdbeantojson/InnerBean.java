package ru.yandex.autotests.httpclient.metabeanprocessor.cmdbeantojson;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 16.04.15.
 */
public class InnerBean {

    @JsonPath(requestPath = "inner/property")
    private Boolean innerProperty;

    public Boolean isInnerProperty() {
        return innerProperty;
    }

    public void setInnerProperty(Boolean innerProperty) {
        this.innerProperty = innerProperty;
    }
}
