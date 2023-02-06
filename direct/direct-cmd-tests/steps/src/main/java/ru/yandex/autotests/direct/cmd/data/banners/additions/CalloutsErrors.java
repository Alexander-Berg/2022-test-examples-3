package ru.yandex.autotests.direct.cmd.data.banners.additions;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorData;

import java.util.ArrayList;
import java.util.List;

/*
* todo javadoc
*/
public class CalloutsErrors {

    @SerializedName("array_errors")
    private List<List<ErrorData>> calloutsErrors;

    @SerializedName("generic_errors")
    private List<ErrorData> genericErrors = new ArrayList<>();

    public List<List<ErrorData>> getCalloutsErrors() {
        return calloutsErrors;
    }

    public CalloutsErrors withCalloutsErrors(List<List<ErrorData>> calloutsErrors) {
        this.calloutsErrors = calloutsErrors;
        return this;
    }

    public List<ErrorData> getGenericErrors() {
        return genericErrors;
    }

    public CalloutsErrors withGenericErrors(List<ErrorData> genericErrors) {
        this.genericErrors = genericErrors;
        return this;
    }
}
