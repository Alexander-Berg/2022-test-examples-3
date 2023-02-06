package ru.yandex.direct.jobs.promocodes;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.common.db.PpcProperty;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SavePartialProgressTest {
    private static final int SERVICE_ID = 7;
    private static final Long FIRST_UNPROCESSED_ROW_VALUE_INDEX = 8901L;
    PpcProperty<Long> property;
    SafeSearchTearOffPromocodesJob safeSearchTearOffPromocodesJob;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void init() {
        property = (PpcProperty<Long>) mock(PpcProperty.class);
        when(property.getOrDefault(anyLong())).thenReturn(FIRST_UNPROCESSED_ROW_VALUE_INDEX);

        safeSearchTearOffPromocodesJob = new SafeSearchTearOffPromocodesJob(SERVICE_ID, null, property, null,
                null, null, null);
    }

    @Test
    void noItems_doNothing() {
        safeSearchTearOffPromocodesJob.savePartialProgress(FIRST_UNPROCESSED_ROW_VALUE_INDEX);
        verify(property, never()).set(anyLong());
    }

    @Test
    void noImmediateItems_doNothing() {
        safeSearchTearOffPromocodesJob.processedItems.addAll(List.of(8902L, 8903L));
        safeSearchTearOffPromocodesJob.savePartialProgress(FIRST_UNPROCESSED_ROW_VALUE_INDEX);
        verify(property, never()).set(anyLong());
    }

    @Test
    void consecutiveImmediateItems_writeLastId() {
        safeSearchTearOffPromocodesJob.processedItems.addAll(List.of(8901L, 8902L, 8903L, 8904L));
        safeSearchTearOffPromocodesJob.savePartialProgress(FIRST_UNPROCESSED_ROW_VALUE_INDEX);
        verify(property).set(8904L);
    }

    @Test
    void immediateItemsWithGap_writeLastIdBeforeGap() {
        safeSearchTearOffPromocodesJob.processedItems.addAll(List.of(8901L, 8902L, 8903L, 8905L));
        safeSearchTearOffPromocodesJob.savePartialProgress(FIRST_UNPROCESSED_ROW_VALUE_INDEX);
        verify(property).set(8903L);
    }
}
