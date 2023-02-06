package ru.yandex.market.core.expimp.storage.export.processor;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.expimp.storage.export.processor.common.UniquePostProcessor;
import ru.yandex.market.core.expimp.storage.export.processor.common.UniquePostProcessorData;
import ru.yandex.market.core.expimp.storage.export.processor.common.UniqueRowProcessor;

/**
 * Unit-тесты для {@link UniqueRowProcessor}.
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class UniqueRowProcessorTest {

    private RowProcessor proc;
    private UniquePostProcessor postProc;
    private ResultSetMetaData meta;

    @Before
    public void setUp() throws Exception {
        proc = new UniqueRowProcessor(Arrays.asList("id", "name"));
        postProc = new UniquePostProcessor();
        meta = Mockito.mock(ResultSetMetaData.class);
    }

    @Test
    public void woTroublesNotEmpty() throws Exception {
        final List<UniquePostProcessorData> list = new ArrayList<>();
        add(list, proc.process(new RowProcessorContext(meta, row("12", "name12"))));
        add(list, proc.process(new RowProcessorContext(meta, row("13", "name13"))));
        add(list, proc.process(new RowProcessorContext(meta, row("14", "name12"))));
        add(list, proc.process(new RowProcessorContext(meta, row("15", null))));
        Assert.assertNull(postProc.result(list));
    }

    @Test
    public void withOneTroubles() throws Exception {
        final List<UniquePostProcessorData> list = new ArrayList<>();
        add(list, proc.process(new RowProcessorContext(meta, row("12", "name12"))));
        add(list, proc.process(new RowProcessorContext(meta, row("13", "name13"))));
        add(list, proc.process(new RowProcessorContext(meta, row("12", "name12"))));
        add(list, proc.process(new RowProcessorContext(meta, row("15", null))));

        Assert.assertThat(
                postProc.result(list).toString(),
                Matchers.containsString("id:12,name:name12,")
        );
    }

    @Test
    public void withTwoTroubles() throws Exception {
        final List<UniquePostProcessorData> list = new ArrayList<>();
        add(list, proc.process(new RowProcessorContext(meta, row("12", "name12"))));
        add(list, proc.process(new RowProcessorContext(meta, row("13", "name13"))));
        add(list, proc.process(new RowProcessorContext(meta, row("12", "name12"))));
        add(list, proc.process(new RowProcessorContext(meta, row("15", null))));
        add(list, proc.process(new RowProcessorContext(meta, row("15", null))));

        Assert.assertThat(
                postProc.result(list).toString(),
                Matchers.containsString("id:12,name:name12"));
        Assert.assertThat(
                postProc.result(list).toString(),
                Matchers.containsString("id:15")
        );
    }


    private Map<String, Object> row(String id, String name) {
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("other", 5);
        return row;
    }

    private void add(List<UniquePostProcessorData> list, RowProcessorResult result) {
        list.add((UniquePostProcessorData) result.getPostProcessorData());
    }

}
