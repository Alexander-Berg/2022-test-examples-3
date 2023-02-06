package ru.yandex.market.tsum.pipelines.common.jobs.delivery;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.tmatesoft.svn.core.SVNLogEntry;

import ru.yandex.market.tsum.release.delivery.ArcadiaVcsChange;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 13.07.18
 */
public class ChangelogTestUtils {
    private ChangelogTestUtils() {
    }

    public static SVNLogEntry createSVNLogEntry(long revision, String... changedPaths) {
        return new SVNLogEntry(createChangedPaths(changedPaths), revision, null, null, null);
    }

    public static ArcadiaVcsChange createSVNLogEntry(long revision, String message, long timestamp,
                                                     String... changedPaths) {
        return new ArcadiaVcsChange(
            revision, Instant.ofEpochSecond(timestamp), Arrays.asList(changedPaths), message, "author"
        );
    }

    private static Map<String, Object> createChangedPaths(String... changedPaths) {
        return Stream.of(changedPaths).collect(Collectors.toMap(Function.identity(), c -> c + " changed"));
    }
}
