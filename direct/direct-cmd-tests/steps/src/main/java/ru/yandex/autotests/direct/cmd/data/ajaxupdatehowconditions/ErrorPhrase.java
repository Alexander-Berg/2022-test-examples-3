package ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class ErrorPhrase {

    @SerializedName("phrase")
    private String phrase;

    @SerializedName("errors")
    private List<String> errors;

    public String getPhrase() {
        return phrase;
    }

    public ErrorPhrase withPhrase(String phrase) {
        this.phrase = phrase;
        return this;
    }

    public List<String> getErrors() {
        return errors;
    }

    public ErrorPhrase withErrors(List<String> errors) {
        this.errors = errors;
        return this;
    }
}
