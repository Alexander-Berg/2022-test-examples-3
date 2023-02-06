package ru.yandex.autotests.direct.cmd.data.strategy;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;

public class AjaxSaveAutobudgetResponse extends CommonResponse {

    @SerializedName("error")
    private List<String> errors;

    public List<String> getErrors() {
        return errors;
    }
}
