package ru.yandex.direct.dbutil.testing.model;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonProperty;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@ParametersAreNonnullByDefault
public class ExplainResult {
    @JsonProperty("query_block")
    private QueryBlock queryBlock;

    public List<Table> getTables() {
        if (queryBlock.table != null) {
            return singletonList(queryBlock.getTable());
        }
        if (queryBlock.nestedLoop != null) {
            return queryBlock.nestedLoop.stream().map(QueryBlock::getTable).collect(toList());
        }
        return emptyList();
    }
}
