package ru.yandex.market.mboc.app.proto.mdm;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.app.proto.MbocCommonMessageUtils;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentProtoConverter;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mdm.http.MdmDocument;
import ru.yandex.market.mdm.http.MdmDocument.AddSupplierDocumentsRequest.DocumentAddition;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class DocumentRequestValidatorTest {
    private static final long SEED = 20181202L;
    private static final String CORRECT_SCAN_FILE_URL = "http://lolkek.jpeg";
    private EnhancedRandom random;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(SEED);
    }

    private QualityDocument generateDocument() {
        return TestDataUtils.generateDocument(random);
    }

    @Test
    public void whenFindDocumentsResponseSupplierIdNotProvidedShouldReturnError() {
        MdmDocument.FindDocumentsResponse findDocumentsResponse = DocumentRequestValidator
            .validateFindSupplierDocumentsRequest(MdmDocument.FindSupplierDocumentsRequest.newBuilder().build()).get();
        assertSoftly(softly -> {
            softly.assertThat(findDocumentsResponse.getStatus())
                .isEqualTo(MdmDocument.FindDocumentsResponse.Status.ERROR);
            softly.assertThat(findDocumentsResponse.getDocumentList()).hasSize(0);
            softly.assertThat(findDocumentsResponse.hasNextOffsetKey()).isFalse();

            softly.assertThat(findDocumentsResponse.getStatusMessage().getMessageCode())
                .isEqualTo(MbocErrors.get().qdSupplierDocumentsNullSupplierId().getErrorCode());
            softly.assertThat(findDocumentsResponse.getStatusMessage().getMustacheTemplate())
                .isEqualTo(MbocErrors.get().qdSupplierDocumentsNullSupplierId().getMessageTemplate());
        });
    }

    @Test
    public void whenFindDocumentsRequestOffsetKeyIsInvalidShouldReturnError() {
        MdmDocument.FindDocumentsResponse findDocumentsResponse = DocumentRequestValidator
            .validateFindSupplierDocumentsRequest(MdmDocument.FindSupplierDocumentsRequest.newBuilder()
                .setSupplierId(1)
                .setOffsetKey("not a number")
                .build()).get();
        assertSoftly(softly -> {
            softly.assertThat(findDocumentsResponse.getStatus())
                .isEqualTo(MdmDocument.FindDocumentsResponse.Status.ERROR);
            softly.assertThat(findDocumentsResponse.getDocumentList()).hasSize(0);
            softly.assertThat(findDocumentsResponse.hasNextOffsetKey()).isFalse();

            softly.assertThat(findDocumentsResponse.getStatusMessage())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(MbocErrors.get()
                    .protoUnknownError("offset_key не являетя целым числом:not a number")));
        });
    }

    @Test
    public void whenAddDocumentsResponseSupplierIdNotProvidedShouldReturnError() {
        MdmDocument.AddDocumentsResponse addDocumentsResponse = DocumentRequestValidator
            .validateAddSupplierDocumentsRequest(MdmDocument.AddSupplierDocumentsRequest.newBuilder().build()).get();
        assertSoftly(softly -> {
            softly.assertThat(addDocumentsResponse.getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);

            softly.assertThat(addDocumentsResponse.getError().getMessageCode())
                .isEqualTo(MbocErrors.get().qdSupplierDocumentsNullSupplierId().getErrorCode());
            softly.assertThat(addDocumentsResponse.getError().getMustacheTemplate())
                .isEqualTo(MbocErrors.get().qdSupplierDocumentsNullSupplierId().getMessageTemplate());
        });
    }

    @Test
    public void whenAddingEmptyDocumentListDocumentsShouldReturnError() {
        MdmDocument.AddDocumentsResponse addDocumentsResponse = DocumentRequestValidator
            .validateAddSupplierDocumentsRequest(
                MdmDocument.AddSupplierDocumentsRequest.newBuilder()
                    .setSupplierId(1)
                    .build()).get();
        assertSoftly(softly -> {
            softly.assertThat(addDocumentsResponse.getStatus())
                .isEqualTo(MdmDocument.AddDocumentsResponse.Status.ERROR);

            softly.assertThat(addDocumentsResponse.getError().getMessageCode())
                .isEqualTo(MbocErrors.get().qdAddSupplierDocumentsEmptyDocumentList().getErrorCode());
            softly.assertThat(addDocumentsResponse.getError().getMustacheTemplate())
                .isEqualTo(MbocErrors.get().qdAddSupplierDocumentsEmptyDocumentList().getMessageTemplate());
        });
    }

    @Test
    public void whenAddingEmptyDocumentsShouldReturnErrorsInDocuments() {
        List<ErrorInfo> errorInfos = DocumentRequestValidator.validateDocumentAddition(
            DocumentAddition.newBuilder().build(), true);
        Assertions.assertThat(errorInfos).containsExactly(MbocErrors.get()
            .qdAddSupplierDocumentsInvalidDocument("document обязателен."));
    }

    @Test
    public void whenAddingIncorrectDocumentInDocumentsShouldReturnErrorsInDocuments() {
        List<ErrorInfo> errorInfos = DocumentRequestValidator.validateDocumentAddition(
            DocumentAddition.newBuilder()
                .setDocument(MdmDocument.Document.newBuilder().build())
                .addNewScanFile(DocumentAddition.ScanFile.newBuilder()
                    .setUrl(CORRECT_SCAN_FILE_URL)
                    .setFileName(CORRECT_SCAN_FILE_URL))
                .build(), true);

        Assertions.assertThat(errorInfos).containsExactly(
            MbocErrors.get().qdAddSupplierDocumentsInvalidDocument("Поле type обязательно."),
            MbocErrors.get().qdAddSupplierDocumentsInvalidDocument("Поле start_date обязательно."),
            MbocErrors.get().qdIncorrectRegistrationNumberFormat());
    }

    @Test
    public void whenAddingIncorrectDocumentInDocumentsShouldReturnQualityDocumentValidationErrorsInDocuments() {
        QualityDocument incorrectDocument = generateDocument()
            .setRegistrationNumber("1")
            .setStartDate(LocalDate.now().plusDays(1))
            .setEndDate(LocalDate.now().minusDays(1));
        List<ErrorInfo> errorInfos = DocumentRequestValidator.validateDocumentAddition(
            DocumentAddition.newBuilder()
                .setDocument(DocumentProtoConverter.createProtoDocument(incorrectDocument))
                .addNewScanFile(DocumentAddition.ScanFile.newBuilder()
                    .setUrl(CORRECT_SCAN_FILE_URL)
                    .setFileName(CORRECT_SCAN_FILE_URL))
                .build(), true);
        Assertions.assertThat(errorInfos).containsExactly(
            MbocErrors.get().qdIncorrectRegistrationNumberFormat(),
            MbocErrors.get().qdStartDateAfterEndDate(),
            MbocErrors.get().qdEndDateInPast());
    }

    @Test
    public void whenEmptyDocumentRelationsPassedShouldReturnError() {
        Optional<MdmDocument.AddDocumentRelationsResponse> errorResponse = DocumentRequestValidator
            .validateAddDocumentRelationsRequest(MdmDocument.AddDocumentRelationsRequest.newBuilder().build());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(errorResponse).isPresent();
            MdmDocument.AddDocumentRelationsResponse response = errorResponse.get();
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.AddDocumentRelationsResponse.Status.ERROR);
            softAssertions.assertThat(response.getError())
                .isEqualTo(MbocCommonMessageUtils
                    .errorInfoToMessage(MbocErrors.get().qdAddDocumentRelationsEmptyRelations()));
        });
    }


    @Test
    public void whenRelationRegistrationNumberIsNullShouldReturnError() {
        Optional<ErrorInfo> error = DocumentRequestValidator.validateDocumentOfferRelation(
            MdmDocument.DocumentOfferRelation
                .newBuilder()
                .setShopSku("1")
                .setSupplierId(1)
                .build());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(error).isPresent();
            softAssertions.assertThat(error.get()).isEqualTo(
                MbocErrors.get().qdAddDocumentRelationsInvalidDocumentRelation(
                    "Поля registration_number, supplier_id, shop_sku обязательны."));
        });
    }

    @Test
    public void whenRelationShopSkuIsNullShouldReturnError() {
        Optional<ErrorInfo> error = DocumentRequestValidator.validateDocumentOfferRelation(
            MdmDocument.DocumentOfferRelation
                .newBuilder()
                .setRegistrationNumber("1")
                .setSupplierId(1)
                .build());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(error).isPresent();
            softAssertions.assertThat(error.get()).isEqualTo(
                MbocErrors.get().qdAddDocumentRelationsInvalidDocumentRelation(
                    "Поля registration_number, supplier_id, shop_sku обязательны."));
        });
    }

    @Test
    public void whenRelationSupplierIdIsNullShouldReturnError() {
        Optional<ErrorInfo> error = DocumentRequestValidator.validateDocumentOfferRelation(
            MdmDocument.DocumentOfferRelation
                .newBuilder()
                .setRegistrationNumber("1")
                .setShopSku("1")
                .build());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(error).isPresent();
            softAssertions.assertThat(error.get()).isEqualTo(
                MbocErrors.get().qdAddDocumentRelationsInvalidDocumentRelation(
                    "Поля registration_number, supplier_id, shop_sku обязательны."));
        });
    }

    @Test
    public void whenFindDocumentRelationsRequestRegistrationNumberIsNullShouldReturnError() {
        Optional<MdmDocument.FindSupplierDocumentRelationsResponse> error =
            DocumentRequestValidator.validateFindDocumentRelationsRequest(
                MdmDocument.FindSupplierDocumentRelationsRequest
                    .newBuilder()
                    .setSupplierId(1)
                    .build());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(error).isPresent();
            MdmDocument.FindSupplierDocumentRelationsResponse response = error.get();
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.FindSupplierDocumentRelationsResponse.Status.ERROR);
            softAssertions.assertThat(response.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().qdFindRelationsEmptyFields()));
        });
    }

    @Test
    public void whenFindDocumentRelationsSupplierIdIsNullShouldReturnError() {
        Optional<MdmDocument.FindSupplierDocumentRelationsResponse> error =
            DocumentRequestValidator.validateFindDocumentRelationsRequest(
                MdmDocument.FindSupplierDocumentRelationsRequest
                    .newBuilder()
                    .setRegistrationNumber("1234")
                    .build());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(error).isPresent();
            MdmDocument.FindSupplierDocumentRelationsResponse response = error.get();
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.FindSupplierDocumentRelationsResponse.Status.ERROR);
            softAssertions.assertThat(response.getError())
                .isEqualTo(MbocCommonMessageUtils.errorInfoToMessage(
                    MbocErrors.get().qdFindRelationsEmptyFields()));
        });
    }

    @Test
    public void whenFindDocumentByRegistrationNumberRegistrationNumberNotProvidedShouldReturnError() {
        MdmDocument.FindDocumentsResponse findDocumentsResponse = DocumentRequestValidator
            .validateFindSupplierDocumentByRegNumberRequest(
                MdmDocument.FindDocumentByRegistrationNumberRequest.newBuilder().build()).get();
        assertSoftly(softly -> {
            softly.assertThat(findDocumentsResponse.getStatus())
                .isEqualTo(MdmDocument.FindDocumentsResponse.Status.ERROR);
            softly.assertThat(findDocumentsResponse.getDocumentList()).hasSize(0);
            softly.assertThat(findDocumentsResponse.hasNextOffsetKey()).isFalse();

            softly.assertThat(findDocumentsResponse.getStatusMessage().getMessageCode())
                .isEqualTo(MbocErrors.get().qdFindDocumentEmptyRegistrationNumber().getErrorCode());
        });
    }

    @Test
    public void whenEmptyDocumentRelationsToRemoveShouldReturnError() {
        Optional<MdmDocument.RemoveDocumentOfferRelationsResponse> errorResponse = DocumentRequestValidator
            .validateRemoveDocumentsRelationsRequest(
                MdmDocument.RemoveDocumentOfferRelationsRequest.newBuilder().build());

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(errorResponse).isPresent();
            MdmDocument.RemoveDocumentOfferRelationsResponse response = errorResponse.get();
            softAssertions.assertThat(response.getStatus())
                .isEqualTo(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.ERROR);
            softAssertions.assertThat(response.getError())
                .isEqualTo(MbocCommonMessageUtils
                    .errorInfoToMessage(MbocErrors.get().qdRemoveDocumentRelationsEmptyRelations()));
        });
    }
}
