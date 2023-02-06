package ru.yandex.market.common.report.parser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.common.report.model.Model;
import ru.yandex.market.common.report.model.Prices;
import ru.yandex.market.common.report.parser.xml.GeneralMarketReportXmlParserFactory;
import ru.yandex.market.common.report.parser.xml.ModelInfoReportXmlParser;

import java.math.BigDecimal;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * @author kukabara
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-bean.xml")
public class ModelInfoReportXmlParserTest {
    private static final String WITHOUT_PRICES_FILE = "/files/modelinfo.xml";
    private static final String WITH_PRICES_FILE = "/files/modelinfo_prices.xml";

    @Autowired
    @Qualifier(value = "modelInfoReportXmlParserFactory")
    private GeneralMarketReportXmlParserFactory testModelReportXmlParserFactory;
    private ModelInfoReportXmlParser parser;

    @Before
    public void setUp() throws Exception {
        parser = (ModelInfoReportXmlParser) testModelReportXmlParserFactory.newParser();
    }

    @Test
    public void testCategoryParse() throws Exception {
        parser.parse(MainReportXmlTest.class.getResourceAsStream(WITHOUT_PRICES_FILE));
        List<Model> models = parser.getModels();
        assertEquals(models.size(), 2);

        Model model = models.get(0);
        assertEquals(12911795, model.getId());
        assertEquals("LG Nexus 5X H791 32Gb", model.getName());
        assertEquals(91491, model.getCategoryId());
        assertEquals("Мобильные телефоны", model.getCategoryName());

        model = models.get(1);
        assertEquals(12911796, model.getId());
        assertEquals("Leberg Flamme 24 ASD", model.getName());
        assertEquals(12385944, model.getCategoryId());
        assertEquals("Отопительные котлы", model.getCategoryName());
    }

    @Test
    public void testCategoriesAndPricesParse() throws Exception {
        parser.parse(MainReportXmlTest.class.getResourceAsStream(WITH_PRICES_FILE));
        List<Model> models = parser.getModels();
        assertEquals(models.size(), 2);

        Model model = models.get(0);
        assertEquals(11902200, model.getId());
        assertEquals("LotusGrill LotusGrill", model.getName());
        assertEquals(5017483, model.getCategoryId());
        assertEquals("Грили, мангалы, шашлычницы", model.getCategoryName());

        Prices prices = model.getPrices();
        assertEquals(BigDecimal.valueOf(12500L), prices.getMin());
        assertEquals(BigDecimal.valueOf(13500), prices.getAvg());
        assertEquals(BigDecimal.valueOf(18500), prices.getMax());


        model = models.get(1);
        assertEquals(4602802, model.getId());
        assertEquals("ATLANT ХМ 6025-031", model.getName());
        assertEquals(90594, model.getCategoryId());
        assertEquals("Холодильники", model.getCategoryName());

        prices = model.getPrices();
        assertEquals(BigDecimal.valueOf(22010), prices.getMin());
        assertEquals(BigDecimal.valueOf(24000), prices.getAvg());
        assertEquals(BigDecimal.valueOf(32816), prices.getMax());
    }

}
