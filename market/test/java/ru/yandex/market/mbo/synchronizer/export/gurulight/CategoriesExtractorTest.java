package ru.yandex.market.mbo.synchronizer.export.gurulight;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.db.TovarTreeForVisualService;
import ru.yandex.market.mbo.db.VisualService;
import ru.yandex.market.mbo.export.MboExport;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.VisualCategory;
import ru.yandex.market.mbo.synchronizer.export.BaseExtractor;
import ru.yandex.market.mbo.synchronizer.export.ExtractorBaseTestClass;
import ru.yandex.market.mbo.synchronizer.export.ExtractorWriterService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ayratgdl
 * @date 13.06.18
 */
public class CategoriesExtractorTest extends ExtractorBaseTestClass {
    private static final long HID1 = 101;
    private static final long VISUAL_ID1 = 301;

    private TovarTreeForVisualService tovarTreeService;
    private VisualService visualService;

    @Override
    @Before
    public void setUp() throws Exception {
        tovarTreeService = Mockito.mock(TovarTreeForVisualService.class);
        visualService = Mockito.mock(VisualService.class);
        super.setUp();
    }

    @Override
    protected BaseExtractor createExtractor() {
        CategoriesExtractor categoriesExtractor = new CategoriesExtractor();
        categoriesExtractor.setTovarTreeForVisualService(tovarTreeService);
        categoriesExtractor.setVisualService(visualService);
        ExtractorWriterService extractorWriterService = new ExtractorWriterService();
        categoriesExtractor.setExtractorWriterService(extractorWriterService);
        return categoriesExtractor;
    }

    @Test
    public void extractEmptyData() throws Exception {
        extractor.perform("");
        Assert.assertArrayEquals(new byte[0], getExtractContent());
    }

    @Test
    public void extractOneCategoryWithoutParameters() throws Exception {
        Mockito.when(tovarTreeService.getLeafVisualCategories()).then(new Answer<List<TovarCategory>>() {
            @Override
            public List<TovarCategory> answer(InvocationOnMock invocation) throws Throwable {
                TovarCategory category = new TovarCategory();
                category.setHid(HID1);
                category.setClusterize(true);
                return Arrays.asList(category);
            }
        });
        Mockito.when(visualService.loadPublishedVisualCategoryByHid(Mockito.eq(HID1))).then(
            new Answer<VisualCategory>() {
                @Override
                public VisualCategory answer(InvocationOnMock invocation) throws Throwable {
                    VisualCategory category = new VisualCategory();
                    category.setId(VISUAL_ID1);
                    category.setName("Category name");
                    CategoryEntities categoryEntities = new CategoryEntities();
                    categoryEntities.setHid(HID1);
                    category.setCategoryEntities(categoryEntities);
                    return category;
                }
            }
        );

        extractor.perform("");

        List<MboExport.Category> expectedCategories = Arrays.asList(
            MboExport.Category.newBuilder()
                .setId(VISUAL_ID1)
                .setHid(HID1)
                .addName(MboExport.Word.newBuilder().setName("Category name").setLangId(Language.RUSSIAN.getId()))
                .build()
        );

        Assert.assertEquals(expectedCategories, parseToCategories(getExtractContent()));
    }

    private List<MboExport.Category> parseToCategories(byte[] bytes) throws IOException {
        List<MboExport.Category> categories = new ArrayList<>();
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        MboExport.Category category;
        while ((category = MboExport.Category.parseDelimitedFrom(input)) != null) {
            categories.add(category);
        }
        return categories;
    }
}
