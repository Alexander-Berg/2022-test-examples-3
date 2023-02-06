package ru.yandex.travel.orders.services;

import java.time.Duration;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.orders.services.pdfgenerator.PdfGeneratorConfigurationProperties;
import ru.yandex.travel.orders.services.pdfgenerator.PdfGeneratorService;
import ru.yandex.travel.orders.services.pdfgenerator.model.PdfGenerateHotelsVoucherRequest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
@ActiveProfiles("test")
public class PdfGeneratorServiceTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options()
            .dynamicPort()
            .usingFilesUnderClasspath("fixtures/pdfgenerator"));

    @MockBean
    private PdfGeneratorConfigurationProperties pdfGeneratorConfigurationProperties;

    @Autowired
    PdfGeneratorService pdfGeneratorService;

    @Before
    public void before() {
        when(pdfGeneratorConfigurationProperties.getBaseUrl()).thenReturn("http://localhost:" + wireMockRule.port());
        when(pdfGeneratorConfigurationProperties.getLongRequestTimeout()).thenReturn(Duration.ofSeconds(60));
        when(pdfGeneratorConfigurationProperties.getLongReadTimeout()).thenReturn(Duration.ofSeconds(60));
        when(pdfGeneratorConfigurationProperties.getQuickRequestTimeout()).thenReturn(Duration.ofSeconds(2));
        when(pdfGeneratorConfigurationProperties.getQuickReadTimeout()).thenReturn(Duration.ofSeconds(2));
    }

    public void testCorrectStartGenerateHotelVoucher() {
        pdfGeneratorService.generateHotelsVoucher(new PdfGenerateHotelsVoucherRequest("c00a32f5-dda2-4750-9dbd-78e5f73113a1", "fileName"));
    }

    @Test
    public void testCheckState() {
        var response = pdfGeneratorService.getState("fileName");
        assertThat(response.getUrl()).isEqualTo("https://s3.mdst.yandex.net/fileName.pdf");
        assertThat(response.getLastModified()).isEqualTo("2022-06-07T07:25:09.278Z");
    }

}
