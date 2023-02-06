package ru.yandex.mail.cerberus.core.mapper;

import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.cerberus.Uid;
import ru.yandex.mail.cerberus.UserType;
import ru.yandex.mail.cerberus.client.dto.User;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(propertySources = "classpath:application_without_database.yml")
class DefaultUserMapperTest {
    @Inject
    private UserMapper mapper;

    private static final Uid UID = new Uid(26373783L);
    private static final User<TestInfo> USER = new User<>(
        UID,
        UserType.BASIC,
        "login",
        new TestInfo("str", 0)
    );

    @Test
    @DisplayName("Verify that 'mapToEntity' applied to user returns correct entity object")
    void testMapUserToEntity() {
        val entity = mapper.mapToEntity(USER);
        val result = mapper.mapToUser(entity, TestInfo.class);
        assertThat(result).isEqualTo(USER);
    }
}
