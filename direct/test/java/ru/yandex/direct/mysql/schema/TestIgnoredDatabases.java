package ru.yandex.direct.mysql.schema;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class TestIgnoredDatabases {
    @Parameterized.Parameters(name = "{index}: {0} should be ignored={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"sys", true},
                {"mysql", true},
                {"information_schema", true},
                {"performance_schema", true},
                {"#mysql50#lost+found", true},
                {"#mysql50#2016-10-05_17-28-20", true},
                {"mymysql", false},
                {"lost+found", false},
                {"test", false},
                {"ppc", false},
        });
    }

    @Parameterized.Parameter(0)
    public String databaseName;

    @Parameterized.Parameter(1)
    public boolean isIgnored;

    @Test
    public void test() {
        assertThat(ServerSchema.isIgnoredDatabase(databaseName), is(isIgnored));
    }
}
