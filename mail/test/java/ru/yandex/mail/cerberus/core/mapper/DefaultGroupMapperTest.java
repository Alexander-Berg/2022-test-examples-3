package ru.yandex.mail.cerberus.core.mapper;

import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.cerberus.GroupId;
import ru.yandex.mail.cerberus.GroupType;
import ru.yandex.mail.cerberus.client.dto.Group;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(propertySources = "classpath:application_without_database.yml")
class DefaultGroupMapperTest {
    @Inject
    private GroupMapper mapper;

    private static final GroupId ID = new GroupId(100500L);
    private static final GroupType TYPE = new GroupType("type");
    private static final Group<TestInfo> GROUP = new Group<>(
        ID,
        TYPE,
        "name",
        true,
        new TestInfo("str", 0)
    );

    @Test
    @DisplayName("Verify that entity could be mapped to group and backwards")
    void testMapBetweenEntityAndGroup() {
        val entity = mapper.mapToEntity(GROUP);
        val result = mapper.mapToGroup(entity, TestInfo.class);
        assertThat(result).isEqualTo(GROUP);
    }
}
