package ru.yandex.direct.teststeps.controller.model;

import io.swagger.annotations.ApiModelProperty;

import ru.yandex.direct.web.core.model.WebResponse;

public class TestStepsSuccessResponse implements WebResponse {
    @Override
    @ApiModelProperty(required = true)
    public boolean isSuccessful() {
        return true;
    }
}
