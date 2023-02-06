package ru.yandex.market.mbo.db.category_wiki;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.gwt.models.visual.CategoryWiki;
import ru.yandex.market.mbo.gwt.models.visual.CategoryWikiInherited;

/**
 * Created by eremeevvo on 04.12.18.
 */
@RunWith(MockitoJUnitRunner.class)
public class CategoryWikiExportServiceTest {

    private CategoryWikiExportService categoryWikiExportService;

    private CategoryWikiInherited categoryWikiInherited;

    @Before
    public void setUp() {
        CategoryWiki categoryWiki = new CategoryWiki()
            .setExportTitleToMbo(true)
            .setExportStructureToMbo(true);
        categoryWikiInherited = new CategoryWikiInherited();
        categoryWikiInherited.addPrimaryCategoryWiki(categoryWiki, "test");
        categoryWikiExportService = new CategoryWikiExportService();
    }

    @Test
    public void createDocxTest() throws Exception {
        XWPFDocument documentMbo = categoryWikiExportService.createDocx(categoryWikiInherited,
            CategoryWiki.ExportType.MBO);
        XWPFDocument documentPartners = categoryWikiExportService.createDocx(categoryWikiInherited,
            CategoryWiki.ExportType.PARTNERS);

        Assertions.assertThat(documentMbo).isNotNull();
        Assertions.assertThat(documentPartners).isNotNull();
    }
}
