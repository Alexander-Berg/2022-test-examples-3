package ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;

public class GroupErrorsResponse extends ErrorResponse {
    @SerializedName("errors")
    private ObjectErrors errors;

    public ObjectErrors getErrors() {
        return errors;
    }

    public void setErrors(ObjectErrors errors) {
        this.errors = errors;
    }
}
