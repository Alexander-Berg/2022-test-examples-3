package ru.yandex.mail.cerberus.dao.resource;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.yandex.mail.cerberus.ResourceId;
import ru.yandex.mail.cerberus.ResourceTypeName;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import javax.inject.Inject;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.mail.cerberus.dao.Constants.DB_NAME_PROPERTY;
import static ru.yandex.mail.cerberus.dao.Constants.MIGRATIONS;
import static ru.yandex.mail.cerberus.dao.resource.ResourceRepositoryTest.DB_NAME;

@ExtendWith(PgLocalExtension.class)
@InitDb(migration = MIGRATIONS, name = DB_NAME)
@MicronautTest(transactional = false)
@Property(name = DB_NAME_PROPERTY, value = DB_NAME)
class ResourceRepositoryTest {
    static final String DB_NAME = "resource_repository_db";

    private static final ResourceTypeName LAYER_TYPE_NAME = new ResourceTypeName("layer");
    private static final List<ResourceEntity> ENTITIES = List.of(
        new ResourceEntity(new ResourceId(42L), LAYER_TYPE_NAME, null, "res", true, Optional.empty()),
        new ResourceEntity(new ResourceId(43L), LAYER_TYPE_NAME, null, "res2", false, Optional.empty())
    );

    @Inject
    private ResourceRepository resourceRepository;

    private static List<ResourceEntity> newEntities = emptyList();

    @BeforeEach
    void init() {
        if (!newEntities.isEmpty()) {
            return;
        }

        newEntities = resourceRepository.insertAll(ENTITIES);
    }

    @Test
    @DisplayName("Verify that select requests returns appropriate records")
    void findByIdTest() {
        val unexistingId = new ResourceId(100500L);
        val ids = StreamEx.of(newEntities)
            .map(ResourceEntity::getId)
            .append(unexistingId)
            .toImmutableList();

        val entities = resourceRepository.findById(LAYER_TYPE_NAME, ids);
        assertThat(entities)
            .containsExactlyInAnyOrderElementsOf(newEntities);
    }
}
