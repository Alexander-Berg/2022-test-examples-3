package ru.yandex.market.logistics.werewolf.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.werewolf.model.enums.PageOrientation;
import ru.yandex.market.logistics.werewolf.model.enums.PageSize;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Конвертация html -> pdf")
class ConvertHtmlToPdfTest extends AbstractClientTest {

    @Test
    @DisplayName("Успешная конвертация")
    void convertHtmlToPdf() {
        prepareMockRequest(
            MockRequestUtils.MockRequest.builder()
                .requestMethod(HttpMethod.PUT)
                .header("accept", List.of("application/json; q=0.9", MediaType.APPLICATION_PDF_VALUE))
                .path("document/convertHtmlToPdf")
                .requestContentType(MediaType.TEXT_HTML)
                .responseContentPath("response/app.pdf")
                .responseContentType(MediaType.APPLICATION_PDF)
                .build()
        )
            .andExpect(content().string(extractFileContent("request/app2.html")));

        InputStream is = new ByteArrayInputStream(extractFileContent("request/app2.html").getBytes());

        softly.assertThat(wwClient.convertHtmlToPdf(is, PageSize.A4, PageOrientation.PORTRAIT))
            .isEqualTo(readFileIntoByteArray("response/app.pdf"));
    }
}
