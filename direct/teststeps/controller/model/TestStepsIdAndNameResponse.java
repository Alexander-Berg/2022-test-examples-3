package ru.yandex.direct.teststeps.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

public class TestStepsIdAndNameResponse extends TestStepsSuccessResponse {
    @JsonProperty("id")
    private final long id;

    @JsonProperty("name")
    private final String name;

    public TestStepsIdAndNameResponse(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @ApiModelProperty(required = true)
    public long getId() {
        return id;
    }

    @ApiModelProperty(required = true)
    public String getName() {
        return name;
    }
}
