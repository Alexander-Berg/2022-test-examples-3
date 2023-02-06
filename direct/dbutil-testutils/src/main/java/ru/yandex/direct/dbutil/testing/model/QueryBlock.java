package ru.yandex.direct.dbutil.testing.model;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
public class QueryBlock {
    @JsonProperty("select_id")
    int selectId;
    @JsonProperty("cost_info")
    CostInfo costInfo;
    @JsonProperty("table")
    Table table;
    @JsonProperty("nested_loop")
    List<QueryBlock> nestedLoop;
    @JsonProperty("message")
    String message;

    public int getSelectId() {
        return selectId;
    }

    public CostInfo getCostInfo() {
        return costInfo;
    }

    public Table getTable() {
        return table;
    }

    public List<QueryBlock> getNestedLoop() {
        return nestedLoop;
    }

    public String getMessage() {
        return message;
    }
}
