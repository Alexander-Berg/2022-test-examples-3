package ru.yandex.mail.cerberus.core.mapper;

import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.cerberus.LocationId;
import ru.yandex.mail.cerberus.LocationType;
import ru.yandex.mail.cerberus.client.dto.Location;

import javax.inject.Inject;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(propertySources = "classpath:application_without_database.yml")
public class DefaultLocationMapperTest {
    @Inject
    private LocationMapper mapper;

    private static final Location<TestInfo> LOCATION = new Location<>(
        new LocationId(1L),
        new LocationType("type"),
        "name",
        Optional.of(new TestInfo("str", 42))
    );

    @Test
    @DisplayName("Verify that entity could be mapped to location and backwards")
    void testMapBetweenEntityAndLocation() {
        val entity = mapper.mapToEntity(LOCATION);
        val result = mapper.mapToLocation(entity, TestInfo.class);
        assertThat(result).isEqualTo(LOCATION);
    }
}
