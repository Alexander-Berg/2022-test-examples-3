package ru.yandex.market.logistics.lrm;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.repository.ydb.EntityMetaRepository;
import ru.yandex.market.logistics.lrm.repository.ydb.converter.EntityMetaConverter;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.market.ydb.integration.YdbTemplate;
import ru.yandex.market.ydb.integration.model.Field;
import ru.yandex.market.ydb.integration.query.YdbInsert;

public abstract class AbstractIntegrationYdbTest extends AbstractIntegrationTest {

    protected static final int RETURN_1_HASH = 2106098876;
    protected static final int RETURN_SEGMENT_1_HASH = 326946049;

    @Autowired
    protected YdbTemplate ydbTemplate;

    @Autowired
    private EntityMetaRepository entityMetaRepository;

    @Autowired
    private EntityMetaConverter entityMetaConverter;

    @BeforeEach
    void setUpYdb() {
        getTablesForSetUp().forEach(table -> ydbTemplate.createTable(table.toCreate()));
    }

    @AfterEach
    void tearDownYdb() {
        getTablesForSetUp().forEach(table -> ydbTemplate.truncateTable(table));
    }

    @Nonnull
    protected abstract List<YdbTableDescription> getTablesForSetUp();

    protected <E> void ydbInsert(
        YdbTableDescription table,
        List<E> entities,
        Function<E, Map<String, Object>> converter
    ) {
        List<Field<?, ?>> fields = table.fields();
        YdbInsert.Builder insert = YdbInsert.insert(table, fields.toArray(Field[]::new));

        entities.stream()
            .map(converter)
            .forEach(item -> insert.row(fields.stream().map(field -> item.get(field.name())).toArray()));

        ydbTemplate.update(insert, YdbTemplate.DEFAULT_WRITE);
    }

    @SneakyThrows
    protected JsonNode jsonFile(String relativePath) {
        return objectMapper.readValue(IntegrationTestUtils.extractFileContentInBytes(relativePath), JsonNode.class);
    }

    @SneakyThrows
    protected <T> T readValue(JsonNode value, Class<T> valueType) {
        return objectMapper.treeToValue(value, valueType);
    }

    @Nonnull
    protected Optional<EntityMetaTableDescription.EntityMetaRecord> getEntityMetaRecord(
        int hash,
        String entityType,
        Long entityId,
        String name
    ) {
        return entityMetaRepository.get(hash, entityType, entityId, name, entityMetaConverter);
    }
}
