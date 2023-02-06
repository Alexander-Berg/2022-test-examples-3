package ru.yandex.market.admin.service.remote;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.log.ShopLogHelper;
import ru.yandex.market.admin.ui.model.shop.UILogDisplayOptions;
import ru.yandex.market.admin.ui.model.shop.UIShopLogRecord;
import ru.yandex.market.admin.ui.service.SortOrder;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("should be enabled after migrating tests to EmbeddedPostgresConfig")
class RemoteDatasourceUIServiceFunctionalTest extends FunctionalTest {
    @Autowired
    RemoteDatasourceUIService adminShopService;

    private static Stream<Arguments> entityTypes() {
        return Stream.of(
          Arguments.of("shop"),
          Arguments.of("supplier"),
          Arguments.of("business")
        );
    }

    @ParameterizedTest
    @MethodSource("entityTypes")
    void testEmptyFilters(String entityType) {
        var log = getLog(entityType, Set.of());
        assertThat(log).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("entityTypes")
    void testAllFilters(String entityType) {
        var availableActions = ShopLogHelper.getAvailableActions(entityType);
        var log = getLog(entityType, IntStream.range(0, availableActions.size())
                .boxed()
                .collect(Collectors.toSet()));
        assertThat(log).isEmpty();
    }

    private List<UIShopLogRecord> getLog(String entityType, Set<Integer> filterIndexes) {
        return adminShopService.getShopLog(entityType, 1, 1, filterIndexes, makeOptions());
    }

    private static UILogDisplayOptions makeOptions() {
        var result = new UILogDisplayOptions();
        result.setField(UILogDisplayOptions.SORT_FIELD, UIShopLogRecord.DATE_TIME);
        result.setField(UILogDisplayOptions.SORT_ORDER, SortOrder.ASC);
        result.setField(UILogDisplayOptions.FROM_INDEX, 0);
        result.setField(UILogDisplayOptions.TO_INDEX, 1);
        return result;
    }
}
