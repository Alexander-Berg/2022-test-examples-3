package ru.yandex.autotests.direct.cmd.data.images;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorData;

import java.util.List;

import static java.util.Collections.singletonList;

public class Error {

    @SerializedName("array_errors")
    private List<List<ErrorData>> errors;

    public List<List<ErrorData>> getErrors() {
        return errors;
    }

    public Error withErrors(List<List<ErrorData>> errors) {
        this.errors = errors;
        return this;
    }

    public Error withErrorText(String text) {
        return new Error().withErrors(singletonList(
                singletonList(new ErrorData().withText(text))));
    }
}
