package ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorData;

import java.util.List;

public class GroupErrors {

    @SerializedName("array_errors")
    private List<ArrayError> arrayErrors;

    @SerializedName("generic_errors")
    private List<ErrorData> genericErrors;

    public List<ArrayError> getArrayErrors() {
        return arrayErrors;
    }

    public void setArrayErrors(List<ArrayError> arrayErrors) {
        this.arrayErrors = arrayErrors;
    }

    public List<ErrorData> getGenericErrors() {
        return genericErrors;
    }

    public void setGenericErrors(List<ErrorData> genericErrors) {
        this.genericErrors = genericErrors;
    }
}
