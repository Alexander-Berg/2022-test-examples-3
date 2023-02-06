package ru.yandex.market.logistics.cs.repository;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.domain.entity.Capacity;
import ru.yandex.market.logistics.cs.domain.jdbc.MappedCapacity;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Репозиторий капасити")
@DatabaseSetup("/repository/capacity/before/base_capacity_tree.xml")
class CapacityRepositoryIT extends AbstractIntegrationTest {
    @Autowired
    private CapacityRepository capacityRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Map<Long, Capacity> EXPECTED_CAPACITIES = ImmutableMap.<Long, Capacity>builder()
        .putAll(Map.of(
            1L, Capacity.builder().id(1L).path("1").name("root 1").build(),
            2L, Capacity.builder().id(2L).path("1.2").parentId(1L).name("node 2").build(),
            3L, Capacity.builder().id(3L).path("1.3").parentId(1L).name("node 3").build(),
            4L, Capacity.builder().id(4L).path("1.2.4").parentId(2L).name("node 4").build(),
            5L, Capacity.builder().id(5L).path("1.2.5").parentId(2L).name("node 5").build(),
            6L, Capacity.builder().id(6L).path("1.3.6").parentId(3L).name("node 6").build(),
            7L, Capacity.builder().id(7L).path("1.3.7").parentId(3L).name("node 7").build()
        ))
        .putAll(Map.of(
            10L, Capacity.builder().id(10L).path("10").name("root 10").build(),
            11L, Capacity.builder().id(11L).path("10.11").parentId(10L).name("node 11").build(),
            12L, Capacity.builder().id(12L).path("10.12").parentId(10L).name("node 12").build(),
            13L, Capacity.builder().id(13L).path("10.11.13").parentId(11L).name("node 13").build(),
            14L, Capacity.builder().id(14L).path("10.11.14").parentId(11L).name("node 14").build(),
            15L, Capacity.builder().id(15L).path("10.12.15").parentId(12L).name("node 15").build(),
            16L, Capacity.builder().id(16L).path("10.12.16").parentId(12L).name("node 16").build()
        ))
        .build();

    private static Set<ExpectedSearchResult> expectedCapacityAncestorsTree1() {
        return Set.of(
            ExpectedSearchResult.of(10L, Set.of(1L)),
            ExpectedSearchResult.of(20L, Set.of()),
            ExpectedSearchResult.of(30L, Set.of(3L)),
            ExpectedSearchResult.of(40L, Set.of(4L)),
            ExpectedSearchResult.of(50L, Set.of()),
            ExpectedSearchResult.of(60L, Set.of(6L)),
            ExpectedSearchResult.of(70L, Set.of())
        );
    }

    private static Set<ExpectedSearchResult> expectedCapacityAncestorsTree2() {
        return Set.of(
            ExpectedSearchResult.of(100L, Set.of()),
            ExpectedSearchResult.of(110L, Set.of(11L)),
            ExpectedSearchResult.of(120L, Set.of()),
            ExpectedSearchResult.of(130L, Set.of()),
            ExpectedSearchResult.of(140L, Set.of(14L)),
            ExpectedSearchResult.of(150L, Set.of()),
            ExpectedSearchResult.of(160L, Set.of(16L))
        );
    }

    private static Stream<Arguments> generateCombinationArgs(Set<ExpectedSearchResult> results, int size) {
        return Sets.combinations(results, size).stream()
            .map(Collection::stream)
            .map(s -> s.reduce(ExpectedSearchResult::union))
            .flatMap(Optional::stream)
            .map(r -> Arguments.of(order(r.getServiceIds()), order(r.getExpectedIds()), r.getExpectedPairs()));
    }

