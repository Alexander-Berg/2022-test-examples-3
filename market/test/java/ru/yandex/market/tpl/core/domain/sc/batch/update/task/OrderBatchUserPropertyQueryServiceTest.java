package ru.yandex.market.tpl.core.domain.sc.batch.update.task;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyMergingQueryService;
import ru.yandex.market.tpl.core.domain.user.UserSpecification;
import ru.yandex.market.tpl.core.domain.user.projection.UserWPropertiesProjection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class OrderBatchUserPropertyQueryServiceTest {

    @Test
    void filterUserIdsWithBatchesEnabled() {
        var userPropertyMergingQueryServiceMock = Mockito.mock(UserPropertyMergingQueryService.class);
        Mockito.when(userPropertyMergingQueryServiceMock.findAllMergedPropertiesForUsers(UserSpecification.builder()
                .userIds(Set.of(1L, 2L, 3L))
                .build()))
                .thenReturn(Map.of(
                        1L, UserWPropertiesProjection.builder()
                                .properties(Map.of(UserProperties.USER_ORDER_BATCH_SHIFTING_ENABLED, "true"))
                                .build(),
                        2L, UserWPropertiesProjection.builder()
                                .properties(Map.of(UserProperties.USER_ORDER_BATCH_SHIFTING_ENABLED, "false"))
                                .build(),
                        3L, UserWPropertiesProjection.builder()
                                .properties(Map.of(UserProperties.FEATURE_LIFE_POS_ENABLED, "true"))
                                .build()
                ));
        var orderBatchUserPropertyQueryService = new OrderBatchUserPropertyQueryService(userPropertyMergingQueryServiceMock);

        Set<Long> filteredUserIds = orderBatchUserPropertyQueryService.filterUserIdsWithBatchesEnabled(Set.of(1L, 2L, 3L));

        assertThat(filteredUserIds).containsExactly(1L);
    }
}
