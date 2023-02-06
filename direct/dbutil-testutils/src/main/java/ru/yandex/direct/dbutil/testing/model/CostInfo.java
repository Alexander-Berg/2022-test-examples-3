package ru.yandex.direct.dbutil.testing.model;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
public class CostInfo {
    @JsonProperty("query_cost")
    Double queryCost;
}
