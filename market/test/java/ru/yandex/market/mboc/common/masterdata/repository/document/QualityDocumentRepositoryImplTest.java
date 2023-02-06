package ru.yandex.market.mboc.common.masterdata.repository.document;

import java.time.LocalDateTime;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentOfferRelation;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;

import static org.assertj.core.api.Assertions.assertThat;

public class QualityDocumentRepositoryImplTest extends MdmBaseDbTestClass {

    private QualityDocumentRepositoryImpl qualityDocumentRepository;
    private EnhancedRandom random;

    @Before
    public void setUp() throws Exception {
        random = TestDataUtils.defaultRandom(170690L);
        qualityDocumentRepository = new QualityDocumentRepositoryImpl(jdbcTemplate, transactionTemplate);
    }

    @Test
    public void testGenerateNumberOfDocumentsQuery() {
        DocumentFilter filter = new DocumentFilter();
        LocalDateTime localDateTime = LocalDateTime.of(1998, 11, 25, 2, 0);
        filter.setModifiedAfter(localDateTime);

        QualityDocument.Metadata metadata = new QualityDocument.Metadata();
        metadata.setLastUpdateDate(LocalDateTime.of(1999, 11, 25, 2, 0));

        QualityDocument qualityDocument1 = TestDataUtils.generateCorrectDocument(random).setId(1L)
            .setMetadata(metadata);
        QualityDocument qualityDocument2 = TestDataUtils.generateCorrectDocument(random).setId(2L)
            .setMetadata(null);
        qualityDocumentRepository.insertBatch(List.of(qualityDocument1, qualityDocument2));

        Assert.assertEquals(1L, qualityDocumentRepository.getNumberOfDocuments(filter));
    }

    @Test
    public void shouldDeleteOldRelationsDuringUpdate() {
        // given
        Long testDocumentID = 1L;
        DocumentOfferRelation relationToDelete =
            new DocumentOfferRelation(1, "1234", testDocumentID, null);
        DocumentOfferRelation relationToLeave =
            new DocumentOfferRelation(1, "1235", testDocumentID, null);
        QualityDocument qualityDocument = TestDataUtils.generateCorrectDocument(random).setId(testDocumentID)
            .setMetadata(new QualityDocument.Metadata());

        qualityDocumentRepository.insert(qualityDocument);
        qualityDocumentRepository.addDocumentRelations(List.of(relationToDelete, relationToLeave));

        // when
        qualityDocumentRepository.updateDocumentRelationsFor(
            List.of(relationToLeave.getShopSkuKey(), relationToDelete.getShopSkuKey()),
            List.of(relationToLeave)
        );

        // then
        var allRelations = qualityDocumentRepository
            .findRelations(new DocumentFilter().addAllDocumentIds(List.of(testDocumentID)));
        assertThat(allRelations).containsExactly(relationToLeave);
    }

    @Test
    public void shouldUpdateOnlyRequestedRelations() {
        // given
        Long testDocumentID = 1L;
        DocumentOfferRelation relationToNotTouch =
            new DocumentOfferRelation(1, "1234", testDocumentID, null);
        DocumentOfferRelation relationToUpdate =
            new DocumentOfferRelation(1, "1235", testDocumentID, null);
        QualityDocument qualityDocument = TestDataUtils.generateCorrectDocument(random).setId(testDocumentID)
            .setMetadata(new QualityDocument.Metadata());

        qualityDocumentRepository.insert(qualityDocument);
        qualityDocumentRepository.addDocumentRelations(List.of(relationToNotTouch, relationToUpdate));

        // when
        qualityDocumentRepository.updateDocumentRelationsFor(
            List.of(relationToUpdate.getShopSkuKey()),
            List.of(relationToUpdate)
        );

        // then
        var allRelations = qualityDocumentRepository
            .findRelations(new DocumentFilter().addAllDocumentIds(List.of(testDocumentID)));
        assertThat(allRelations).containsExactlyInAnyOrder(relationToUpdate, relationToNotTouch);
    }

}
