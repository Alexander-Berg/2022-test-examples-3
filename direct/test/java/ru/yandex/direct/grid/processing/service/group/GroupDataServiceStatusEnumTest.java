package ru.yandex.direct.grid.processing.service.group;

import java.util.Arrays;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Test;

import ru.yandex.direct.grid.core.entity.group.model.GdiGroupModerationStatus;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupPrimaryStatus;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupPrimaryStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.core.entity.group.repository.GridAdGroupStatusPredicates.ADGROUP_PRIMARY_STATUS_PREDICATES;
import static ru.yandex.direct.grid.core.entity.group.repository.GridAdGroupStatusPredicates.STATUS_MODERATE_PREDICATES;

public class GroupDataServiceStatusEnumTest {

    @Test
    public void testCountOfPrimaryStatus() {
        assertThat(ADGROUP_PRIMARY_STATUS_PREDICATES).containsOnlyKeys(GdiGroupPrimaryStatus.values());
    }

    @Test
    public void testCountOfModerationStatus() {
        assertThat(STATUS_MODERATE_PREDICATES).containsOnlyKeys(GdiGroupModerationStatus.values());
    }

    @Test
    public void testGdAdGroupPrimaryStatusEnumSequence() {
        List<String> expectedValues = Arrays.asList("DRAFT", "ARCHIVED", "STOPPED", "MODERATION", "ACTIVE", "REJECTED");

        assertThat(StreamEx.of(GdAdGroupPrimaryStatus.values()).map(Enum::name).toList())
                .containsExactlyElementsOf(expectedValues);
    }
}
