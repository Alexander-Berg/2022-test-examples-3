package ru.yandex.market.logistics.config.quartz.hostcontextaware;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
class DatabaseMasterEnvironmentPredicateIntegrationTest extends AbstractContextualTest {

    @Autowired
    private DataSource dataSource;

    @ParameterizedTest(name = "{0}")
    @MethodSource("testIsMasterCached")
    void testIsMasterCached(
        String displayName,
        DatabaseMasterEnvironmentPredicate predicate,
        int databaseCalls
    ) throws SQLException {
        Connection connection = spy(dataSource.getConnection());
        predicate.isMaster(connection);
        verify(connection).prepareStatement(any());

        predicate.isMaster(connection);
        verify(connection, times(databaseCalls)).prepareStatement(any());
    }

    @Nonnull
    private static Stream<Arguments> testIsMasterCached() {
        return Stream.of(
            Arguments.of(
                "cache enabled",
                new DatabaseMasterEnvironmentPredicate(
                    "", Collections.emptySet(), Collections.emptySet(), true, Duration.ofDays(1)
                ),
                1
            ),
            Arguments.of(
                "short cache enabled",
                new DatabaseMasterEnvironmentPredicate(
                    "", Collections.emptySet(), Collections.emptySet(), true, Duration.ZERO
                ),
                2
            ),
            Arguments.of(
                "cache disabled",
                new DatabaseMasterEnvironmentPredicate(
                    "", Collections.emptySet(), Collections.emptySet(), false, Duration.ofDays(1)
                ),
                2
            )
        );
    }
}
