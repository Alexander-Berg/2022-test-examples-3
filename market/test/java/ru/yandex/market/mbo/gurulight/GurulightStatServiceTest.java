package ru.yandex.market.mbo.gurulight;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.TovarTreeForVisualService;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;

import static org.mockito.ArgumentMatchers.anyLong;

/**
 * @author galaev@yandex-team.ru
 * @since 30/10/2018.
 */
public class GurulightStatServiceTest {

    private GurulightStatService service;

    @Rule
    public ExpectedException expectedExpection = ExpectedException.none();

    @Before
    public void setUp() {
        service = new GurulightStatService();
        TovarTreeForVisualService tovarTreeService = Mockito.mock(TovarTreeForVisualService.class);
        TovarTree tovarTree = Mockito.mock(TovarTree.class);
        Mockito.when(tovarTree.findByHid(anyLong())).thenAnswer(i -> {
            TovarCategoryNode node = new TovarCategoryNode();
            TovarCategory data = new TovarCategory();
            data.setVisual(false);
            node.setData(data);
            return node;
        });
        Mockito.when(tovarTreeService.loadCachedWholeTree()).thenAnswer(i -> tovarTree);
        service.setTovarTreeForVisualService(tovarTreeService);
    }

    @Test
    public void testNullVisualCategoryForCategoriesStat() {
        expectedExpection.expect(RuntimeException.class);
        expectedExpection.expectMessage("visual categoryId is null");

        service.getCategoriesStat("20180604_0847", 1L, 1L, 1L);
    }

    @Test
    public void testNullVisualCategoryForParamsStat() {
        expectedExpection.expect(RuntimeException.class);
        expectedExpection.expectMessage("visual categoryId is null");

        service.getParametersStat("20180604_0847", 1L, 1L, 1L);
    }

    @Test
    public void testNullVisualCategoryForValuesStat() {
        expectedExpection.expect(RuntimeException.class);
        expectedExpection.expectMessage("visual categoryId is null");

        service.getValuesStat("20180604_0847", 1L, 1L, 1L, 1L);
    }

    @Test
    public void testNullVisualCategoryForShopsStat() {
        expectedExpection.expect(RuntimeException.class);
        expectedExpection.expectMessage("visual categoryId is null");

        service.getShopsStat("20180604_0847", 1L, 1L, 1L);
    }
}
