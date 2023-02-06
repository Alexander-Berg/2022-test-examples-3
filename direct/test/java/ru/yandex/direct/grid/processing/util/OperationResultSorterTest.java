package ru.yandex.direct.grid.processing.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.grid.core.entity.banner.model.GdiFindAndReplaceBannerHrefItem;
import ru.yandex.direct.grid.processing.model.common.GdiOperationResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.validation.result.PathHelper.index;

public class OperationResultSorterTest {

    @Test
    public void emptySortTest() {
        List<GdiFindAndReplaceBannerHrefItem> sourceList = Collections.emptyList();
        ValidationResult<List<OldBanner>, Defect> sourceValidationResult = new ValidationResult<>(Collections.emptyList());

        GdiOperationResult<GdiFindAndReplaceBannerHrefItem, OldBanner> sortedOperationResult =
                OperationResultSorter
                        .sortByValidationResult(new GdiOperationResult<>(sourceList, sourceValidationResult));

        assertEquals("Collections should be equals", Collections.emptyList(), sortedOperationResult.getRowset());
        assertEquals("Collections should be equals", sourceValidationResult.getValue(),
                sortedOperationResult.getValidationResult().getValue());
        assertEquals("Collections should be equals", sourceValidationResult.getSubResults(),
                sortedOperationResult.getValidationResult().getSubResults());
    }

    @Test
    public void onlyErrors_sortByValidationResult() {
        GdiFindAndReplaceBannerHrefItem item1 = new GdiFindAndReplaceBannerHrefItem();
        item1.setBannerId(1L);
        GdiFindAndReplaceBannerHrefItem item2 = new GdiFindAndReplaceBannerHrefItem();
        item2.setBannerId(2L);
        GdiFindAndReplaceBannerHrefItem item3 = new GdiFindAndReplaceBannerHrefItem();
        item3.setBannerId(3L);
        List<GdiFindAndReplaceBannerHrefItem> sourceList = new ArrayList<>(Arrays.asList(item1, item2, item3));


        ValidationResult<List<OldBanner>, Defect> sourceValidationResult =
                new ValidationResult<>(Collections.singletonList(new OldTextBanner()));

        Object object1 = new Object();
        Object object2 = new Object();
        Object object3 = new Object();
        sourceValidationResult.getOrCreateSubValidationResult(index(0), object1)
                .addError(BannerDefects.invalidHref());
        sourceValidationResult.getOrCreateSubValidationResult(index(2), object3)
                .addError(BannerDefects.invalidHref());

        GdiOperationResult<GdiFindAndReplaceBannerHrefItem, OldBanner> sortedOperationResult =
                OperationResultSorter
                        .sortByValidationResult(new GdiOperationResult<>(sourceList, sourceValidationResult));

        List<GdiFindAndReplaceBannerHrefItem> expectedList = new ArrayList<>(Arrays.asList(item1, item3, item2));

        assertEquals("Collections and order should be equals", expectedList, sortedOperationResult.getRowset());
        assertEquals(object1, sortedOperationResult.getValidationResult().getSubResults().get(index(0)).getValue());
        assertEquals(object3, sortedOperationResult.getValidationResult().getSubResults().get(index(1)).getValue());
    }

    @Test
    public void separateWarningFirst_sortByValidationResult() {
        GdiFindAndReplaceBannerHrefItem item1 = new GdiFindAndReplaceBannerHrefItem();
        item1.setBannerId(1L);
        GdiFindAndReplaceBannerHrefItem item2 = new GdiFindAndReplaceBannerHrefItem();
        item2.setBannerId(2L);
        GdiFindAndReplaceBannerHrefItem item3 = new GdiFindAndReplaceBannerHrefItem();
        item3.setBannerId(3L);
        List<GdiFindAndReplaceBannerHrefItem> sourceList = new ArrayList<>(Arrays.asList(item1, item2, item3));


        ValidationResult<List<OldBanner>, Defect> sourceValidationResult =
                new ValidationResult<>(Collections.singletonList(new OldTextBanner()));

        Object object1 = new Object();
        Object object3 = new Object();
        sourceValidationResult.getOrCreateSubValidationResult(index(0), object1)
                .addWarning(BannerDefects.invalidHref());
        sourceValidationResult.getOrCreateSubValidationResult(index(2), object3)
                .addError(BannerDefects.invalidHref());

        GdiOperationResult<GdiFindAndReplaceBannerHrefItem, OldBanner> sortedOperationResult =
                OperationResultSorter
                        .sortByValidationResult(new GdiOperationResult<>(sourceList, sourceValidationResult));

        List<GdiFindAndReplaceBannerHrefItem> expectedList = new ArrayList<>(Arrays.asList(item3, item1, item2));

        assertEquals("Collections and order should be equals", expectedList, sortedOperationResult.getRowset());
        assertEquals(object3, sortedOperationResult.getValidationResult().getSubResults().get(index(0)).getValue());
        assertEquals(object1, sortedOperationResult.getValidationResult().getSubResults().get(index(1)).getValue());
    }

    @Test
    public void warningAndErrorInOneElement_sortByValidationResult() {
        GdiFindAndReplaceBannerHrefItem item1 = new GdiFindAndReplaceBannerHrefItem();
        item1.setBannerId(1L);
        GdiFindAndReplaceBannerHrefItem item2 = new GdiFindAndReplaceBannerHrefItem();
        item2.setBannerId(2L);
        GdiFindAndReplaceBannerHrefItem item3 = new GdiFindAndReplaceBannerHrefItem();
        item3.setBannerId(3L);
        List<GdiFindAndReplaceBannerHrefItem> sourceList = new ArrayList<>(Arrays.asList(item1, item2, item3));


        ValidationResult<List<OldBanner>, Defect> sourceValidationResult =
                new ValidationResult<>(Collections.singletonList(new OldTextBanner()));

        Object object1 = new Object();
        Object object3 = new Object();
        sourceValidationResult.getOrCreateSubValidationResult(index(0), object1)
                .addError(BannerDefects.invalidHref())
                .addWarning(BannerDefects.invalidHref());
        sourceValidationResult.getOrCreateSubValidationResult(index(2), object3)
                .addError(BannerDefects.invalidHref());

        GdiOperationResult<GdiFindAndReplaceBannerHrefItem, OldBanner> sortedOperationResult =
                OperationResultSorter
                        .sortByValidationResult(new GdiOperationResult<>(sourceList, sourceValidationResult));

        List<GdiFindAndReplaceBannerHrefItem> expectedList = new ArrayList<>(Arrays.asList(item1, item3, item2));

        assertEquals("Collections and order should be equals", expectedList, sortedOperationResult.getRowset());
        assertEquals(object1, sortedOperationResult.getValidationResult().getSubResults().get(index(0)).getValue());
        assertEquals(object3, sortedOperationResult.getValidationResult().getSubResults().get(index(1)).getValue());
    }


}
