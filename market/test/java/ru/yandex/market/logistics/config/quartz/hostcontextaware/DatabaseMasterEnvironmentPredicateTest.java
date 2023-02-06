package ru.yandex.market.logistics.config.quartz.hostcontextaware;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseMasterEnvironmentPredicateTest {

    private static final String GENCFG_HOSTNAME =
        "sas2-4538-f23-sas-market-prod--503-15375.gencfg-c.yandex.net165012544477";

    private static final String YP_HOSTNAME =
        "t4jie36m6a7ym5gt.sas.yp-c.yandex.net16456046148434346";
    private static final String DC = "sas";

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIsMaster(
        String displayName,
        String hostname,
        List<String> masters,
        List<String> excludedMasters,
        List<String> replicas,
        boolean expected
    ) throws SQLException {

        DatabaseMasterEnvironmentPredicate predicate = new DatabaseMasterEnvironmentPredicate(
            hostname, new HashSet<>(masters), new HashSet<>(excludedMasters), false, Duration.ZERO
        );
        Connection connection = mock(Connection.class, Answers.RETURNS_DEEP_STUBS);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.prepareStatement(any()).executeQuery()).thenReturn(resultSet);
        if (replicas.isEmpty()) {
            when(resultSet.next()).thenReturn(false);
        } else {
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getString(any())).thenReturn(
                replicas.stream().findFirst().orElseThrow(RuntimeException::new)
            );
        }

        assertThat(predicate.isMaster(connection)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testExtractDataCenter(
        String displayName,
        String hostname,
        String expectedDc
    ) {
        DatabaseMasterEnvironmentPredicate predicate = new DatabaseMasterEnvironmentPredicate(
            hostname, new HashSet<>(), Collections.emptySet(), false, Duration.ZERO
        );
        assertThat(predicate.getHostDataCenter()).isEqualTo(expectedDc);
    }

    @Nonnull
    private static Stream<Arguments> testIsMaster() {
        return Stream.of(
            Arguments.of(
                "hostname is null",
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                true
            ),
            Arguments.of(
                "gencfg host is replica, but considered master",
                GENCFG_HOSTNAME,
                Collections.singletonList(DC),
                Collections.emptyList(),
                Collections.singletonList(DC),
                true
            ),
            Arguments.of(
                "yp host is replica, but considered master",
                YP_HOSTNAME,
                Collections.singletonList(DC),
                Collections.emptyList(),
                Collections.singletonList(DC),
                true
            ),
            Arguments.of(
                "gencfg host is not replica",
                GENCFG_HOSTNAME,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                true
            ),
            Arguments.of(
                "yp host is not replica",
                YP_HOSTNAME,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                true
            ),
            Arguments.of(
                "gencfg host is not replica, but excluded from master",
                GENCFG_HOSTNAME,
                Collections.emptyList(),
                Collections.singletonList(DC),
                Collections.emptyList(),
                false
            ),
            Arguments.of(
                "yp host is not replica, but excluded from master",
                YP_HOSTNAME,
                Collections.emptyList(),
                Collections.singletonList(DC),
                Collections.emptyList(),
                false
            ),
            Arguments.of(
                "gencfg host is replica",
                GENCFG_HOSTNAME,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList(DC),
                false
            ),
            Arguments.of(
                "yp is replica",
                YP_HOSTNAME,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList(DC),
                false
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> testExtractDataCenter() {
        return Stream.of(
            Arguments.of(
                "val gencfg",
                "vla2-4538-f23-vla-market-prod--705-25675.gencfg-c.yandex.net1650460954477",
                "vla"
            ),
            Arguments.of(
                "val yp",
                "t3jie4flxa7ymqgt.vla.yp-c.yandex.net1650461484386",
                "vla"
            ),
            Arguments.of(
                "man gencfg",
                "man2-0219-f59-man-market-prod--0fc-25707.gencfg-c.yandex.net1650462119102",
                "man"
            ),
            Arguments.of(
                "man yp",
                "t3jie4flxa7ymqgt.man.yp-c.yandex.net1650461484386",
                "man"
            ),
            Arguments.of(
                "empty",
                "",
                null
            ),
            Arguments.of(
                "null",
                null,
                null
            )
        );
    }
}
