package ru.yandex.market.logistics.lom.service.businessProcess;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.logistics.lom.entity.enums.EntityType;
import ru.yandex.market.logistics.lom.filter.BusinessProcessStateFilter;
import ru.yandex.market.logistics.lom.jobs.model.EntityId;

@DisplayName("Поиск процессов по фильтру")
@DatabaseSetup("/service/business_process_state/ydb/prepare_get_business_process.xml")
public class SearchSliceProcessesTest extends AbstractBusinessProcessStateYdbServiceTest {

    private static final List<EntityId> INCOMPATIBLE_ENTITY_IDS = List.of(
        EntityId.of(EntityType.ORDER, 1L),
        EntityId.of(EntityType.ORDER, 2L)
    );

    @Test
    @DisplayName("Поиск по несовместимым сущностям, находятся несколько процессов")
    void searchSliceForEntities() {
        softly.assertThat(
                businessProcessStateService.searchSliceForEntities(
                    BusinessProcessStateFilter.builder().entityIdsIntersection(INCOMPATIBLE_ENTITY_IDS).build(),
                    PageRequest.of(0, 100)
                )
            )
            .hasSize(2);
    }

    @Test
    @DisplayName("Поиск по несовместимым сущностям, у 1 процесса не может быть такой набор сущностей")
    void searchSlice() {
        softly.assertThat(
                businessProcessStateService.searchSlice(
                    BusinessProcessStateFilter.builder().entityIdsIntersection(INCOMPATIBLE_ENTITY_IDS).build(),
                    PageRequest.of(0, 100)
                )
            )
            .isEmpty();
    }

    @Test
    @DisplayName("Поиск по сущностям возвращает процессы со всеми сущностями из фильтра")
    void searchSliceAllEntitiesShouldBe() {
        //has order id 2 and segment id 3
        insertProcessesToYdb(1L);
        List<EntityId> entityIds = List.of(
            EntityId.of(EntityType.ORDER, 1L),
            EntityId.of(EntityType.WAYBILL_SEGMENT, 3L)
        );

        softly.assertThat(
                businessProcessStateService.searchSlice(
                    BusinessProcessStateFilter.builder().entityIdsIntersection(entityIds).build(),
                    PageRequest.of(0, 100)
                )
            )
            .isEmpty();
    }

    @DisplayName("Обработка невалидных фильтров")
    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void invalidFilters(String displayName, BusinessProcessStateFilter filter) {
        softly.assertThat(businessProcessStateService.searchSlice(filter, PageRequest.of(0, 100))).isEmpty();
    }

    @Nonnull
    static Stream<Arguments> invalidFilters() {
        return Stream.of(
            Arguments.of(
                "Empty entityIds intersections",
                BusinessProcessStateFilter.builder().entityIdsIntersection(List.of()).build()
            ),
            Arguments.of("Empty ids", BusinessProcessStateFilter.builder().ids(Set.of()).build()),
            Arguments.of("Empty statuses", BusinessProcessStateFilter.builder().statuses(Set.of()).build()),
            Arguments.of("Empty queue types", BusinessProcessStateFilter.builder().queueTypes(Set.of()).build()),
            Arguments.of(
                "EntityIds intersections required",
                BusinessProcessStateFilter.builder()
                    .isFilterByEntityIdsRequired(true)
                    .build()
            )
        );
    }
}
