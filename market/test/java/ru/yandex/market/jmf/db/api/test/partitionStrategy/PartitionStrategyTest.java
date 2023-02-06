package ru.yandex.market.jmf.db.api.test.partitionStrategy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;

import ru.yandex.market.jmf.db.api.AbstractByDatePartitionStrategy;
import ru.yandex.market.jmf.db.api.test.partitionStrategy.strategies.ByDateDeletionIntervalOneMonthPartitionStrategyTest;
import ru.yandex.market.jmf.db.api.test.partitionStrategy.strategies.ByDateDeletionIntervalTwoMonthsPartitionStrategyTest;
import ru.yandex.market.jmf.db.api.test.partitionStrategy.strategies.ByDayDeletionIntervalOneDayPartitionStrategyTest;
import ru.yandex.market.jmf.db.api.test.partitionStrategy.strategies.ByDayDeletionIntervalTwoDaysPartitionStrategyTest;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.jmf.db.api.AbstractByDatePartitionStrategy.TABLE_CHILD_RELATION_NAME;

public class PartitionStrategyTest {

    private static final String CREATE_PATTERN = "CREATE TABLE IF NOT EXISTS %s PARTITION OF " +
            "%s ( PRIMARY KEY (id) ) FOR VALUES FROM ('%s') TO ('%s')";

    private static final String DELETE_PATTERN = "DROP TABLE IF EXISTS %s;";

    private final Statement statement = Mockito.mock(Statement.class);

    private static List<Arguments> createData() {
        return List.of(
                Arguments.of(
                        new ByDayDeletionIntervalOneDayPartitionStrategyTest(),
                        LocalDate.of(2022, 5, 15),
                        List.of(
                                LocalDate.of(2022, 5, 15),
                                LocalDate.of(2022, 5, 16)
                        )
                ),
                Arguments.of(
                        new ByDayDeletionIntervalTwoDaysPartitionStrategyTest(),
                        LocalDate.of(2022, 5, 15),
                        List.of(
                                LocalDate.of(2022, 5, 15),
                                LocalDate.of(2022, 5, 16)
                        )
                ),
                Arguments.of(
                        new ByDateDeletionIntervalOneMonthPartitionStrategyTest(),
                        LocalDate.of(2022, 5, 1),
                        List.of(
                                LocalDate.of(2022, 5, 1),
                                LocalDate.of(2022, 6, 1)
                        )
                ),
                Arguments.of(
                        new ByDateDeletionIntervalTwoMonthsPartitionStrategyTest(),
                        LocalDate.of(2022, 5, 1),
                        List.of(
                                LocalDate.of(2022, 5, 1),
                                LocalDate.of(2022, 6, 1)
                        )
                )
        );
    }

    /**
     * strategy - проверяемая стратегия
     * today - "сегодняшняя" дата, относительно которой стратегия создает партиции
     * result - коллекция партиций которые будут созданы стратегией
     */

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource(value = "createData")
    public void create(AbstractByDatePartitionStrategy strategy, LocalDate today, Collection<LocalDate> result) throws SQLException {
        List<String> results = result.stream()
                .map(x -> String.format(CREATE_PATTERN,
                                strategy.partitionName(x),
                                "tbl_test",
                                strategy.boundValue(x),
                                strategy.boundValue(strategy.plusInterval(x, 1))
                        )
                ).toList();

        strategy.create(statement, today);

        for (var r : results) {
            verify(statement, times(1)).execute(r);
        }
    }


