package ru.yandex.market.olap2.ytreflect;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeListNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.market.olap2.load.TestVerticaLoadTask;
import ru.yandex.market.olap2.load.tasks.VerticaLoadTask;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.olap2.util.CommonUtil.containsAny;
import static ru.yandex.market.olap2.util.CommonUtil.firstInSet;

@Log4j2
public class CreateTableLikeYtTest {
    private CreateTableLikeYt createTableLikeYt;


    @Before
    public void initCreateTableLikeYt() {
        YTreeMapNodeImpl col1 = new YTreeMapNodeImpl(null);
        col1.put("name", new YTreeStringNodeImpl("somecol1", null));
        col1.put("sort_order", new YTreeStringNodeImpl("ascending", null));

        YTreeListNodeImpl listNode = new YTreeListNodeImpl(null);
        listNode.add(col1);
        Cypress cypress = mock(Cypress.class);
        when(cypress.get(any(YPath.class))).thenReturn(listNode);


        YtTableService ytTableService = mock(YtTableService.class);
        createTableLikeYt = new CreateTableLikeYt(ytTableService, null);
    }

    @Test
    public void mustCheckSameTypesOk() {
        Map<String, String> verticaColumns = ImmutableMap.of(
            "datetime", "timestamp", // da, my pro эto dogoworileesь
            "somenum_numeric", "numeric", // da, my pro эto dogoworileesь
            "some_date", "date",
            "somecol1", "int"
        );
        Map<String, String> ytColumns = ImmutableMap.of(
            "datetime", "string",
            "somenum_numeric", "string",
            "some_date", "string",
            "somecol1", "int8"
        );
        VerticaLoadTask testVerticaLoadTask = create(false);
        createTableLikeYt.checkSameTypes(testVerticaLoadTask.getTable(), testVerticaLoadTask.toString(), verticaColumns, ytColumns);
    }

    @Test(expected = RuntimeException.class)
    public void mustCheckSameTypesFailOnDifferentTypes() {
        Map<String, String> verticaColumns = ImmutableMap.of(
            "somecol1", "varchar"
        );
        Map<String, String> ytColumns = ImmutableMap.of(
            "somecol1", "int8"
        );
        VerticaLoadTask testVerticaLoadTask = create(false);
        createTableLikeYt.checkSameTypes(testVerticaLoadTask.getTable(), testVerticaLoadTask.toString(), verticaColumns, ytColumns);
    }

    @Test
    public void testFirstInSetOk() {
        assertThat(firstInSet(
            Arrays.asList("a", "b", "c"),
            ImmutableSet.of("b", "c", "z")),
            is("b"));

        assertThat(firstInSet(
            Arrays.asList("a", "b", "c"),
            ImmutableSet.of("a", "b", "c")),
            is("a"));
    }

    @Test(expected = RuntimeException.class)
    public void testFirstInSetFail() {
        firstInSet(
            Arrays.asList("a", "b", "c"),
            ImmutableSet.of("x", "y", "z"));
    }

    @Test
    public void testContainsAny() {
        Set<String> s = ImmutableSet.of("a", "b", "c");
        assertThat(containsAny(s, l("a", "b")), is(true));
        assertThat(containsAny(s, l("b")), is(true));
        assertThat(containsAny(s, l("z")), is(false));
        assertThat(containsAny(s, l("a", "z")), is(true));
        assertThat(containsAny(s, l("z", "a")), is(true));
    }

    private static <T> List<T> l(T... ts) {
        return Arrays.asList(ts);
    }

    private static VerticaLoadTask create(boolean partitioned) {
        return new TestVerticaLoadTask("id1", "//some/yt/path", partitioned ? 201803 : null);
    }
}
