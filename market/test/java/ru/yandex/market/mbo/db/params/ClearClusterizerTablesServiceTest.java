package ru.yandex.market.mbo.db.params;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.assessment.AssessOfferListService;
import ru.yandex.market.mbo.common.processing.ProcessingResult;
import ru.yandex.market.mbo.gwt.models.tovartree.ProcessingWarning;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;

import java.util.Collections;
import java.util.List;

/**
 * @author prediger
 */
@RunWith(MockitoJUnitRunner.class)
public class ClearClusterizerTablesServiceTest {
    private static final TovarCategory CATEGORY_1 = TovarCategoryBuilder.newBuilder(1, 100L)
        .setName("test category")
        .setTovarId(1)
        .create();

    private ClearClusterizerTablesService clearClusterizerTablesService;
    @Mock
    private AssessOfferListService assessOfferListService;

    @Before
    public void setUp() throws Exception {
        clearClusterizerTablesService = new ClearClusterizerTablesService();
        clearClusterizerTablesService.setAssessOfferListService(assessOfferListService);
    }

    @Test
    public void testCorrectPostDelete() {
        Mockito.when(assessOfferListService.deleteTovarIdFromClusterizerLists(CATEGORY_1.getTovarId())).thenReturn(0);
        List<ProcessingResult> results = clearClusterizerTablesService
            .tovarCategoryPostDeleted(null, CATEGORY_1);
        Assert.assertEquals(Collections.emptyList(), results);

    }

    @Test
    public void testWarningPostDelete() {
        Mockito.when(assessOfferListService.deleteTovarIdFromClusterizerLists(CATEGORY_1.getTovarId()))
            .thenThrow(new RuntimeException("Test exception"));
        List<ProcessingResult> results = clearClusterizerTablesService
            .tovarCategoryPostDeleted(null, CATEGORY_1);
        Assert.assertEquals(1, results.size());
        ProcessingResult processingResult = results.get(0);
        Assert.assertEquals(ProcessingWarning.class, processingResult.getClass());
        Assert.assertEquals(
            "Произошла ошибка при удалении категории из оценочных выборок кластеризатора, tovarId=" +
                CATEGORY_1.getTovarId(), processingResult.getText());
    }
}
