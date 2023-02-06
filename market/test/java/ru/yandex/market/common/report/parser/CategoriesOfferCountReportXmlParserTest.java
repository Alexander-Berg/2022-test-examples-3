package ru.yandex.market.common.report.parser;

import org.junit.Test;
import ru.yandex.market.common.report.parser.xml.CategoriesOfferCountReportXmlParser;

import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author artemmz
 *         created on 23.03.17.
 */
public class CategoriesOfferCountReportXmlParserTest {
    @Test
    public void parseCategories() throws Exception {
        CategoriesOfferCountReportXmlParser parser = new CategoriesOfferCountReportXmlParser();
        parser.parse(getClass().getResourceAsStream("/files/report_with_categories.xml"));
        Map<Long, Long> categoriesWithCount = parser.getCategoryToOffersCount();

        assertTrue(categoriesWithCount.size() > 0);
        assertEquals((long) categoriesWithCount.get(91521L), 1);
        assertEquals((long) categoriesWithCount.get(90692L), 24);
        assertEquals((long) categoriesWithCount.get(543487L), 2);
    }
}
