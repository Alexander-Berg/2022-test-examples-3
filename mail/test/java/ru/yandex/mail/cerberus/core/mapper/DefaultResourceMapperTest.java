package ru.yandex.mail.cerberus.core.mapper;

import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.cerberus.LocationId;
import ru.yandex.mail.cerberus.LocationKey;
import ru.yandex.mail.cerberus.LocationType;
import ru.yandex.mail.cerberus.ResourceId;
import ru.yandex.mail.cerberus.ResourceTypeName;
import ru.yandex.mail.cerberus.client.dto.Resource;
import ru.yandex.mail.cerberus.client.dto.ResourceData;

import javax.inject.Inject;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(propertySources = "classpath:application_without_database.yml")
class DefaultResourceMapperTest {
    @Inject
    private ResourceMapper mapper;

    private static final ResourceId ID = new ResourceId(100500L);
    private static final ResourceTypeName TYPE_NAME = new ResourceTypeName("type name");
    private static final LocationKey LOCATION = new LocationKey(new LocationId(42L), new LocationType("type"));
    private static final Resource<TestInfo> RESOURCE = new Resource<>(
        ID,
        new ResourceData<>(
            TYPE_NAME,
            "name",
            Optional.of(LOCATION),
            true,
            new TestInfo("str", 0)
        )
    );

    @Test
    @DisplayName("Verify that 'mapToEntity' applied to resource returns correct entity object")
    void testMapResourceToEntity() {
        val entity = mapper.mapToEntity(RESOURCE);
        val result = mapper.mapToResource(entity, TestInfo.class);
        assertThat(result).isEqualTo(RESOURCE);
    }

    @Test
    @DisplayName("Verify that 'mapToEntity' applied to resource data returns correct entity object")
    void testMapDataToEntity() {
        val data = RESOURCE.getData();
        val entity = mapper.mapToEntity(ID, data);
        val result = mapper.mapToResource(entity, TestInfo.class);
        val expectedGroup = new Resource<>(ID, data);
        assertThat(result).isEqualTo(expectedGroup);
    }
}
