package ru.yandex.market.crm.campaign.services.segments.tasks.algorithms.sendings;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.campaign.services.properties.PropertiesService;
import ru.yandex.market.crm.core.domain.CommonDateRange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QueryUseChytCheckerTest {

    private QueryUseChytChecker queryUseChytChecker;

    @BeforeEach
    public void setUp() {
        PropertiesService propertiesService = mock(PropertiesService.class);
        when(propertiesService.getLong(anyString())).thenReturn(Optional.of(4L));
        queryUseChytChecker = new QueryUseChytChecker(propertiesService);
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                arguments(new RecentQuery(), true),
                arguments(rangeQuery(1, 2), true),
                arguments(rangeQuery(1, 4), true),
                arguments(rangeQuery(1, 5), false),
                arguments(rangeQuery(5, 6, 10, 11), true),
                arguments(rangeQuery(10, 11, 5, 7), false),
                arguments(rangeQuery(10, 12, 12, 13), true),
                arguments(rangeQuery(10, 12, 12, 14), false),
                arguments(rangeQuery(10, 12, 11, 13), true),
                arguments(rangeQuery(11, 14, 10, 12), false),
                arguments(rangeQuery(11, 12, 10, 13), true),
                arguments(rangeQuery(11, 12, 10, 14), false)
        );
    }

    private static RangeQuery rangeQuery(int... days) {
        var rangeQuery = new RangeQuery();
        List<CommonDateRange> periods = IntStream.range(0, days.length / 2)
                .map(i -> i * 2)
                .mapToObj(i -> new CommonDateRange(
                        LocalDate.of(2022, 7, days[i]),
                        LocalDate.of(2022, 7, days[i + 1])
                ))
                .collect(Collectors.toList());
        rangeQuery.setPeriods(periods);
        return rangeQuery;
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testUseChyt(Query query, boolean expected) {
        assertEquals(expected, queryUseChytChecker.useChyt(query));
    }
}
