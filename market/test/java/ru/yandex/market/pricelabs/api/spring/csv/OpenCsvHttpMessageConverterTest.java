package ru.yandex.market.pricelabs.api.spring.csv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.market.pricelabs.exports.ExporterTestContextCsv;
import ru.yandex.market.pricelabs.exports.params.MediaTypeBuilder;
import ru.yandex.market.pricelabs.generated.server.pub.model.AnalyticsPerOfferResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ExtendWith(MockitoExtension.class)
class OpenCsvHttpMessageConverterTest {

    @Mock
    private HttpServletRequest request;
    private OpenCsvHttpMessageConverter converter;

    static Object[][] scenarios() {
        return new Object[][]{
                {new MediaTypeBuilder()
                        .charset(StandardCharsets.UTF_8)
                        .build(), "api/spring/csv/analytics-per-offer-response.csv"},
                {new MediaTypeBuilder()
                        .charset(StandardCharsets.UTF_8)
                        .delimiter(',')
                        .quote('\'')
                        .build(), "api/spring/csv/analytics-per-offer-response-alter-format.csv"}
        };
    }

    @BeforeEach
    void init() {
        converter = new OpenCsvHttpMessageConverter();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @ParameterizedTest
    @MethodSource("scenarios")
    void testConverter(MediaType mediaType, String expectResource) throws IOException {
        AnalyticsPerOfferResponse r1 = new AnalyticsPerOfferResponse()
                .domain("domain 1")
                .date("2019-03-01")
                .offerId("100")
                .offerName("Оффер")
                .price(99.9);

        AnalyticsPerOfferResponse r2 = new AnalyticsPerOfferResponse()
                .domain("domain 2")
                .date("2019-03-01")
                .offerId("101")
                .offerName("Оффер второй")
                .price(95.9)
                .purchasePrice(90.6);

        var responseList = List.of(r1, r2);
        log.info("Processing mediaType: {}", mediaType);

        assertTrue(converter.canWrite(responseList.getClass(), mediaType));

        // Добавим конфигурацию для экспорта в UTF-8
        var output = new MockHttpOutputMessage();
        output.getHeaders().setContentType(mediaType);

        converter.write(responseList, mediaType, output);

        var context = new ExporterTestContextCsv<>(Objects.requireNonNull(mediaType.getCharset()));
        context.verify(expectResource, output.getBodyAsBytes());
    }

}
