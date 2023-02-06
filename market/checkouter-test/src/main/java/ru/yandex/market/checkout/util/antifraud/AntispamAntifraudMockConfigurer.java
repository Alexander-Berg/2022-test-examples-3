package ru.yandex.market.checkout.util.antifraud;

import java.io.IOException;
import java.nio.charset.Charset;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.checkout.checkouter.antifraud.entity.AntifraudActionResult;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@TestComponent
public class AntispamAntifraudMockConfigurer {

    @Autowired
    private WireMockServer antispamAntifraudMock;

    private static String getStringBodyFromFile(String fileName, Object... args) {
        try {
            String response = IOUtils.toString(
                    AntispamAntifraudMockConfigurer.class.getResourceAsStream(fileName),
                    Charset.defaultCharset());
            return String.format(response, args);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void mockAntifraud(AntifraudActionResult actionResult) {
        MappingBuilder builder = post(urlEqualTo("/execute?app=verified"))
                .willReturn(ok()
                        .withBody(getStringBodyFromFile("antispamAntifraudResponse.json", actionResult.name()))
                        .withHeader("Content-Type", "application/json"));

        antispamAntifraudMock.stubFor(builder);
    }
}
