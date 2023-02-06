package ru.yandex.market.mbi.api.controller.outlet.enums;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.id.HasId;
import ru.yandex.market.core.outlet.moderation.ModerationLevel;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.ModerationLevelDTO;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationLevelEnumTest {
    @Test
    void enumsShouldBeInSync() {
        for (ModerationLevel moderationLevel : ModerationLevel.values()) {
            var moderationLevelDTO = ModerationLevelDTO.valueOf(moderationLevel.name());
            assertThat(moderationLevel).isEqualTo(HasId.getById(ModerationLevel.class, moderationLevelDTO.getId()));
        }

        for (ModerationLevelDTO moderationLevelDTO : ModerationLevelDTO.values()) {
            var moderationLevel = ModerationLevel.valueOf(moderationLevelDTO.name());
            assertThat(moderationLevelDTO).isEqualTo(HasId.getById(ModerationLevelDTO.class, moderationLevel.getId()));
        }
    }
}
