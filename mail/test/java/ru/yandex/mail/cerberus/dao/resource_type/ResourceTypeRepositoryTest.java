package ru.yandex.mail.cerberus.dao.resource_type;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.yandex.mail.cerberus.ResourceTypeName;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import javax.inject.Inject;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.mail.cerberus.dao.Constants.DB_NAME_PROPERTY;
import static ru.yandex.mail.cerberus.dao.Constants.MIGRATIONS;
import static ru.yandex.mail.cerberus.dao.resource_type.ResourceTypeRepositoryTest.DB_NAME;

@ExtendWith(PgLocalExtension.class)
@InitDb(migration = MIGRATIONS, name = DB_NAME)
@MicronautTest(transactional = false)
@Property(name = DB_NAME_PROPERTY, value = DB_NAME)
class ResourceTypeRepositoryTest {
    static final String DB_NAME = "resource_type_repository_db";

    private static final ResourceTypeName RESOURCE_TYPE_NAME = new ResourceTypeName("test-resource-type");
    private static final String DESCRIPTION = "blah blah";
    private static final Set<String> ACTION_SET = Set.of("one", "two");

    @Inject
    private ResourceTypeRepository repository;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Verify that 'createIfNotExist' create new resource type if it doesn't exist and do nothing otherwise")
    void testInsertIfNotExist() {
        val initialCount = repository.count();
        val entity = new ResourceTypeEntity(RESOURCE_TYPE_NAME, Optional.of(DESCRIPTION), ACTION_SET);

        var result = repository.createIfNotExist(entity);
        assertThat(result.isInserted()).isTrue();
        assertThat(repository.count()).isEqualTo(initialCount + 1);

        result = repository.createIfNotExist(entity);
        assertThat(result.isInserted()).isFalse();
        assertThat(repository.count()).isEqualTo(initialCount + 1);
    }
}
