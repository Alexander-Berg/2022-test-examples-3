package ru.yandex.market.mboc.common.masterdata.repository.document;

import java.text.SimpleDateFormat;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

@SuppressWarnings("checkstyle:magicNumber")
public class DateDeserializerTest extends MdmBaseIntegrationTestClass {

    private static final int SEED = 256;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    QualityDocumentRepositoryImpl qualityDocumentRepository;

    private EnhancedRandom defaultRandom;

    @Before
    public void setUp() throws Exception {
        defaultRandom = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void whenUseDifferentDataFormatShouldGenerateDifferentJson() {
        QualityDocument doc = generateDocument(1);
        String listDate = generateMetadataJsonWithData(doc, false);
        String stringDate = generateMetadataJsonWithData(doc, true);

        Assertions.assertThat(listDate).isNotEqualTo(stringDate);
    }

    @Test
    public void whenDatePresentAsArrayShouldLoadDocumentCorrectly() {
        QualityDocument doc = generateDocument(2);
        doc.setRegistrationNumber("Array Date");

        String json = generateMetadataJsonWithData(doc, false);

        jdbcTemplate.execute(insertDocument(doc, json));
        List<QualityDocument> docs = qualityDocumentRepository.findBy(new DocumentFilter()
            .addRegistrationNumber(doc.getRegistrationNumber()));
        Assertions.assertThat(docs.size()).isEqualTo(1);
        QualityDocument loadedDoc = docs.get(0);
        Assertions.assertThat(doc.getMetadata().getLastUpdateDate())
            .isEqualTo(loadedDoc.getMetadata().getLastUpdateDate());
    }

    @Test
    public void whenDatePresentAsStringShouldLoadDocumentCorrectly() {
        QualityDocument doc = generateDocument(3);
        doc.setRegistrationNumber("String Date");

        String json = generateMetadataJsonWithData(doc, true);
        jdbcTemplate.execute(insertDocument(doc, json));

        List<QualityDocument> docs = qualityDocumentRepository.findBy(new DocumentFilter()
            .addRegistrationNumber(doc.getRegistrationNumber()));
        Assertions.assertThat(docs.size()).isEqualTo(1);
        QualityDocument loadedDoc = docs.get(0);
        Assertions.assertThat(doc.getMetadata().getLastUpdateDate())
            .isEqualTo(loadedDoc.getMetadata().getLastUpdateDate());
    }

    private String insertDocument(QualityDocument document, String metadata) {
        return "INSERT INTO mdm.quality_document " +
            "(id, document_type, reg_number, start_date, end_date, picture, metadata) \n" +
            "VALUES ('" + document.getId() + "', '" + document.getType() + "', '" +
            document.getRegistrationNumber() + "', '" + document.getStartDate() + "', '" +
            document.getEndDate() + "', '" + document.getPictures() + "', '" + metadata + "')";
    }

    private QualityDocument generateDocument(int id) {
        QualityDocument document = TestDataUtils.generateDocument(defaultRandom);
        document.setId(id);
        document.getMetadata().setLastUpdateDate(DateTimeUtils.dateTimeNow());
        return document;
    }

    @SuppressWarnings("checkstyle:linelength")
    private String generateMetadataJsonWithData(QualityDocument document, Boolean useDateFormat) {
        try {
            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            if (useDateFormat) {
                mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
            }

            return mapper.writeValueAsString(document.getMetadata());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Error generating json", ex);
        }
    }
}
