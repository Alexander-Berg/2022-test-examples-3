package ru.yandex.market.mboc.app.proto.mdm;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.app.proto.MbocCommonMessageUtils;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentOfferRelation;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mdm.http.MdmDocument;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class DocumentResponseUtilTest {

    private static final Long SEED = 1899L;
    private EnhancedRandom random;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(SEED);
    }

    private QualityDocument generateMinimalDocument() {
        EnhancedRandom enhancedRandom = random;
        return new QualityDocument()
            .setId(enhancedRandom.nextLong())
            .setRegistrationNumber(enhancedRandom.nextObject(String.class))
            .setType(enhancedRandom.nextObject(QualityDocument.QualityDocumentType.class));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void whenFindDocumentsResponseCreatedItShouldSameAsDirectBuilderCalls() {
        QualityDocument fullDocument = TestDataUtils.generateFullDocument(random);
        QualityDocument minimalDocument = generateMinimalDocument();
        MdmDocument.FindDocumentsResponse converted = DocumentResponseUtil
            .createFindDocumentsResponse(Arrays.asList(fullDocument, minimalDocument), Optional.empty());

        assertSoftly(softly -> {
            softly.assertThat(converted.getStatus()).isEqualTo(MdmDocument.FindDocumentsResponse.Status.OK);
            softly.assertThat(converted.hasStatusMessage()).isFalse();
            softly.assertThat(converted.hasNextOffsetKey()).isFalse();
            softly.assertThat(converted.getDocumentList()).containsExactly(
                MdmDocument.Document.newBuilder()
                    .setRegistrationNumber(fullDocument.getRegistrationNumber())
                    .setType(MdmDocument.Document.DocumentType.valueOf(fullDocument.getType().name()))
                    .setStartDate(fullDocument.getStartDate().toEpochDay())
                    .setEndDate(fullDocument.getEndDate().toEpochDay())
                    .addAllPicture(fullDocument.getPictures())
                    .setSerialNumber(fullDocument.getSerialNumber())
                    .addAllCustomsCommodityCodes(fullDocument.getCustomsCommodityCodes())
                    .setRequirements(fullDocument.getRequirements())
                    .setCertificationOrgRegNumber(fullDocument.getCertificationOrgRegNumber())
                    .setMetadata(MdmDocument.Document.Metadata.newBuilder()
                        .setCreatedBy(String.valueOf(fullDocument.getMetadata().getCreatedBy()))
                        .setSource(MdmDocument.Document.Metadata.Source.valueOf(
                            fullDocument.getMetadata().getSource().name()))
                        .build())
                    .build(),
                MdmDocument.Document.newBuilder()
                    .setRegistrationNumber(minimalDocument.getRegistrationNumber())
                    .setType(MdmDocument.Document.DocumentType.valueOf(minimalDocument.getType().name()))
                    .build()
            );
        });
    }

    @Test
    public void whenFindDocumentsResponseOffsetKeyIsEmptyIdShouldNotSetNextOffsetKey() {
        MdmDocument.FindDocumentsResponse converted = DocumentResponseUtil
            .createFindDocumentsResponse(Collections.emptyList(), Optional.empty());
        Assertions.assertThat(converted.hasNextOffsetKey()).isFalse();
    }

    @Test
    public void whenFindDocumentsResponseOffsetKeyIsNotEmptyShouldSetNextOffsetKey() {
        MdmDocument.FindDocumentsResponse converted = DocumentResponseUtil
            .createFindDocumentsResponse(Collections.emptyList(), Optional.of(1L));
        assertSoftly(softly -> {
            softly.assertThat(converted.hasNextOffsetKey()).isTrue();
            softly.assertThat(converted.getNextOffsetKey())
                .isEqualTo(String.valueOf(1L));
        });
    }

    @Test
    public void whenFindDocumentsErrorResponseCreatedItShouldHaveCorrectStatusAndMessage() {
        assertFindDocumentsErrorResponse(MbocErrors.get().protoUnknownError("Some message"));
        assertFindDocumentsErrorResponse(MbocErrors.get().qdSupplierDocumentsNullSupplierId());
    }

    private void assertFindDocumentsErrorResponse(ErrorInfo errorInfo) {
        MdmDocument.FindDocumentsResponse errorResponse = DocumentResponseUtil
            .createFindDocumentsErrorResponse(errorInfo);
        assertSoftly(softly -> {
            softly.assertThat(errorResponse.hasNextOffsetKey()).isFalse();
            softly.assertThat(errorResponse.getDocumentList()).isEmpty();
            softly.assertThat(errorResponse.getStatus())
                .isEqualTo(MdmDocument.FindDocumentsResponse.Status.ERROR);
            softly.assertThat(errorResponse.getStatusMessage())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(errorInfo));
        });
    }

    @Test
    public void whenAddDocumentsErrorResponseCreatedItShouldHaveCorrectStatusAndMessage() {
        assertAddDocumentsErrorResponse(MbocErrors.get().qdSupplierDocumentsNullSupplierId());
        assertAddDocumentsErrorResponse(MbocErrors.get().qdAddSupplierDocumentsEmptyDocumentList());
    }

    private void assertAddDocumentsErrorResponse(ErrorInfo errorInfo) {
        MdmDocument.AddDocumentsResponse errorResponse = DocumentResponseUtil
            .createAddDocumentsErrorResponse(errorInfo);
        assertSoftly(softly -> {
            softly.assertThat(errorResponse.getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);
            softly.assertThat(errorResponse.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(errorInfo));
        });
    }

    @Test
    public void whenCreateAddDocumentsErrorDocResponseShouldHaveCorrectStatusAndMessage() {
        ErrorInfo errorInfo1 = MbocErrors.get().protoUnknownError("Error 1");
        ErrorInfo errorInfo2 = MbocErrors.get().protoUnknownError("Error 2");
        MdmDocument.AddDocumentsResponse.DocumentResponse errorResponse = DocumentResponseUtil
            .createAddDocumentsErrorDocResponse(Arrays.asList(errorInfo1, errorInfo2));
        assertSoftly(softly -> {
            softly.assertThat(errorResponse.getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);
            softly.assertThat(errorResponse.getErrorList())
                .containsExactly(
                    MbocCommonMessageUtils.errorInfoToMessage(errorInfo1),
                    MbocCommonMessageUtils.errorInfoToMessage(errorInfo2));
        });
    }

    @Test
    public void whenCreateAddDocumentRelationsErrorResponseShouldHaveCorrectStatusAndMessage() {
        ErrorInfo errorInfo = MbocErrors.get().protoUnknownError("Error");
        MdmDocument.AddDocumentRelationsResponse errorResponse = DocumentResponseUtil
            .createAddDocumentRelationsErrorResponse(errorInfo);
        assertSoftly(softly -> {
            softly.assertThat(errorResponse.getStatus())
                .isEqualTo(MdmDocument.AddDocumentRelationsResponse.Status.ERROR);
            softly.assertThat(errorResponse.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(errorInfo));
        });
    }

    @Test
    public void whenCreateAddDocumentRelationsErrorRelationResponseShouldHaveCorrectStatusAndMessage() {
        ErrorInfo errorInfo = MbocErrors.get().protoUnknownError("Error");
        MdmDocument.DocumentOfferRelation relation = MdmDocument.DocumentOfferRelation.newBuilder()
            .setRegistrationNumber("reg_number")
            .setSupplierId(1)
            .setShopSku("shop_sku")
            .build();
        MdmDocument.AddDocumentRelationsResponse.DocumentRelationAddition errorResponse =
            DocumentResponseUtil.createAddDocumentRelationsErrorRelationResponse(relation, errorInfo);
        assertSoftly(softly -> {
            softly.assertThat(errorResponse.getStatus())
                .isEqualTo(MdmDocument.AddDocumentRelationsResponse.Status.ERROR);
            softly.assertThat(errorResponse.getOfferRelation())
                .isEqualTo(relation);
            softly.assertThat(errorResponse.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(errorInfo));
        });
    }

    @Test
    public void whenFindDocumentRelationsErrorResponseShouldHaveCorrectStatusAndMessage() {
        ErrorInfo errorInfo = MbocErrors.get().protoUnknownError("Error");
        MdmDocument.FindSupplierDocumentRelationsResponse errorResponse =
            DocumentResponseUtil.createFindDocumentRelationsErrorResponse(errorInfo);
        assertSoftly(softly -> {
            softly.assertThat(errorResponse.getStatus())
                .isEqualTo(MdmDocument.FindSupplierDocumentRelationsResponse.Status.ERROR);
            softly.assertThat(errorResponse.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(errorInfo));
        });
    }

    @Test
    public void whenFindDocumentRelationsResponseOffsetKeyIsNotEmptyShouldSetNextOffsetKey() {
        MdmDocument.FindSupplierDocumentRelationsResponse converted = DocumentResponseUtil
            .createFindDocumentRelationsResponse(1, "reg",
                Collections.emptyList(), Optional.of("offset"));
        assertSoftly(softly -> {
            softly.assertThat(converted.hasNextOffsetKey()).isTrue();
            softly.assertThat(converted.getNextOffsetKey()).isEqualTo("offset");
        });
    }

    @Test
    public void whenCreateFindDocumentRelationsOKResponseShouldHaveCorrectStatusAndMessage() {
        int documentId = 1;
        int supplierId = 2;
        String regNumber = "regNumber";
        String shopSku1 = "shopSku1";
        String shopSku2 = "shopSku2";
        List<DocumentOfferRelation> relations = Arrays.asList(
            DocumentOfferRelation.from(new ShopSkuKey(supplierId, shopSku1), new QualityDocument().setId(documentId)),
            DocumentOfferRelation.from(new ShopSkuKey(supplierId, shopSku2), new QualityDocument().setId(documentId)));

        MdmDocument.FindSupplierDocumentRelationsResponse response =
            DocumentResponseUtil.createFindDocumentRelationsResponse(supplierId,
                regNumber, relations, Optional.empty());
        assertSoftly(softly -> {
            softly.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.FindSupplierDocumentRelationsResponse.Status.OK);
            softly.assertThat(response.hasNextOffsetKey()).isFalse();
            MdmDocument.FindSupplierDocumentRelationsResponse.SupplierOfferRelations offerRelations =
                response.getOfferRelations();
            softly.assertThat(offerRelations.getRegistrationNumber()).isEqualTo(regNumber);
            softly.assertThat(offerRelations.getSupplierId()).isEqualTo(supplierId);
            softly.assertThat(offerRelations.getShopSkuList()).containsExactly(shopSku1, shopSku2);
        });
    }

    @Test
    public void whenCreateRemoveDocumentOfferRelationsResponseErrorResponseShouldHaveCorrectStatusAndMessage() {
        ErrorInfo errorInfo = MbocErrors.get().protoUnknownError("Error");
        MdmDocument.RemoveDocumentOfferRelationsResponse errorResponse = DocumentResponseUtil
            .createRemoveDocumentRelationsErrorResponse(errorInfo);
        assertSoftly(softly -> {
            softly.assertThat(errorResponse.getStatus())
                .isEqualTo(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.ERROR);
            softly.assertThat(errorResponse.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(errorInfo));
        });
    }

    @Test
    public void whenCreateRemoveDocumentOfferRelationsErrorRelationResponseShouldHaveCorrectStatusAndMessage() {
        ErrorInfo errorInfo = MbocErrors.get().protoUnknownError("Error");
        MdmDocument.DocumentOfferRelation relation =
            MdmDocument.DocumentOfferRelation.newBuilder()
                .setRegistrationNumber("reg_number")
                .setSupplierId(1)
                .setShopSku("shop_sku")
                .build();
        MdmDocument.RemoveDocumentOfferRelationsResponse.RemoveRelationResponse errorResponse =
            DocumentResponseUtil.createRemoveRelationErrorResponse(relation, errorInfo);
        assertSoftly(softly -> {
            softly.assertThat(errorResponse.getStatus())
                .isEqualTo(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.ERROR);
            softly.assertThat(errorResponse.getRelation())
                .isEqualTo(relation);
            softly.assertThat(errorResponse.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(errorInfo));
        });
    }
}
