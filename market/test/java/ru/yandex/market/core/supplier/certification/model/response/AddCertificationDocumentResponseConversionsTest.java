package ru.yandex.market.core.supplier.certification.model.response;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.supplier.certification.CertificationDocumentConversions;
import ru.yandex.market.core.supplier.certification.model.CertificationDocument;
import ru.yandex.market.core.supplier.certification.model.CertificationDocumentType;
import ru.yandex.market.core.supplier.certification.model.MdmImageSet;
import ru.yandex.market.core.tanker.model.UserMessage;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mdm.http.MdmDocument;

/**
 * Тесты для преобразования моделей ответов при создании сертификатов.
 */
class AddCertificationDocumentResponseConversionsTest {

    @Test
    void testDocumentResponseMinFieldsTest() {
        AddCertificationDocumentsResponse docsResponse = CertificationDocumentConversions.toAddCertDocResponse(
                MdmDocument.AddDocumentsResponse.newBuilder()
                        .addDocumentResponse(MdmDocument.AddDocumentsResponse.DocumentResponse.newBuilder()
                                .build())
                        .build()
        );

        Assertions.assertEquals(CertDocResponseStatus.OK, docsResponse.getStatus().orElse(null));
        Assertions.assertTrue(!docsResponse.getError().isPresent());
        for (AddCertificationDocumentResponse docResponse : docsResponse.getDocumentResponses()) {
            Assertions.assertEquals(docResponse.getStatus().orElse(null), CertDocResponseStatus.OK);
            Assertions.assertTrue(docResponse.getErrors().isEmpty());
            Assertions.assertEquals(new CertificationDocument.Builder<MdmImageSet>()
                            .setType(CertificationDocumentType.DECLARATION_OF_CONFORMITY)
                            .setRegistrationNumber("")
                            .setStartDate(LocalDate.ofEpochDay(0))
                            .setImages(MdmImageSet.of(List.of())).build(),
                    docResponse.getDocument().orElse(null));
        }
    }

    @Test
    void testDocumentResponseAllFiledsTest() {
        AddCertificationDocumentsResponse docsResponse = CertificationDocumentConversions.toAddCertDocResponse(
                MdmDocument.AddDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                        .setError(MbocCommon.Message.newBuilder()
                                .setMessageCode("resp error code 1")
                                .setMustacheTemplate("resp error template 1")
                                .build())
                        .addDocumentResponse(MdmDocument.AddDocumentsResponse.DocumentResponse.newBuilder()
                                .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                                .addError(MbocCommon.Message.newBuilder()
                                        .setMessageCode("doc error code 1")
                                        .setMustacheTemplate("doc error template 1")
                                        .build())
                                .setDocument(MdmDocument.Document.newBuilder()
                                        .setRegistrationNumber("number1")
                                        .setStartDate(17500)
                                        .setEndDate(17865)
                                        .setType(MdmDocument.Document.DocumentType.CERTIFICATE_OF_CONFORMITY)
                                        .addPicture("http://picture.ru/pict1")
                                        .build())
                                .build())
                        .build()
        );
        Assertions.assertEquals(CertDocResponseStatus.OK, docsResponse.getStatus().orElse(null));
        Assertions.assertEquals(
                new UserMessage.Builder()
                        .setMessageCode("resp error code 1")
                        .setDefaultTranslation("resp error template 1")
                        .build(), docsResponse.getError().orElse(null));
        for (AddCertificationDocumentResponse docResponse : docsResponse.getDocumentResponses()) {
            Assertions.assertEquals(docResponse.getStatus().orElse(null), CertDocResponseStatus.OK);
            Assertions.assertEquals(
                    new UserMessage.Builder()
                            .setMessageCode("doc error code 1")
                            .setDefaultTranslation("doc error template 1")
                            .build(), docResponse.getErrors().iterator().next());
            Assertions.assertEquals(new CertificationDocument.Builder<MdmImageSet>()
                            .setType(CertificationDocumentType.CERTIFICATE_OF_CONFORMITY)
                            .setRegistrationNumber("number1")
                            .setStartDate(LocalDate.ofEpochDay(17500))
                            .setEndDate(LocalDate.ofEpochDay(17865))
                            .setImages(MdmImageSet.of(List.of("http://picture.ru/pict1"))).build(),
                    docResponse.getDocument().orElse(null));
        }
    }
}
