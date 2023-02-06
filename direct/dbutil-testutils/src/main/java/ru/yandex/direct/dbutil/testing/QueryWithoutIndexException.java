package ru.yandex.direct.dbutil.testing;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.Query;

/**
 * Исключение, сигнализирующее что код потенциально не очень хорошо написан.
 */
@ParametersAreNonnullByDefault
class QueryWithoutIndexException extends RuntimeException {
    private static final String ERROR_STRING_TEMPLATE = "Execution has heavy query\n: %s\n" +
            "Execution plan\n: %s\n" +
            "Please fix the code, the DB, or use QueryWithoutIndex annotation";

    QueryWithoutIndexException(Query query, String executionPlan) {
        super(String.format(ERROR_STRING_TEMPLATE, query.getSQL(), executionPlan));
    }

    QueryWithoutIndexException(Query query) {
        super(String.format("Execution has heavy query:\n: %s", query.getSQL()));
    }
}
