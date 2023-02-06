package ru.yandex.market.logistics.werewolf.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import ru.yandex.market.logistics.werewolf.AbstractTest;
import ru.yandex.market.logistics.werewolf.dto.document.TemplateEngineInput;
import ru.yandex.market.logistics.werewolf.dto.document.WriterOptions;
import ru.yandex.market.logistics.werewolf.util.HtmlToPdfConverter;
import ru.yandex.market.logistics.werewolf.util.ResponseWriterFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContentInBytes;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

/**
 * Общие тесты не заходят в сам шаблонизатор, за исключением проверки конвертации в PDF (flow проверка).
 */
@Slf4j
abstract class AbstractDocumentGeneratorTest extends AbstractTest {
    protected static final MediaType TEXT_HTML_UTF_8 = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);
    protected static final MediaType APPLICATION_JSON_Q_09 = new MediaType("application", "json", 0.9);
    protected static final String MOCK_PDF_CONTENT = "This is a pdf content";
    private static final String MOCK_HTML_CONTENT = "This is a html content";

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private HtmlToPdfConverter htmlToPdfConverter;

    @Autowired
    private ResponseWriterFactory responseWriterFactory;

    @Test
    @DisplayName("Не найден совместимый тип")
    void generateUnsupportedType() throws Exception {
        perform(
            defaultRequestBodyPath(),
            request -> request.accept(
                MediaType.TEXT_PLAIN,
                MediaType.TEXT_MARKDOWN,
                APPLICATION_JSON_Q_09
            )
        )
            .andExpect(status().isNotAcceptable())
            .andExpect(jsonPath("message")
                .value("Supported media types=[[text/html;charset=UTF-8, application/pdf]],"
                    + " accepted=[text/plain, text/markdown, application/json;q=0.9]"));
    }

    @Test
    @DisplayName("Проверка поддерживаемых типов: HTML")
    void generateHttpSuccess() throws Exception {
        mockResponseWriterFactory(MediaType.TEXT_HTML, MOCK_HTML_CONTENT.getBytes());

        performAndDispatch(
            defaultRequestBodyPath(),
            request -> request.header(
                HttpHeaders.ACCEPT,
                MediaType.APPLICATION_OCTET_STREAM,
                MediaType.TEXT_MARKDOWN,
                MediaType.TEXT_HTML
            )
        )
            .andExpect(status().isOk())
            .andExpect(content().contentType(TEXT_HTML_UTF_8))
            .andExpect(header().string(
                "Content-Disposition",
                String.format("attachment; filename=\"%s.html\"", defaultFilename())
            ))
            .andExpect(content().string(MOCK_HTML_CONTENT));
    }

    @Test
    @DisplayName("Проверка поддерживаемых типов: PDF")
    void generatePdfSuccess() throws Exception {
        mockResponseWriterFactory(MediaType.APPLICATION_PDF, MOCK_PDF_CONTENT.getBytes());

        performAndDispatch(
            defaultRequestBodyPath(),
            request -> request.header(
                HttpHeaders.ACCEPT,
                MediaType.APPLICATION_OCTET_STREAM,
                MediaType.TEXT_MARKDOWN,
                MediaType.APPLICATION_PDF
            )
        )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string(
                "Content-Disposition",
                String.format("attachment; filename=\"%s.pdf\"", defaultFilename())
            ))
            .andExpect(content().string(MOCK_PDF_CONTENT));
    }

    @Test
    @DisplayName("Проверка сортировки типов по q-factor")
    void generateFromMultipleSupportedTypes() throws Exception {
        mockResponseWriterFactory(MediaType.APPLICATION_PDF, MOCK_PDF_CONTENT.getBytes());

        performAndDispatch(
            defaultRequestBodyPath(),
            request -> request.header(
                HttpHeaders.ACCEPT,
                new MediaType(MediaType.TEXT_PLAIN.getType(), MediaType.TEXT_PLAIN.getSubtype(), 1),
                new MediaType(MediaType.TEXT_HTML.getType(), MediaType.TEXT_HTML.getSubtype(), 0.1),
                new MediaType(MediaType.APPLICATION_PDF.getType(), MediaType.APPLICATION_PDF.getSubtype(), 0.9)
            )
        )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string(
                "Content-Disposition",
                String.format("attachment; filename=\"%s.pdf\"", defaultFilename())
            ))
            .andExpect(content().string(MOCK_PDF_CONTENT));
    }

    @Test
    @DisplayName("Проверка корректности вызова pdf-конвертера")
    void generatePdfConverterCalled() throws Exception {
        mockConverterWithChecks(
            extractFileContentInBytes(defaultHtmlResponseBodyPath()),
            defaultWriterOptions(),
            MOCK_PDF_CONTENT.getBytes()
        );

        performAndDispatch(
            defaultRequestBodyPath(),
            request -> request.accept(MediaType.APPLICATION_PDF)
        )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string(
                "Content-Disposition",
                String.format("attachment; filename=\"%s.pdf\"", defaultFilename())
            ))
            .andExpect(content().string(MOCK_PDF_CONTENT));
    }

    @Nonnull
    protected abstract String defaultRequestBodyPath();

    @Nonnull
    protected abstract String defaultHtmlResponseBodyPath();

    @Nonnull
    protected abstract String defaultFilename();

    @Nonnull
    protected abstract String requestPath();

    @Nonnull
    protected abstract WriterOptions defaultWriterOptions();

    @Nonnull
    protected ResultActions perform(
        String requestBodyPath,
        Consumer<MockHttpServletRequestBuilder> requestModifier
    ) throws Exception {
        return performWithBody(extractFileContent(requestBodyPath), requestModifier);
    }

    @Nonnull
    protected ResultActions performWithBody(
        String requestBody,
        Consumer<MockHttpServletRequestBuilder> requestModifier
    ) throws Exception {
        MockHttpServletRequestBuilder request =
            MockMvcRequestBuilders.put(requestPath())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody);
        requestModifier.accept(request);

        return mockMvc.perform(request);
    }

    @Nonnull
    protected ResultActions performAndDispatch(
        String requestBodyPath,
        Consumer<MockHttpServletRequestBuilder> requestModifier
    ) throws Exception {

        MvcResult callbackResult = perform(requestBodyPath, requestModifier)
            .andExpect(status().isOk())
            .andReturn();

        return mockMvc.perform(asyncDispatch(callbackResult));
    }

    protected void mockResponseWriterFactory(MediaType requestedType, byte[] response) {
        doReturn((StreamingResponseBody) out -> out.write(response))
            .when(responseWriterFactory)
            .create(eq(requestedType), any(TemplateEngineInput.class), eq(defaultWriterOptions()));
    }

    protected void mockConverterWithChecks(
        byte[] expectedContent,
        WriterOptions writerOptions,
        byte[] returnContent
    ) throws IOException {
        doAnswer(invocation -> {
            try (InputStream inputStream = invocation.getArgument(0)) {
                softly.assertThat(new String(inputStream.readAllBytes()))
                    .isEqualTo(new String(expectedContent));
            }

            return returnContent;
        })
            .when(htmlToPdfConverter).convert(any(InputStream.class), eq(writerOptions));
    }
    @Nonnull
    protected ResultMatcher fieldError(String field, String message) {
        return jsonPath(
            "message",
            Matchers.equalTo(String.format(
                "Following validation errors occurred:\nField: '%s', message: '%s'",
                field,
                message
            ))
        );
    }
}
