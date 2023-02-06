package ru.yandex.market.logistics.lom;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.logistics.lom.initializer.YdbInitializer;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.market.ydb.integration.YdbTemplate;
import ru.yandex.market.ydb.integration.model.Field;
import ru.yandex.market.ydb.integration.query.YdbInsert;

@ContextConfiguration(initializers = YdbInitializer.class)
public abstract class AbstractContextualYdbTest extends AbstractContextualCommonTest {

    @Autowired
    protected YdbTemplate ydbTemplate;

    @BeforeEach
    void setUpYdb() {
        getTablesForSetUp().forEach(table -> ydbTemplate.createTable(table.toCreate()));
    }

    @AfterEach
    protected void tearDownYdb() {
        getTablesForSetUp().forEach(table -> ydbTemplate.truncateTable(table));
    }

    @Nonnull
    protected abstract List<YdbTableDescription> getTablesForSetUp();

    protected <T> void insertAllIntoTable(
        YdbTableDescription table,
        List<T> entities,
        BiFunction<YdbTableDescription, Object, Map<String, Object>> converter
    ) {
        List<Field<?, ?>> fields = table.fields();

        YdbInsert.Builder insert = YdbInsert.insert(
            table,
            fields.toArray(new Field[0])
        );

        List<Map<String, Object>> items = entities.stream()
            .map(entity -> converter.apply(table, entity))
            .collect(Collectors.toList());

        items.forEach(item -> insert.row(
            fields.stream().map(field -> item.get(field.name())).toArray()
        ));

        ydbTemplate.update(
            insert,
            YdbTemplate.DEFAULT_WRITE
        );
    }
}
