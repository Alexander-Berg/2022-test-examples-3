package ru.yandex.direct.dbutil.wrapper;

import java.util.HashMap;

import com.google.common.collect.ImmutableMap;
import org.jooq.ExecuteContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.tracing.Trace;
import ru.yandex.direct.tracing.util.TraceCommentVars;
import ru.yandex.direct.tracing.util.TraceCommentVarsHolder;
import ru.yandex.direct.utils.hostname.SimpleHostnameResolver;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class QueryTaggerTest {
    private static final ImmutableMap<String, String> EMPTY_COMMENT_VARS = ImmutableMap.of();
    private static final ImmutableMap<String, String> NON_EMPTY_COMMENT_VARS = ImmutableMap.of(
            "operator", "12345", "foo", "bar */");

    @Parameterized.Parameter(0)
    public long spanId;
    @Parameterized.Parameter(1)
    public String service;
    @Parameterized.Parameter(2)
    public String method;
    @Parameterized.Parameter(3)
    public EnvironmentType environmentType;
    @Parameterized.Parameter(4)
    public String comment;
    @Parameterized.Parameter(5)
    public ImmutableMap<String, String> commentVars;

    private Trace trace = Trace.current();

    private DatabaseWrapper.QueryTagger tagger;

    @Parameterized.Parameters(name = "{0} {1} {2} {3}")
    public static Iterable<Object[]> data() {
        String hostStr = String.format("/* %s@%s */", System.getProperty("user.name"),
                new SimpleHostnameResolver().getHostname());
        return asList(new Object[][]{
                {42L, "dir:ct.web", "show */Camps", EnvironmentType.PRODUCTION,
                        "/* reqid:42:dir_ct.web:show___Camps:foo=bar___:operator=12345 */", NON_EMPTY_COMMENT_VARS},
                {42L, "dir:ct.web", "show */Camps", EnvironmentType.DEVELOPMENT,
                        "/* reqid:42:dir_ct.web:show___Camps:foo=bar___:operator=12345 */ " + hostStr,
                        NON_EMPTY_COMMENT_VARS},
                {42L, "direct.web", "showCamps", EnvironmentType.PRODUCTION,
                        "/* reqid:42:direct.web:showCamps */", EMPTY_COMMENT_VARS},
                {42L, "dir:ct.web", "show */Camps", EnvironmentType.PRODUCTION,
                        "/* reqid:42:dir_ct.web:show___Camps */", EMPTY_COMMENT_VARS},
                {42L, "dir:ct.web", "show */Camps", EnvironmentType.DEVELOPMENT,
                        "/* reqid:42:dir_ct.web:show___Camps */ " + hostStr, EMPTY_COMMENT_VARS},
                {42L, "dir:ct.web", "show */Camps", EnvironmentType.DEVELOPMENT,
                        "/* reqid:42:dir_ct.web:show___Camps */ " + hostStr, EMPTY_COMMENT_VARS},
        });
    }

    @Before
    public void init() {
        trace = mock(Trace.class);
        when(trace.getSpanId()).thenReturn(spanId);
        when(trace.getService()).thenReturn(service);
        when(trace.getMethod()).thenReturn(method);
        Trace.push(trace);

        tagger = new DatabaseWrapper.QueryTagger(environmentType);

        TraceCommentVarsHolder.set(new TraceCommentVars(new HashMap<>(commentVars)));
    }

    @After
    public void cleanup() {
        Trace.pop();
        TraceCommentVarsHolder.get().getMap().clear();
    }


    @Test
    public void renderEnd_SelectQueryIsTagged() {
        ExecuteContext context = mock(ExecuteContext.class);
        String[] batchSql = {"SELECT * FROM temp"};
        when(context.batchSQL()).thenReturn(batchSql);

        tagger.renderEnd(context);

        assertEquals("SELECT " + comment + " * FROM temp", batchSql[0]);
    }

    @Test
    public void renderEnd_EmptyQueryIsTagged() {
        ExecuteContext context = mock(ExecuteContext.class);
        String[] batchSql = {""};
        when(context.batchSQL()).thenReturn(batchSql);

        tagger.renderEnd(context);

        assertEquals(comment + " ", batchSql[0]);
    }

    @Test
    public void renderEnd_MultiLineQueryIsTaggedCorrectly() {
        ExecuteContext context = mock(ExecuteContext.class);
        String[] batchSql = {"SELECT x, y\n"
                + "FROM test\n"
                + "WHERE x > y"};
        when(context.batchSQL()).thenReturn(batchSql);

        tagger.renderEnd(context);

        assertEquals("SELECT " + comment + " x, y\n"
                        + "FROM test\n"
                        + "WHERE x > y",
                batchSql[0]);
    }

    @Test
    public void renderEnd_SkipNullStatement() {
        ExecuteContext context = mock(ExecuteContext.class);
        String[] batchSql = {null};
        when(context.batchSQL()).thenReturn(batchSql);

        tagger.renderEnd(context);

        assertNull(batchSql[0]);
    }

    @Test
    public void renderEnd_SkipAlreadyProcessedStatement() {
        ExecuteContext context = mock(ExecuteContext.class);
        String[] batchSql = {"SELECT x, y FROM z"};
        when(context.batchSQL()).thenReturn(batchSql);

        tagger.renderEnd(context);
        tagger.renderEnd(context);

        assertThat(batchSql[0], equalTo("SELECT " + comment + " x, y FROM z"));
    }
}
