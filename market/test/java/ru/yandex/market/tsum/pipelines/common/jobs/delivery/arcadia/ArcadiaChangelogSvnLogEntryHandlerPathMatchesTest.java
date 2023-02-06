package ru.yandex.market.tsum.pipelines.common.jobs.delivery.arcadia;

import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class ArcadiaChangelogSvnLogEntryHandlerPathMatchesTest {
    private final String changedPath;
    private final String dependencyPath;
    private final boolean pathMatches;

    private ArcadiaChangelogSvnLogEntryHandler handler;

    public ArcadiaChangelogSvnLogEntryHandlerPathMatchesTest(String changedPath, String dependencyPath,
                                                             boolean pathMatches) {
        this.changedPath = changedPath;
        this.dependencyPath = dependencyPath;
        this.pathMatches = pathMatches;
    }

    @Parameterized.Parameters(name = "{0} against {1} => {2}")
    public static Collection<Object[]> parameters() {
        return List.of(
            new Object[]{"/trunk/arcadia/a", "a", true},
            new Object[]{"trunk/arcadia/a", "a", true},
            new Object[]{"trunk/arcadia/a/", "a", true},
            new Object[]{"trunk/arcadia/a/1.txt", "a", true},
            new Object[]{"branches/junk/market/whatever/arcadia/a/1.txt", "a", true},
            new Object[]{"trunk/arcadia/b/1.txt", "a", false},
            new Object[]{"trunk/arcadia/a/1.txt", "arcadia/a", false},
            new Object[]{"trunk/arcadia/a/arcadia/b/1.txt", "b", false},
            new Object[]{"trunk/arcadia/a/arcadia/b/1.txt", "a/arcadia/b", true}
        );
    }

    @Before
    public void setUp() throws Exception {
        handler = new ArcadiaChangelogSvnLogEntryHandler(List.of(dependencyPath));
    }

    @Test
    public void pathMatches() {
        assertThat(handler.pathMatches(changedPath), equalTo(pathMatches));
    }
}