    private static List<Arguments> deleteData() {
        return List.of(
                Arguments.of(
                        new ByDayDeletionIntervalOneDayPartitionStrategyTest(),
                        LocalDate.of(2022, 5, 15),
                        List.of(
                                LocalDate.of(2022, 5, 12),
                                LocalDate.of(2022, 5, 13),
                                LocalDate.of(2022, 5, 14),
                                LocalDate.of(2022, 5, 15),
                                LocalDate.of(2022, 5, 16)
                        ),
                        List.of(
                                LocalDate.of(2022, 5, 12),
                                LocalDate.of(2022, 5, 13)
                        )
                ),
                Arguments.of(
                        new ByDayDeletionIntervalTwoDaysPartitionStrategyTest(),
                        LocalDate.of(2022, 5, 15),
                        List.of(
                                LocalDate.of(2022, 5, 12),
                                LocalDate.of(2022, 5, 13),
                                LocalDate.of(2022, 5, 14),
                                LocalDate.of(2022, 5, 15),
                                LocalDate.of(2022, 5, 16)
                        ),
                        List.of(
                                LocalDate.of(2022, 5, 12)
                        )
                ),
                Arguments.of(
                        new ByDateDeletionIntervalOneMonthPartitionStrategyTest(),
                        LocalDate.of(2022, 5, 1),
                        List.of(
                                LocalDate.of(2022, 2, 1),
                                LocalDate.of(2022, 3, 1),
                                LocalDate.of(2022, 4, 1),
                                LocalDate.of(2022, 5, 1),
                                LocalDate.of(2022, 6, 1)
                        ),
                        List.of(
                                LocalDate.of(2022, 2, 1),
                                LocalDate.of(2022, 3, 1)
                        )
                ),
                Arguments.of(
                        new ByDateDeletionIntervalOneMonthPartitionStrategyTest(),
                        LocalDate.of(2022, 5, 15),
                        List.of(
                                LocalDate.of(2022, 2, 1),
                                LocalDate.of(2022, 3, 1),
                                LocalDate.of(2022, 4, 1),
                                LocalDate.of(2022, 5, 1),
                                LocalDate.of(2022, 6, 1)
                        ),
                        List.of(
                                LocalDate.of(2022, 2, 1),
                                LocalDate.of(2022, 3, 1),
                                LocalDate.of(2022, 4, 1)
                        )
                ),
                Arguments.of(
                        new ByDateDeletionIntervalTwoMonthsPartitionStrategyTest(),
                        LocalDate.of(2022, 5, 1),
                        List.of(
                                LocalDate.of(2022, 2, 1),
                                LocalDate.of(2022, 3, 1),
                                LocalDate.of(2022, 4, 1),
                                LocalDate.of(2022, 5, 1),
                                LocalDate.of(2022, 6, 1)
                        ),
                        List.of(
                                LocalDate.of(2022, 2, 1)
                        )
                ),
                Arguments.of(
                        new ByDateDeletionIntervalTwoMonthsPartitionStrategyTest(),
                        LocalDate.of(2022, 5, 15),
                        List.of(
                                LocalDate.of(2022, 2, 1),
                                LocalDate.of(2022, 3, 1),
                                LocalDate.of(2022, 4, 1),
                                LocalDate.of(2022, 5, 1),
                                LocalDate.of(2022, 6, 1)
                        ),
                        List.of(
                                LocalDate.of(2022, 2, 1),
                                LocalDate.of(2022, 3, 1)
                        )
                )

        );
    }

    /**
     * strategy - проверяемая стратегия
     * today - "сегодняшняя" дата, относительно которой стратегия удаляет партиции
     * dates - коллекция существующих партиций за выбранную дату
     * result - коллекция партиций которые будут удалены стратегией
     */

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource(value = "deleteData")
    public void delete(AbstractByDatePartitionStrategy strategy, LocalDate today, Collection<LocalDate> dates,
                       Collection<LocalDate> result) throws SQLException {
        Collection<String> tableNames = dates
                .stream()
                .map(strategy::partitionName)
                .toList();

        String resultString = result
                .stream()
                .map(x -> String.format(DELETE_PATTERN, strategy.partitionName(x)))
                .collect(Collectors.joining());

        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Collection<Boolean> nextCollect = Stream.concat(
                        tableNames.stream().map(x -> Boolean.TRUE),
                        Stream.of(Boolean.FALSE))
                .toList();
        when(resultSet.next()).thenAnswer(AdditionalAnswers.returnsElementsOf(nextCollect));
        when(resultSet.getString(TABLE_CHILD_RELATION_NAME)).thenAnswer(AdditionalAnswers.returnsElementsOf(tableNames));

        when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);

        strategy.delete(statement, today);

        verify(statement, times(1)).execute(resultString);
    }
}


