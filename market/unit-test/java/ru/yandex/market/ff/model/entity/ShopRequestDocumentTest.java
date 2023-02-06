package ru.yandex.market.ff.model.entity;

import java.time.LocalDateTime;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.client.enums.DocumentType;
import ru.yandex.market.ff.enums.FileExtension;

class ShopRequestDocumentTest extends SoftAssertionSupport {

    @Test
    void testCopy() {

        var shopRequestDocument = createShopRequestDocumentWithNonNullFieldValues();

        var fieldNamesWithNullValue = ReflectionTestUtils.findFieldNamesWithNullOrDefaultValue(
                shopRequestDocument,
                ShopRequestDocument.class
        );

        if (!fieldNamesWithNullValue.isEmpty()) {
            assertions.fail(
                    "All fields of copying object must be set. Field names with null or default values: " +
                            fieldNamesWithNullValue
            );
        }

        var actualShopRequestDocument = shopRequestDocument.copy();

        var expectedShopRequest = createShopRequestDocumentWithNonNullFieldValues();
        // ignore id, items and status history
        expectedShopRequest.setId(null);

        ReflectionTestUtils.AssertingFieldValuesConsumer fieldValuesConsumer =
                (fieldName, actualFieldValue, expectedFieldValue) -> {
                    if (actualFieldValue instanceof Collection) {
                        Collection actual = (Collection) actualFieldValue;
                        Collection expected = (Collection) expectedFieldValue;
                        assertions.assertThat(actual.size()).as(fieldName).isEqualTo(expected.size());
                    }
                    if (actualFieldValue instanceof Supplier) {
                        Supplier actual = (Supplier) actualFieldValue;
                        Supplier expected = (Supplier) expectedFieldValue;
                        assertions.assertThat(actual.getId()).isEqualTo(expected.getId());
                        assertions.assertThat(actual.getName()).isEqualTo(expected.getName());
                        assertions.assertThat(actual.getSupplierType()).isEqualTo(expected.getSupplierType());
                    } else {
                        assertions.assertThat(actualFieldValue).as(fieldName).isEqualTo(expectedFieldValue);
                    }
                };

        ReflectionTestUtils.compareFieldValues(
                actualShopRequestDocument,
                expectedShopRequest,
                ShopRequestDocument.class,
                fieldValuesConsumer
        );
    }

    private ShopRequestDocument createShopRequestDocumentWithNonNullFieldValues() {
        var createdDate = LocalDateTime.of(2020, 5, 10, 0, 0);
        ShopRequestDocument document = new ShopRequestDocument();
        document.setId(1L);
        document.setRequestId(1L);
        document.setFileUrl("https://fileUrl.com");
        document.setCreatedAt(createdDate);
        document.setExtension(FileExtension.PDF);
        document.setType(DocumentType.TORG2);
        document.setUpdateRequired(true);
        return document;
    }
}
