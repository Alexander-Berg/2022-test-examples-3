package ru.yandex.direct.teststeps.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

public class TestStepsIdResponse extends TestStepsSuccessResponse {
    @JsonProperty("id")
    private final long id;

    public TestStepsIdResponse(long id) {
        this.id = id;
    }

    @ApiModelProperty(required = true)
    public long getId() {
        return id;
    }

}
