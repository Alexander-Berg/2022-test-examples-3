package ru.yandex.market.sc.core.domain.stage;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
class StageQueryServiceTest {
    @Autowired
    StageQueryService stageQueryService;

    @ParameterizedTest
    @MethodSource("getSortableStatuses")
    void checkBySortableStatus(SortableStatus status) {
        assertThat(stageQueryService.findStagesBySortableStatusOrThrow(status)).isNotEmpty();
    }

    private static List<SortableStatus> getSortableStatuses() {
        return Arrays.stream(SortableStatus.values())
                .filter(st -> st != SortableStatus.PREPARED_RETURN)
                .toList();
    }

}
