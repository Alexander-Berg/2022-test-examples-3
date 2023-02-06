package ru.yandex.market.mboc.common.erp;

import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.erp.document.ErpDocumentExporterService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.misc.thread.ThreadUtils;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("checkstyle:MagicNumber")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ErpDocumentExporterServiceTest extends MdmBaseIntegrationTestClass {

    private static final long SEED = 1234;

    private static final int SLEEP_TIME = 1000;

    @Autowired
    private ErpDocumentExporterService erpDocumentExporterService;

    @Autowired
    private QualityDocumentRepository qualityDocumentRepository;

    @Autowired
    @Qualifier("erpJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private EnhancedRandom random;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void whenRepeatExportShouldExportOnlyNewDocument() {
        QualityDocument document = addNewDocument();
        erpDocumentExporterService.exportDocuments();
        List<Map<String, Object>> firstExport = getErpRows();
        assertEquals(1, firstExport.size());

        // check data correction
        assertSoftly(s -> {
            var assertThat = s.assertThat(firstExport);

            assertThat.extracting(r -> r.get("cert_id")).containsOnly(document.getId());
            assertThat.extracting(r -> r.get("type")).containsOnly(Long.valueOf(document.getType().getId()));
            assertThat.extracting(r -> r.get("reg_number")).containsOnly(document.getRegistrationNumber());
            assertThat.extracting(r -> r.get("start_date")).containsOnly(Date.valueOf(document.getStartDate()));
            assertThat.extracting(r -> r.get("end_date")).containsOnly(Date.valueOf(document.getEndDate()));
            assertThat.extracting(r -> r.get("picture_url")).containsOnly(document.getPictures().stream()
                .collect(Collectors.joining(",")));
        });

        // time goes, nothing change
        ThreadUtils.sleep(SLEEP_TIME);

        erpDocumentExporterService.exportDocuments();
        List<Map<String, Object>> secondExport = getErpRows();
        assertEquals(1, secondExport.size());

        // and now add new document
        ThreadUtils.sleep(SLEEP_TIME);
        addNewDocument();

        erpDocumentExporterService.exportDocuments();
        List<Map<String, Object>> thirdExport = getErpRows();
        assertEquals(2, thirdExport.size());
    }

    @Test
    public void whenDocumentDeletedShouldExportItAnyway() {
        addNewDocument();
        QualityDocument deletedDocument = addDeletedDocument();
        assertEquals(true, deletedDocument.getMetadata().isDeleted());

        erpDocumentExporterService.exportDocuments();
        List<Map<String, Object>> fourthExport = getErpRows();
        assertEquals(2, fourthExport.size());
    }

    @Test
    public void whenNoPicturesShouldExportCorrectly() {
        QualityDocument document = addNewDocument();
        document.setPictures(Collections.emptyList());
        qualityDocumentRepository.insertOrUpdate(document);
        erpDocumentExporterService.exportDocuments();
        List<Map<String, Object>> noPicturesExport = getErpRows();
        assertEquals(1, noPicturesExport.size());
        assertSoftly(s -> {
            var assertThat = s.assertThat(noPicturesExport);
            assertThat.extracting(r -> r.get("picture_url")).containsOnly("");
        });
    }

    @Test
    public void whenManyPicturesShouldExportOnlyEight() {
        List<String> eightPictures = Arrays.asList(
            "one.jpg",
            "two.jpg",
            "three.jpg",
            "four.jpg",
            "five.jpg",
            "six.jpg",
            "seven.jpg",
            "eight.jpg"
        );
        QualityDocument document = addNewDocument();
        document.setPictures(eightPictures);
        document.addPicture("nine.jpg");
        document.addPicture("ten.jpg");

        qualityDocumentRepository.insertOrUpdate(document);
        erpDocumentExporterService.exportDocuments();
        List<Map<String, Object>> tenPicturesExport = getErpRows();
        assertEquals(1, tenPicturesExport.size());
        assertSoftly(s -> {
            var assertThat = s.assertThat(tenPicturesExport);
            assertThat.extracting(r -> r.get("picture_url")).containsOnly(String.join(",", eightPictures));
        });
    }

    private QualityDocument addNewDocument() {
        QualityDocument document = TestDataUtils.generateCorrectDocument(random);
        document.getMetadata().setLastUpdateDate(DateTimeUtils.dateTimeNow());
        qualityDocumentRepository.insert(document);
        return document;
    }

    private QualityDocument addDeletedDocument() {
        QualityDocument document = TestDataUtils.generateCorrectDocument(random);
        document.getMetadata().setDeleted(true);
        document.getMetadata().setLastUpdateDate(DateTimeUtils.dateTimeNow());
        qualityDocumentRepository.insert(document);
        return document;
    }

    private List<Map<String, Object>> getErpRows() {
        return jdbcTemplate.queryForList("SELECT * FROM MBOCertificateIN");
    }
}