    private static Set<Long> order(Set<Long> data) {
        return data.stream().sorted(Long::compareTo).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Stream<Arguments> expectedCapacityAncestorsProvider() {
        return Streams.concat(
            IntStream.range(1, 3)
                .mapToObj(size -> generateCombinationArgs(expectedCapacityAncestorsTree1(), size))
                .flatMap(Function.identity()),
            IntStream.range(1, 3)
                .mapToObj(size -> generateCombinationArgs(expectedCapacityAncestorsTree2(), size))
                .flatMap(Function.identity())
        );
    }

    @ParameterizedTest(name = "Для сервисов {0} ожидается капасити с идентификаторами {1}")
    @DisplayName("Корректность поиска потомков по набору идентификаторов сервисов")
    @MethodSource("expectedCapacityAncestorsProvider")
    void testAncestorsSearch(Set<Long> serviceIds, Collection<Long> expectedIds, Set<Entry<Long, Long>> expectedPairs) {
        List<MappedCapacity> ancestors = capacityRepository.findAncestorsByServiceIdsInclusive(serviceIds);
        softly.assertThat(ancestors.stream().map(c -> c.getCapacity().getId()).distinct())
            .containsExactlyInAnyOrderElementsOf(expectedIds);
        softly.assertThat(ancestors.stream()
                .map(c -> (Entry<Long, Long>) new SimpleEntry<>(c.getServiceId(), c.getCapacity().getId())))
            .containsExactlyInAnyOrderElementsOf(expectedPairs);
        softly.assertThat(ancestors.stream().map(MappedCapacity::getCapacity)
                .map(c -> c.toBuilder().build())
                .distinct())
            .containsExactlyInAnyOrderElementsOf(expectedIds.stream().map(EXPECTED_CAPACITIES::get).collect(toList()));
    }

    @Test
    @DisplayName("Поиск по пустому множеству сервисов")
    void emptyAncestorsSearch() {
        assertTrue(capacityRepository.findAncestorsByServiceIdsInclusive(Set.of()).isEmpty());
    }

    @Test
    @DisplayName("Корректная обработка множественных ссылок на сервисы")
    void multipleMappingVersionSupport() {
        jdbcTemplate.execute("DELETE FROM service_capacity_mapping where service_id=10 and capacity_id=1;");
        jdbcTemplate.execute("INSERT INTO service_capacity_mapping (capacity_id, service_id) VALUES (1, 10);");
        jdbcTemplate.execute("INSERT INTO service_capacity_mapping (capacity_id, service_id) VALUES (1, 11);");
        jdbcTemplate.execute("DELETE FROM service_capacity_mapping where service_id=11 and capacity_id=1;");
        jdbcTemplate.execute("INSERT INTO service_capacity_mapping (capacity_id, service_id) VALUES (1, 12);");

        List<MappedCapacity> result = capacityRepository.findAncestorsByServiceIdsInclusive(Set.of(10L, 11L, 12L));
        assertEquals(2, result.size());
        Map<Long, Long> serviceVersions = result.stream()
            .map(c -> new SimpleEntry<>(c.getServiceId(), c.getMappingVersion()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        assertEquals(Map.of(10L, 3L, 12L, 1L), serviceVersions);
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class ExpectedSearchResult {
        Set<Long> serviceIds;
        Set<Long> expectedIds;
        Set<Entry<Long, Long>> expectedPairs = new HashSet<>();

        public static ExpectedSearchResult of(long serviceId, Set<Long> expectedIds) {
            ExpectedSearchResult result = new ExpectedSearchResult(Set.of(serviceId), expectedIds);
            expectedIds.forEach(id -> result.expectedPairs.add(new SimpleEntry<>(serviceId, id)));
            return result;
        }

        public ExpectedSearchResult union(ExpectedSearchResult that) {
            ExpectedSearchResult result = new ExpectedSearchResult(
                Sets.union(serviceIds, that.getServiceIds()),
                Sets.union(expectedIds, that.getExpectedIds())
            );
            result.expectedPairs.addAll(this.expectedPairs);
            result.expectedPairs.addAll(that.expectedPairs);
            return result;
        }
    }
}
