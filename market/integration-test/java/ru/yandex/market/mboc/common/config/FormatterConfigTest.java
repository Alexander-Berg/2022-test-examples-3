package ru.yandex.market.mboc.common.config;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.Formatter;

import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentOfferRelation;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;

public class FormatterConfigTest extends MdmBaseIntegrationTestClass {
    private static final Locale US_LOCALE = Locale.US;
    private static final long SEED = 0;

    @Autowired
    private Formatter<LocalDate> localDateFormatter;

    @Autowired
    private Formatter<DocumentOfferRelation> documentOfferRelationCollectionFormatter;

    @Autowired
    private Formatter<QualityDocument> qualityDocumentFormatter;

    @Autowired
    private ObjectMapper objectMapper;

    private static DocumentOfferRelation generateNextRelation(long seed) {
        return TestDataUtils.defaultRandom(seed)
            .nextObject(DocumentOfferRelation.class, "modifiedTimestamp", "shopSkuKey");
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testLocalDateFormatter() {
        final String expectedText = "11.02.1964";

        LocalDate expectedLocalDate = LocalDate.of(1964, 2, 11);
        String actualText = localDateFormatter.print(expectedLocalDate, US_LOCALE);
        Assertions.assertThat(actualText)
            .isEqualTo(expectedText);

        String print = localDateFormatter.print(expectedLocalDate, US_LOCALE);
        Assertions.assertThat(print)
            .isEqualTo(expectedText);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRelationParser() throws ParseException, IOException {
        DocumentOfferRelation relation0 = generateNextRelation(0);
        relation0.setSupplierId(863873);
        relation0.setShopSku("YfkhveI");
        relation0.setDocumentId(837778L);

        DocumentOfferRelation relation1 = generateNextRelation(1);
        relation1.setSupplierId(149663);
        relation1.setShopSku("NbizRGdzIebs");
        relation1.setDocumentId(725710L);

        // language=JSON
        String expectedJson0 = "{\"supplierId\":863873,\"shopSku\":\"YfkhveI\",\"documentId\":837778}";
        String expectedJson1 = "{\"supplierId\":149663,\"shopSku\":\"NbizRGdzIebs\",\"documentId\":725710}";

        DocumentOfferRelation actualRelation0 =
            documentOfferRelationCollectionFormatter.parse(expectedJson0, US_LOCALE);
        DocumentOfferRelation actualRelation1 =
            documentOfferRelationCollectionFormatter.parse(expectedJson1, US_LOCALE);
        Assertions.assertThat(actualRelation0).isEqualTo(relation0);
        Assertions.assertThat(actualRelation1).isEqualTo(relation1);

        String actualJson0 = convertToJson(relation0);
        String actualJson1 = convertToJson(relation1);
        Assertions.assertThat(actualJson0).isEqualTo(expectedJson0);
        Assertions.assertThat(actualJson1).isEqualTo(expectedJson1);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testQualityDocumentParser() throws ParseException, IOException {
        // language=JSON
        final String expectedJson = "{" +
            "\"id\":837778," +
            "\"type\":\"CERTIFICATE_OF_CONFORMITY\"," +
            "\"registrationNumber\":\"YfkhveI\"," +
            "\"certificationOrgRegNumber\":\"vbTNzcsNPcyqJ\"," +
            "\"startDate\":\"2015-03-18\"," +
            "\"endDate\":\"2025-11-12\"," +
            "\"pictures\":[]," +
            "\"serialNumber\":\"osHTm\"," +
            "\"customsCommodityCodes\":[]," +
            "\"requirements\":\"ca\"," +
            "\"metadata\":" +
            "{\"source\":null,\"createdBy\":null,\"deleted\":false,\"deleteDate\":null,\"lastUpdateDate\":null}" +
            "}";

        EnhancedRandom defaultRandom = TestDataUtils.qualityDocumentsRandom(SEED);
        QualityDocument expectedQualityDocument = TestDataUtils.generate(QualityDocument.class, defaultRandom);

        expectedQualityDocument.setId(837778);
        expectedQualityDocument.setType(QualityDocument.QualityDocumentType.CERTIFICATE_OF_CONFORMITY);
        expectedQualityDocument.setRegistrationNumber("YfkhveI");
        expectedQualityDocument.setCertificationOrgRegNumber("vbTNzcsNPcyqJ");
        expectedQualityDocument.setStartDate(LocalDate.parse("2015-03-18"));
        expectedQualityDocument.setEndDate(LocalDate.parse("2025-11-12"));
        expectedQualityDocument.setPictures(new ArrayList<>());
        expectedQualityDocument.setSerialNumber("osHTm");
        expectedQualityDocument.setRequirements("ca");
        expectedQualityDocument.setMetadata(new QualityDocument.Metadata());

        QualityDocument actualQualityDocument = qualityDocumentFormatter.parse(expectedJson, US_LOCALE);
        Assertions.assertThat(actualQualityDocument)
            .isEqualTo(expectedQualityDocument);

        String actualJson = convertToJson(expectedQualityDocument);
        Assertions.assertThat(actualJson)
            .isEqualTo(expectedJson);
    }

    private String convertToJson(Object object) throws IOException {
        try (StringWriter stringWriter = new StringWriter()) {
            objectMapper.writeValue(stringWriter, object);
            return stringWriter.toString();
        }
    }
}
