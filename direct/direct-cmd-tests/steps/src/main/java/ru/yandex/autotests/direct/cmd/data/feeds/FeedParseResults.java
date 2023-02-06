package ru.yandex.autotests.direct.cmd.data.feeds;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FeedParseResults {

    @SerializedName("warnings")
    private List<String> warnings;

    @SerializedName("errors")
    private List<FeedError> errors;

    public List<String> getWarnings() {
        return warnings;
    }

    public FeedParseResults withWarnings(List<String> warnings) {
        this.warnings = warnings;
        return this;
    }

    public List<FeedError> getErrors() {
        return errors;
    }

    public FeedParseResults withErrors(List<FeedError> errors) {
        this.errors = errors;
        return this;
    }
}
