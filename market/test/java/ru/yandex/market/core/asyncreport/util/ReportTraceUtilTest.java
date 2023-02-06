package ru.yandex.market.core.asyncreport.util;

import java.util.HashMap;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.request.trace.RequestContext;

public class ReportTraceUtilTest {

    @Test
    void testTraceLogString() {
        var map = new HashMap<String, Object>();
        map.put("123", null);
        map.put(null, null);

        Assertions.assertThatNoException()
                .isThrownBy(() -> ReportTraceUtils.logReportGenerationStarted(RequestContext.EMPTY, map));

        var logString = ReportTraceUtils.forReportGenerationStarted(RequestContext.EMPTY, map);
        Assertions.assertThat(logString).contains("kv.123=null").doesNotContain("kv.null");
    }
}
