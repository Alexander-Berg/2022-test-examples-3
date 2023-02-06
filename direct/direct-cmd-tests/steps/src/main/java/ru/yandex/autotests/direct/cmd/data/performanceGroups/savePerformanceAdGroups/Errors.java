package ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorData;

import java.util.List;

public class Errors {



    @SerializedName("generic_errors")
    private List<ErrorData> genericErrors;

    @SerializedName("array_errors")
    private List<ErrorData> arrayErrors;



    public List<ErrorData> getGenericErrors() {
        return genericErrors;
    }

    public void setGenericErrors(List<ErrorData> genericErrors) {
        this.genericErrors = genericErrors;
    }

    public List<ErrorData> getArrayErrors() {
        return arrayErrors;
    }

    public void setArrayErrors(List<ErrorData> arrayErrors) {
        this.arrayErrors = arrayErrors;
    }
}
