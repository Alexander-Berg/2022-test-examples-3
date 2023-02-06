package ru.yandex.market.mboc.common.masterdata.services.document;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class DocumentOfferFilterTest {

    /**
     * По умолчанию этот фильтр игнорирует документы, помеченные как удалённые.
     */
    @Test
    public void testDefaultFilterIgnoresDeletedDocs() {
        DocumentOfferFilter filter = new DocumentOfferFilter();
        assertFalse(filter.getDocumentFilter().getDeleted());
    }
}
