package ru.yandex.market.supportwizard.controller;

import java.io.IOException;

import net.javacrumbs.jsonunit.core.Configuration;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
import ru.yandex.market.supportwizard.service.build.BuildLogEntryConsumer;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BuildStatControllerTest extends BaseFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BuildLogEntryConsumer buildLogEntryConsumer;

    @Test
    void testAddBuildStats() throws Exception {
        var logCaptor = ArgumentCaptor.forClass(String.class);
        var body = readFile("build-stat.json");

        mockMvc.perform(
                post("/api/add-build-stats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andExpect(status().isOk());

        verify(buildLogEntryConsumer, times(1)).consume(logCaptor.capture());

        var logEntry = logCaptor.getValue();

        assertFalse(logEntry.contains("\n"), "Log entry contains new lines: [" + logEntry + "]");

        var expectedLogEntry = readFile("expected-log-entry.json");
        assertJsonEquals(expectedLogEntry, logEntry, Configuration.empty().whenIgnoringPaths("timestamp"));
    }

    private static String readFile(String fileName) throws IOException {
        try (var is = new ClassPathResource(fileName, BuildStatControllerTest.class).getInputStream()) {
            return IOUtils.toString(is);
        }
    }
}
