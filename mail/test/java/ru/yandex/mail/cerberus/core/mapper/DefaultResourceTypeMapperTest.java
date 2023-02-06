package ru.yandex.mail.cerberus.core.mapper;

import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.cerberus.ResourceTypeName;
import ru.yandex.mail.cerberus.client.dto.ResourceType;
import ru.yandex.mail.cerberus.dao.resource_type.ResourceTypeEntity;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

enum Action {
    DIG,
    DONT_DIG
}

@MicronautTest(propertySources = "classpath:application_without_database.yml")
class DefaultResourceTypeMapperTest {
    @Inject
    private ResourceTypeMapper mapper;

    private static final ResourceTypeName NAME = new ResourceTypeName("name");
    private static final ResourceType RESOURCE_TYPE_DATA = new ResourceType(
        NAME,
        "description",
        Action.class
    );

    @Test
    @DisplayName("Verify that 'mapToEntity' applied to resource type data returns correct entity object")
    void mapTypeDataToEntityTest() {
        val entity = mapper.mapToEntity(RESOURCE_TYPE_DATA);
        val expectedEntity = new ResourceTypeEntity(
            NAME,
            Optional.of("description"),
            Set.of("DIG", "DONT_DIG")
        );
        assertThat(entity).isEqualTo(expectedEntity);
    }
}
