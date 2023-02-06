package ru.yandex.market.health.configs.common.versionedconfig;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionedConfigDaoCreateConfigTest extends VersionedConfigDaoBaseTest {
    @Test
    public void shouldInsert() {
        //mongo хранит время с точностью до секунд
        Instant createdTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        dao.createConfig(config("name1", createdTime));
        dao.createConfig(config("name2", createdTime));
        assertThat(dao.getAllConfigs())
            .extracting(
                VersionedConfigEntity::getId,
                VersionedConfigEntity::getCreatedTime,
                VersionedConfigEntity::getCurrentVersion
            )
            .containsExactlyInAnyOrder(
                Assertions.tuple("name1", createdTime, null),
                Assertions.tuple("name2", createdTime, null)
            );
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldThrow_WhenConfigAlreadyExists() {
        dao.createConfig(config("name1"));
        Assertions.assertThatThrownBy(() -> dao.createConfig(config("name1")))
            .isInstanceOf(ConfigAlreadyExistsException.class)
            .hasMessageContaining("name1");
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldThrow_WhenConfigIdIsEmptyOrNull() {
        Assertions.assertThatThrownBy(() -> dao.createConfig(config("")))
            .isInstanceOf(ConfigValidationException.class);
        Assertions.assertThatThrownBy(() -> dao.createConfig(config(null)))
            .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldThrow_WhenConfigHasCurrentVersion() {
        Assertions.assertThatThrownBy(() -> dao.createConfig(config("name1", version("v1", 0, 123))))
            .isInstanceOf(ConfigValidationException.class);
        Assertions.assertThatThrownBy(() -> dao.createConfig(config(null)))
            .isInstanceOf(ConfigValidationException.class);
    }
}
