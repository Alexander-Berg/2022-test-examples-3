package ru.yandex.market.mboc.common.erp;

import java.time.LocalDateTime;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.erp.document.ErpDocumentRelationsExporterDao;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentOfferRelation;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepositoryImpl;

public class ErpStatisticsServiceTest extends MdmBaseDbTestClass {
    private ErpDocumentRelationsExporterDao erpDocumentRelationsExporterDao;
    private ErpStatisticsService statisticsService;
    private QualityDocumentRepositoryImpl qualityDocumentRepository;
    private MappingsCacheRepositoryImpl mappingsCacheRepository;
    private EnhancedRandom random;

    @Before
    public void setUp() throws Exception {
        random = TestDataUtils.defaultRandom(170690L);
        erpDocumentRelationsExporterDao = Mockito.mock(ErpDocumentRelationsExporterDao.class);
        Mockito.when(erpDocumentRelationsExporterDao.getLastExportDate())
            .thenReturn(LocalDateTime.of(1999, 11, 25, 2, 0));
        qualityDocumentRepository = new QualityDocumentRepositoryImpl(jdbcTemplate, transactionTemplate);
        mappingsCacheRepository = new MappingsCacheRepositoryImpl(jdbcTemplate, transactionTemplate);
        statisticsService = new ErpStatisticsService(
            jdbcTemplate,
            null,
            erpDocumentRelationsExporterDao,
            new BeruIdMock(1),
            qualityDocumentRepository
        );
    }

    @Test
    public void testGetNumberOfDocumentsForExportApproximation() {
        MappingCacheDao mappingCache1 = new MappingCacheDao().setShopSku("1234");
        MappingCacheDao mappingCache2 = new MappingCacheDao().setShopSku("1235");
        MappingCacheDao mappingCache3 = new MappingCacheDao().setShopSku("1236");
        mappingsCacheRepository.insertOrUpdateAll(List.of(mappingCache1, mappingCache2, mappingCache3));

        QualityDocument qualityDocument1 = TestDataUtils.generateCorrectDocument(random).setId(1L);
        QualityDocument qualityDocument2 = TestDataUtils.generateCorrectDocument(random).setId(2L);
        QualityDocument qualityDocument3 = TestDataUtils.generateCorrectDocument(random).setId(3L);
        QualityDocument qualityDocument4 = TestDataUtils.generateCorrectDocument(random).setId(4L);
        qualityDocumentRepository.insertBatch(List.of(qualityDocument1, qualityDocument2, qualityDocument3,
            qualityDocument4));

        DocumentOfferRelation documentOfferRelation1 = new DocumentOfferRelation(1, "1234", 1L, null);
        DocumentOfferRelation documentOfferRelation2 = new DocumentOfferRelation(1, "1235", 2L, null);
        DocumentOfferRelation documentOfferRelation3 = new DocumentOfferRelation(2, "1236", 3L, null);
        DocumentOfferRelation documentOfferRelation4 = new DocumentOfferRelation(1, "1237", 4L, null);
        qualityDocumentRepository.addDocumentRelations(List.of(documentOfferRelation1, documentOfferRelation2,
            documentOfferRelation3, documentOfferRelation4));

        Assert.assertEquals(2L, statisticsService.getNumberOfDocumentRelationsForExportApproximation());
    }
}
