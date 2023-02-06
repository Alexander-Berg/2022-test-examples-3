package ru.yandex.direct.dbutil.testing;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.jooq.DSLContext;
import org.jooq.DeleteQuery;
import org.jooq.ExecuteContext;
import org.jooq.Query;
import org.jooq.SelectQuery;
import org.jooq.UpdateQuery;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultExecuteListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.testing.model.ExplainResult;
import ru.yandex.direct.utils.JsonUtils;

/**
 * JooqListener, который перед каждым запросом делает explain и смотрит, что выборка значений идет по ключу.
 * <p>
 * Проверку для конкретного метода можно отключить при помощи аннотации
 * {@link ru.yandex.direct.dbutil.QueryWithoutIndex}.
 */
@ParametersAreNonnullByDefault
public class JooqExplainExecuteListener extends DefaultExecuteListener {
    private static final Logger logger = LoggerFactory.getLogger(JooqExplainExecuteListener.class);
    private static final Map<String, Boolean> alreadySeenSqlIsOk = new ConcurrentHashMap<>();

    /**
     * Используется для выключения проверки, например чтоб в дебаггере гонять юниты.
     * Значение этого параметра НЕ проверяется, проверяется только его наличие.
     */
    private static final boolean disabled = System.getProperty("DISABLE_EXPLAIN") != null;

    private IgnoreAnnotationHelper ignoreAnnotationHelper =
            new IgnoreAnnotationHelper(QueryWithoutIndex.class, getClass());

    @Override
    public void start(ExecuteContext ctx) {
        if (disabled) {
            return;
        }
        Query query = ctx.query();
        if (query instanceof SelectQuery || query instanceof UpdateQuery || query instanceof DeleteQuery) {
            try {
                String querySql = query.getSQL();
                boolean queryIsOk = alreadySeenSqlIsOk.computeIfAbsent(querySql, unused -> {
                    boolean hasHeavyQuery = StreamEx.of(explainQuery(ctx, query).getTables())
                            .remove(table -> table.getTableName() == null)
                            .remove(table -> table.getTableName().matches("<derived\\d*>"))
                            .remove(table -> table.getTableName().matches("alias_\\d*"))
                            .anyMatch(table -> table.getPossibleKeys() == null);

                    return !hasHeavyQuery;
                });

                if (!queryIsOk && !ignoreAnnotationHelper.hasIgnoreAnnotation(true)) {
                    String prettyPrintPlan = ctx.dsl().explain(query).plan();
                    try {
                        ctx.connection().close();
                    } catch (SQLException e) {
                        // ignore, возможно коннект уже отвалился и/или не был открыт
                    }
                    throw new QueryWithoutIndexException(query, prettyPrintPlan);
                }
            } catch (DataAccessException ex) {
                // Бывает, например падает с timestamp + LocalDateTime.MIN, почему-то неправильно рендерит
                logger.warn("JooqExplainExecuteListener failed", ex);
            }
        }
    }

    private ExplainResult explainQuery(ExecuteContext cxt, Query query) {
        DSLContext dsl = DSL.using(cxt.configuration());
        String explain = "explain format=json " + dsl.renderInlined(query);
        String explainResult = (String) dsl.fetch(explain).getValue(0, 0);
        return JsonUtils.fromJson(explainResult, ExplainResult.class);
    }
}
