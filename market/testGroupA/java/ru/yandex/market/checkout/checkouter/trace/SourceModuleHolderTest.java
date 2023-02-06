package ru.yandex.market.checkout.checkouter.trace;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import ru.yandex.market.request.trace.Module;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.checkouter.trace.TraceLogHelper.awaitTraceLog;
import static ru.yandex.market.checkout.common.web.CheckoutHttpParameters.SOURCE_MODULE_HEADER;

public class SourceModuleHolderTest extends AbstractTraceLogTestBase {

    @Test
    public void shouldWriteSourceModuleIntoTrace() {
        String module = Module.DELIVERY_MDB.name();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(SOURCE_MODULE_HEADER, module);

        testRestTemplate.exchange("/ping", HttpMethod.GET, new HttpEntity<>(null, httpHeaders), String.class);

        verifyTraceLog(module);
    }

    @Test
    public void shouldWriteSourceModuleViaClient() {
        checkouterAPI.ping();

        verifyTraceLog(Module.CHECKOUTER.name());
    }

    private void verifyTraceLog(String expected) {
        List<Map<String, String>> events = Collections.emptyList();

        events = awaitTraceLog(inMemoryAppender, events);
        assertThat("No events received", events, CoreMatchers.notNullValue());

        Map<String, String> inRecord = events.stream()
                .filter(it -> "IN".equals(it.get("type")))
                .findFirst()
                .get();

        Assertions.assertEquals(expected, inRecord.get("kv.sourceModule"));
    }
}
