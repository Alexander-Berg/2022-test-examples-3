package ru.yandex.market.core.supplier.certification;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.supplier.certification.model.CertificationDocument;
import ru.yandex.market.core.supplier.certification.model.CertificationDocumentType;
import ru.yandex.market.core.supplier.certification.model.MdmImageSet;
import ru.yandex.market.mdm.http.MdmDocument;

/**
 * Тесты на преобразование моделей сертификатов.
 */
class CertificationDocumentConversionsTest {

    @Test
    void testEmptyFieldsConversion() {
        CertificationDocument<MdmImageSet> result = CertificationDocumentConversions.toCertificationDocument(
                MdmDocument.Document.newBuilder().build()
        );
        Assertions.assertEquals(new CertificationDocument.Builder<MdmImageSet>()
                .setRegistrationNumber("")
                .setType(CertificationDocumentType.DECLARATION_OF_CONFORMITY)
                .setStartDate(LocalDate.ofEpochDay(0))
                .setImages(MdmImageSet.of(Collections.emptyList()))
                .build(), result);
    }

    @Test
    void testAllFieldsConversion() {

        CertificationDocument<MdmImageSet> result = CertificationDocumentConversions.toCertificationDocument(
                MdmDocument.Document.newBuilder()
                        .setRegistrationNumber("number1")
                        .setStartDate(17500)
                        .setEndDate(17865)
                        .setType(MdmDocument.Document.DocumentType.CERTIFICATE_OF_CONFORMITY)
                        .addPicture("http://picture.ru/pict1")
                        .build()
        );

        Assertions.assertEquals(new CertificationDocument.Builder<MdmImageSet>()
                .setRegistrationNumber("number1")
                .setType(CertificationDocumentType.CERTIFICATE_OF_CONFORMITY)
                .setStartDate(LocalDate.ofEpochDay(17500))
                .setEndDate(LocalDate.ofEpochDay(17865))
                .setImages(MdmImageSet.of(Collections.singletonList("http://picture.ru/pict1")))
                .build(), result);
    }
}
