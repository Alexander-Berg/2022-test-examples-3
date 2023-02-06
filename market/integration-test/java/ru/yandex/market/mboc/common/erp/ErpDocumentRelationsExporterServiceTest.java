package ru.yandex.market.mboc.common.erp;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.erp.document.ErpDocumentRelationsExporterDao;
import ru.yandex.market.mboc.common.erp.document.ErpDocumentRelationsExporterService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentOfferRelation;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepositoryImpl;
import ru.yandex.market.mboc.common.masterdata.services.document.DocumentServiceImpl;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.MboMappingsServiceMock;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.misc.thread.ThreadUtils;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@SuppressWarnings({"checkstyle:magicnumber"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ErpDocumentRelationsExporterServiceTest extends MdmBaseIntegrationTestClass {

    private static final long SEED = 1234;
    private static final int SLEEP_TIME = 1000;
    private static final String REAL_SUPPLIER_ID = "realId";
    @Autowired
    DocumentServiceImpl documentService;
    @Autowired
    QualityDocumentRepositoryImpl qualityDocumentRepository;
    private ErpDocumentRelationsExporterService erpDocumentRelationsExporterService;
    private MboMappingsServiceMock mboMappingsService;

    private SupplierConverterServiceMock supplierConverterService;

    @Autowired
    private ErpDocumentRelationsExporterDao erpDocumentRelationsExporterDao;

    @Autowired
    @Qualifier("erpJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private EnhancedRandom random;
    private QualityDocument document;

    private static ShopSkuKey key(int supplierId, String shopSku) {
        return new ShopSkuKey(supplierId, shopSku);
    }

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(SEED);

        document = addDocument();

        mboMappingsService = new MboMappingsServiceMock();
        supplierConverterService = new SupplierConverterServiceMock();

        erpDocumentRelationsExporterService = new ErpDocumentRelationsExporterService(
                erpDocumentRelationsExporterDao,
                qualityDocumentRepository,
                mboMappingsService,
                new BeruIdMock(supplierConverterService.getBeruId())
        );
    }

    @Test
    public void whenImportingRelationsShouldWriteDataToCorrectColumns() {
        DocumentOfferRelation relation = addRelation(document, true);

        erpDocumentRelationsExporterService.exportDocumentRelations();

        List<Map<String, Object>> export = getErpRows();

        // check data correction
        assertSoftly(softly -> {
            var assertThat = softly.assertThat(export);

            softly.assertThat(export).hasSize(1);

            assertThat.extracting(r -> r.get("cert_id")).containsExactly(relation.getDocumentId());
            assertThat.extracting(r -> r.get("ssku")).containsOnly(relation.getShopSku());
            assertThat.extracting(r -> r.get("deleted")).containsOnly(false);
        });
    }

    @Test
    public void whenRepeatExportShouldExportOnlyNewDocument() {
        SoftAssertions softly = new SoftAssertions();

        DocumentOfferRelation firstRelation = addRelation(document, true);
        erpDocumentRelationsExporterService.exportDocumentRelations();

        List<Map<String, Object>> firstExport = getErpRows();

        softly.assertThat(firstExport).hasSize(1);
        softly.assertThat(firstExport).extracting(r -> r.get("ssku")).containsExactly(firstRelation.getShopSku());

        ThreadUtils.sleep(SLEEP_TIME);

        erpDocumentRelationsExporterService.exportDocumentRelations();

        List<Map<String, Object>> firstExportRepeat = getErpRows();

        softly.assertThat(firstExportRepeat).hasSize(1);
        softly.assertThat(firstExportRepeat).extracting(r -> r.get("ssku")).containsExactly(firstRelation.getShopSku());

        ThreadUtils.sleep(SLEEP_TIME);

        DocumentOfferRelation secondRelation = addRelation(document, true);
        erpDocumentRelationsExporterService.exportDocumentRelations();
        List<Map<String, Object>> secondExport = getErpRows();

        softly.assertThat(secondExport).hasSize(2);
        softly.assertThat(secondExport).extracting(r -> r.get("ssku"))
            .containsExactlyInAnyOrder(
                firstRelation.getShopSku(),
                secondRelation.getShopSku()
            );

        softly.assertAll();
    }

    @Test
    public void whenRelationIsNotTo1PSupplierShouldNotExport() {
        addRelation(document, false);
        erpDocumentRelationsExporterService.exportDocumentRelations();

        List<Map<String, Object>> export = getErpRows();

        assertSoftly(softly -> {
            softly.assertThat(export).hasSize(0);
        });
    }

    private QualityDocument addDocument() {
        QualityDocument doc = TestDataUtils.generateCorrectDocument(random);
        doc.getMetadata().setLastUpdateDate(DateTimeUtils.dateTimeNow());
        return qualityDocumentRepository.insert(doc);
    }

    private DocumentOfferRelation addRelation(QualityDocument document, boolean realSupplier) {
        DocumentOfferRelation relation = generateRelation(document, realSupplier);
        qualityDocumentRepository.addDocumentRelations(Collections.singletonList(relation));
        addMapping(relation.getShopSkuKey());
        return relation;
    }

    private List<Map<String, Object>> getErpRows() {
        return jdbcTemplate.queryForList("SELECT * FROM MBOSKUCertificateIN");
    }

    private DocumentOfferRelation generateRelation(QualityDocument document, boolean realSupplier) {
        ShopSkuKey shopSkuKey = TestDataUtils.generate(ShopSkuKey.class, random);
        if (realSupplier) {
            shopSkuKey = new ShopSkuKey(SupplierConverterServiceMock.BERU_ID,
                REAL_SUPPLIER_ID + "." + shopSkuKey.getShopSku());
        }
        return DocumentOfferRelation.from(shopSkuKey, document);
    }

    private void addMapping(ShopSkuKey shopSkuKey) {
        MboMappings.ApprovedMappingInfo mappingWithExternalKeysInfo = TestDataUtils
            .generateCorrectApprovedMappingInfoBuilder(random)
            .setSupplierId(shopSkuKey.getSupplierId())
            .setShopSku(shopSkuKey.getShopSku())
            .build();
        mboMappingsService.addMapping(mappingWithExternalKeysInfo);
    }
}
