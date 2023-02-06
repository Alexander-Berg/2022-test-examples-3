package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.reporter.ReporterClient;
import ru.yandex.market.wms.servicebus.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

class PutOutboundDocumentsControllerTest extends IntegrationTest {
    @Autowired
    @Qualifier("logistic-api")
    protected XmlMapper xmlMapper;
    @MockBean
    @Autowired
    private ReporterClient reporterClient;

    @Test
    public void putOutboundDocumentsHappy() throws Exception {
        doAnswer(invocation -> {
            String arg0 = invocation.getArgument(0);
            Map<String, String> arg1 = invocation.getArgument(1);
            Instant arg2 = invocation.getArgument(2);

            final DateTimeFormatter formatter = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            Instant expected = Instant.from(formatter.parse("2022-03-28 12:00:00"));

            assertEquals("0005551111", arg0);
            assertEquals(Map.of(
                    "https://link1", "ТОРГ-13",
                    "https://link2", "ТрН",
                    "https://link3", "Документ"), arg1);
            assertEquals(expected, arg2);
            return null;
        }).when(reporterClient).createReportLinksForOrderKey(anyString(), anyMap(), any(Instant.class));

        mockMvc.perform(post("/api/logistic/putOutboundDocuments")
                        .contentType(MediaType.TEXT_XML)
                        .accept(MediaType.TEXT_XML)
                        .content(getFileContent("api/logistics/server/putOutboundDocuments/happy/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .xml(getFileContent("api/logistics/server/putOutboundDocuments/happy/response.xml")))
                .andReturn();

        verify(reporterClient, times(1))
                .createReportLinksForOrderKey(anyString(), anyMap(), any(Instant.class));
    }
}
