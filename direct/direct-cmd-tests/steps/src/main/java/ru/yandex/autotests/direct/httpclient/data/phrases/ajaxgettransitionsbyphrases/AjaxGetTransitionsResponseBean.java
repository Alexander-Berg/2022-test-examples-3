package ru.yandex.autotests.direct.httpclient.data.phrases.ajaxgettransitionsbyphrases;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by shmykov on 09.06.15.
 */
public class AjaxGetTransitionsResponseBean {

    @JsonPath(responsePath = "phrase")
    private String phrases;

    @JsonPath(responsePath = "transitions")
    private List<Transition> transitions;

    public String getPhrases() {
        return phrases;
    }

    public void setPhrases(String phrases) {
        this.phrases = phrases;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<Transition> transitions) {
        this.transitions = transitions;
    }
}