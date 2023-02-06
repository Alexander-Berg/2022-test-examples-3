package ru.yandex.market.tsum.trace;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.trace.model.RequestType;
import ru.yandex.market.tsum.trace.model.Trace;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 05.09.16
 */
@SuppressWarnings("checkstyle:magicnumber")
public class TraceTest {
    @Test
    public void getId() throws Exception {
        Trace trace = new Trace();

        trace.setIdMs(1473065879452L);
        trace.setIdHash("4a83873768237497b45ed43b576a1ff0");
        trace.setIdSeq(new int[]{2, 4, 3});

        Assert.assertEquals("1473065879452/4a83873768237497b45ed43b576a1ff0/2/4/3", trace.getId());
    }

    @Test
    public void testSorting() {

        Trace trace0 = createTrace(1, 10, 20, new int[]{}, RequestType.IN);
        Trace trace1 = createTrace(1, 5, 15, new int[]{1}, RequestType.IN);
        Trace trace2 = createTrace(1, 7, 17, new int[]{1}, RequestType.IN);
        Trace trace3 = createTrace(1, 10, 25, new int[]{1}, RequestType.IN);
        Trace trace4 = createTrace(1, 10, 15, new int[]{1, 1}, RequestType.IN);
        Trace trace4_1 = createTrace(1, 11, 20, new int[]{1, 1}, RequestType.IN);
        Trace trace4_2 = createTrace(1, 11, 15, new int[]{1, 1}, RequestType.IN);
        Trace trace5 = createTrace(1, 12, 15, new int[]{2}, RequestType.IN);

        Trace trace6 = createTrace(1, 5, 15, new int[]{2, 3}, RequestType.PROXY);
        Trace trace7 = createTrace(1, 5, 15, new int[]{2, 3}, RequestType.IN);
        Trace trace8 = createTrace(1, 6, 15, new int[]{2, 3}, RequestType.OUT);

        Trace trace9 = createTrace(2, 11, 15, new int[]{}, RequestType.IN);
        Trace trace10 = createTrace(3, 12, 15, new int[]{}, RequestType.IN);

        List<Trace> traces = new ArrayList<>(Arrays.asList(
            trace4_2, trace4_1, trace3, trace9, trace4, trace10, trace2, trace5, trace7, trace8, trace6,
            trace0, trace1
        ));

        List<Trace> expectedTraces = new ArrayList<>(Arrays.asList(
            trace0, trace1, trace2, trace3, trace4, trace4_1, trace4_2, trace5, trace6, trace7, trace8, trace9,
            trace10
        ));

        Collections.sort(traces);
        //output https://paste.yandex-team.ru/1035238

        Assert.assertEquals(traces, expectedTraces);
    }

    private Trace createTrace(long idMs, long start, long end, int[] idSeq, RequestType requestType) {
        Trace trace = new Trace();
        trace.setIdMs(idMs);
        trace.setDate(new Date(1));
        trace.setIdSeq(idSeq);
        trace.setStartTimeMs(start);
        trace.setEndTimeMs(end);
        trace.setType(requestType);
        return trace;
    }
}
