package ru.yandex.market.pricelabs.api.spring.excel;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.market.pricelabs.exports.ExporterTestContextExcel;
import ru.yandex.market.pricelabs.exports.params.ExcelParameters;
import ru.yandex.market.pricelabs.generated.server.pub.model.AnalyticsPerOfferResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ExcelHttpMessageConverterTest {

    @Mock
    private HttpServletRequest request;
    private ExcelHttpMessageConverter converter;
    private MediaType mediaType;

    @BeforeEach
    void init() {
        converter = new ExcelHttpMessageConverter();
        mediaType = ExcelParameters.XLSX;
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void testConverter() throws IOException {
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
        assertTrue(converter.canWrite(responseList.getClass(), mediaType));

        var output = new MockHttpOutputMessage();
        converter.write(responseList, mediaType, output);

        var context = new ExporterTestContextExcel<>();
        context.verify("api/spring/excel/analytics-per-offer-response.xlsx", output.getBodyAsBytes());
    }

}
