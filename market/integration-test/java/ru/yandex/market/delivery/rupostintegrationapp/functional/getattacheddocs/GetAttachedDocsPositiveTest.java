package ru.yandex.market.delivery.rupostintegrationapp.functional.getattacheddocs;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import utils.FixtureRepository;

import ru.yandex.market.delivery.rupostintegrationapp.BaseContextualTest;
import ru.yandex.market.delivery.russianpostapiclient.bean.createbatch.Batch;
import ru.yandex.market.delivery.russianpostapiclient.client.RussianPostApiClient;
import ru.yandex.market.delivery.russianpostapiclient.processor.ApiMethodProcessingException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class GetAttachedDocsPositiveTest extends BaseContextualTest {
    private static final String BATCH_NAME = "123";
    private static final String BATCH_DATE = "2017-03-31";

    @MockBean
    private RussianPostApiClient client;

    private MvcResult responseEntity;

    @BeforeEach
    void initClientMock() throws Exception {
        mockGetBatch();
        mock103FormGeneration();
        responseEntity = mockMvc.perform(post("/ds/getAttachedDocs")
            .contentType(MediaType.APPLICATION_XML)
            .content(FixtureRepository.getAttachedDocsValidRequest()))
            .andReturn();
    }

    private void mockGetBatch() throws ApiMethodProcessingException {
        Batch batch = new Batch();
        batch.setBatchName(BATCH_NAME);
        batch.setListNumberDate(BATCH_DATE);
        batch.setListNumber(5);
        given(client.getBatch(BATCH_NAME)).willReturn(batch);
    }

    private void mock103FormGeneration() throws ApiMethodProcessingException {
        given(client.generateF103Form(BATCH_NAME)).willReturn(FixtureRepository.getF103FormSample());
    }

    @Test
    void testGetAttachedResponse() throws UnsupportedEncodingException {
        softly.assertThat(responseEntity.getResponse().getStatus())
            .as("Response status code is NOT OK")
            .isEqualTo(HttpStatus.OK.value());
        assertThat(
            "Response does not contains correct method type",
            responseEntity.getResponse().getContentAsString(),
            containsString("type=\"getAttachedDocs\"")
        );
        assertThat(
            "Response does not contains pdf format flag",
            responseEntity.getResponse().getContentAsString(),
            containsString("<format>1</format>")
        );
        assertThat(
            "Response does not contains isError=false flag",
            responseEntity.getResponse().getContentAsString(),
            containsString("<isError>false</isError>")
        );
    }

    @Test
    @SuppressWarnings("checkstyle:LineLength")
    void testAttachedDocsPdf() throws IOException {
        byte[] pdfData = extractPdfDataFromResponse(responseEntity.getResponse().getContentAsString());
        softly.assertThat(pdfData).as("Found pdf data is null").isNotNull();

        PDDocument pdDocument = PDDocument.load(pdfData);
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String pdfText = pdfStripper.getText(pdDocument);

        assertThat(
            "Result document does not contains batch number",
            pdfText,
            containsString("Партия 247")
        );
        assertThat(
            "Result document does not contains order count",
            pdfText,
            containsString("Общее количество отправлений 2 (два)")
        );
        assertThat(
            "Result document does not contains letter with currently filled fields",
            pdfText,
            containsString("Настоящим уведомляем, что ООО «Яндекс.Маркет» (ОГРН: 1167746491395) уполномочило\n" +
                "ИП «Бондаренко Александр Ярославович» ОГРН: _____________ в лице его представителей на\n" +
                "сдачу в ЦВВП ФГУП «Почта России» отправлений «EMS оптимальное» согласно Списку № 5\n" +
                "(Партия № 123) от 31.03.2017 ф.103.\n" +
                "Сдача отправлений «EMS оптимальное» осуществляется на основании Договора на оказание\n" +
                "услуг по пересылке почтовых отправлений и дополнительных услуг № Ф-011016-087 от 04.10.2016,\n" +
                "заключенного между ООО «Яндекс.Маркет» и ФГУП «Почта России».\n" +
                "Полномочия лиц, осуществляющих непосредственную сдачу отправлений в ЦВВП\n" +
                "ФГУП «Почта России», подтверждаются доверенностью, выданной ИП «Бондаренко Александр\n" +
                "Ярославович» ОГРН: _____________ на их имя в порядке передоверия.\n" +
                "Дата: 31.03.2017")
        );
        assertThat(
            "Result document does not contains letter with correctly formatted date",
            pdfText,
            containsString("Дата: 31.03.2017")
        );
    }

    private byte[] extractPdfDataFromResponse(String response) {
        Pattern pattern = Pattern.compile("<pdf>(\\S+)</pdf>");
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            String foundBase64 = matcher.group(1);
            return Base64.getDecoder().decode(foundBase64);
        } else {
            return null;
        }
    }
}
