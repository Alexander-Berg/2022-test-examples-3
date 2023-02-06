package ru.yandex.market.wms.servicebus.api.internal.wms.server.controller;

import java.time.LocalDateTime;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.common.spring.dto.subs.SubtitlesDto;
import ru.yandex.market.wms.common.spring.dto.subs.receiving.ReceiptFitTagDto;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.async.service.SubtitlesService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SubtitlesControllerTest extends IntegrationTest {

    @MockBean
    @Autowired
    private JmsTemplate defaultJmsTemplate;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(defaultJmsTemplate);
    }

    @Test
    public void pushSubtitles() throws Exception {
        ReceiptFitTagDto receiptFitTagDto = ReceiptFitTagDto.builder()
                .receiptKey("12345678")
                .lot("001")
                .sku("ROV123")
                .uit("470010000000")
                .storer("storer")
                .user("user")
                .location("STAGE01")
                .build();

        SubtitlesDto subtitlesDto = SubtitlesDto.builder()
                .timestamp(LocalDateTime.now())
                .tags(Collections.singletonList(receiptFitTagDto))
                .build();

        String content = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(subtitlesDto);
        mockMvc.perform(post("/subtitles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andExpect(status().is2xxSuccessful());

        // Проверяем, что мы запушили субтитр в очередь
        verify(defaultJmsTemplate, times(1))
                .convertAndSend(eq(SubtitlesService.QUEUE_NAME_TPL), notNull(), notNull());

        // Проверяем, что мы запушили в очередь именно тот субтитр, который был отправлен в запросе
        Invocation invocation = Mockito.mockingDetails(defaultJmsTemplate).getInvocations().stream()
                .filter(inv -> inv.getMethod().getName().equals("convertAndSend"))
                .findFirst().orElseThrow(IllegalStateException::new);

        SubtitlesDto subtitlesDtoReceived = invocation.getArgument(1);
        Assertions.assertEquals(subtitlesDto, subtitlesDtoReceived);
    }
}
