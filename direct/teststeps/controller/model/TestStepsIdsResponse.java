package ru.yandex.direct.teststeps.controller.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

public class TestStepsIdsResponse extends TestStepsSuccessResponse {
    @JsonProperty("ids")
    private final List<Long> ids;

    public TestStepsIdsResponse(List<Long> ids) {
        this.ids = ids;
    }

    @ApiModelProperty(required = true)
    public List<Long> getIds() {
        return ids;
    }
}
