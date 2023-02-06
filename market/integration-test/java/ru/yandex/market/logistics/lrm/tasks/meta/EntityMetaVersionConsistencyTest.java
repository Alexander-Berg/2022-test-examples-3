package ru.yandex.market.logistics.lrm.tasks.meta;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.DetachedTypedEntity;
import ru.yandex.market.logistics.lrm.service.meta.EntityMeta;
import ru.yandex.market.logistics.lrm.service.meta.EntityMetaService;
import ru.yandex.market.logistics.lrm.service.meta.TypedEntity;
import ru.yandex.market.ydb.integration.YdbTableDescription;

@DisplayName("Проверка совместимости версий мета-информации в YDB")
public class EntityMetaVersionConsistencyTest extends AbstractIntegrationYdbTest {
    private static final TypedEntity ENTITY = new DetachedTypedEntity(EntityType.RETURN, 1L);

    @Autowired
    private EntityMetaTableDescription entityMetaTable;

    @Autowired
    private EntityMetaService entityMetaService;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(entityMetaTable);
    }

    @Test
    @DisplayName("Записываем в формате с аннотацией jsonTypeInfo, читаем без неё")
    void writeOldAndReadNew() {
        entityMetaService.save(ENTITY, new OldMeta(1));
        softly.assertThatCode(() -> {
                Optional<NewMeta> meta = entityMetaService.find(ENTITY, NewMeta.class);
                softly.assertThat(meta.isPresent()).isTrue();
                softly.assertThat(meta.get().getField()).isEqualTo(1);
            })
            .doesNotThrowAnyException();
    }

    interface Meta {
        int getField();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = OldMeta.class, name = "META_TEST")
    })
    interface AnnotatedMeta {
    }

    @Value
    @Builder
    @Jacksonized
    @EntityMeta("meta-test")
    static class OldMeta implements AnnotatedMeta, Meta {
        int field;
    }

    @Value
    @Builder
    @Jacksonized
    @EntityMeta("meta-test")
    static class NewMeta implements Meta {
        int field;
    }
}
