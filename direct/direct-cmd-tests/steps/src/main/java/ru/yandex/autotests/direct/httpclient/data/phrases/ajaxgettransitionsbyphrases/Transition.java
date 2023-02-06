package ru.yandex.autotests.direct.httpclient.data.phrases.ajaxgettransitionsbyphrases;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 09.06.15.
 */
public class Transition {
    @JsonPath(responsePath = "cost")
    private String cost;

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }
}
