package ru.yandex.autotests.direct.cmd.data.vcards;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SaveVCardResponse {

    @SerializedName("errors")
    private List<String> errors;

    public List<String> getErrors() {
        return errors;
    }

    public SaveVCardResponse withErrors(List<String> errors) {
        this.errors = errors;
        return this;
    }
}
