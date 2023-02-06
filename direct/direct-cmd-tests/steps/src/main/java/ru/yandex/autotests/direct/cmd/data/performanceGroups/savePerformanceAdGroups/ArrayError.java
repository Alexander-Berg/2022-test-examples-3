package ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups;

import com.google.gson.annotations.SerializedName;

public class ArrayError {

    @SerializedName("object_errors")
    private ObjectErrors objectErrors;

    public ObjectErrors getObjectErrors() {
        return objectErrors;
    }

    public void setObjectErrors(ObjectErrors objectErrors) {
        this.objectErrors = objectErrors;
    }
}
