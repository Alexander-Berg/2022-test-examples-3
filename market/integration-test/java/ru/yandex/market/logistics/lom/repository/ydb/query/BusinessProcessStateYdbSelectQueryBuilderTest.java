package ru.yandex.market.logistics.lom.repository.ydb.query;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.filter.BusinessProcessStateFilter;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateEntityIdTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateTableDescription;
import ru.yandex.market.ydb.integration.YdbTableDescription;

@ParametersAreNonnullByDefault
@DisplayName("Получение запросов для поиска бизнес-процессов в YDB")
class BusinessProcessStateYdbSelectQueryBuilderTest extends AbstractContextualYdbTest {

    @Autowired
    private BusinessProcessStateYdbSelectQueryBuilder selectQueryBuilder;
    @Autowired
    protected BusinessProcessStateTableDescription businessProcessStateTable;
    @Autowired
    protected BusinessProcessStateEntityIdTableDescription businessProcessStateEntityIdTable;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(businessProcessStateTable, businessProcessStateEntityIdTable);
    }

    @Test
    @DisplayName("В фильтре нет условий на поля с индексом")
    void nullIfFilterOnNonIndexedFields() {
        BusinessProcessStateFilter filterWithNonIndexedFieldsOnly = BusinessProcessStateFilter.builder()
            .ids(Set.of())
            .statuses(Set.of())
            .queueTypes(Set.of())
            .entityIdsIntersection(List.of())
            .createdFrom(clock.instant())
            .createdTo(clock.instant().plus(1, ChronoUnit.HOURS))
            .updatedFrom(clock.instant())
            .updatedTo(clock.instant().plus(1, ChronoUnit.DAYS))
            .comment("abcdefg")
            .build();
        softly.assertThat(selectQueryBuilder.getSelectForFilter(filterWithNonIndexedFieldsOnly)).isNull();
    }

    @Test
    @DisplayName("Пустой фильтр")
    void nullIfEmptyFilter() {
        softly.assertThat(selectQueryBuilder.getSelectForFilter(BusinessProcessStateFilter.builder().build()))
            .isNull();
    }
}
