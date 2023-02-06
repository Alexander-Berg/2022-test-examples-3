package ru.yandex.direct.dbutil.testing.model;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
public class Table {
    @JsonProperty("table_name")
    String tableName;
    @JsonProperty("access_type")
    String accessType;
    @JsonProperty("possible_keys")
    List<String> possibleKeys;
    @JsonProperty("key")
    String key;
    @JsonProperty("filtered")
    Double filtered;
    @JsonProperty("cost_info")
    CostInfo costInfo;

    @Nullable
    public String getAccessType() {
        return accessType;
    }

    public List<String> getPossibleKeys() {
        return possibleKeys;
    }

    public String getTableName() {
        return tableName;
    }
}
