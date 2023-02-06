package ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters;

import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;


public class AjaxEditPerformanceFiltersResponse extends ErrorResponse {

    private String errors;

    private String result;

    public String getErrors() {
        return errors;
    }

    public void setErrors(String errors) {
        this.errors = errors;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
