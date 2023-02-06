package ru.yandex.market.api.partner.controllers.model.getmodels;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.api.partner.controllers.model.model.Models;
import ru.yandex.market.api.partner.controllers.model.view.PartnerApiModelsPrimeReportConverter;
import ru.yandex.market.common.report.model.Model;
import ru.yandex.market.common.report.parser.json.PrimeSearchResultParser;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

/**
 * @author: belmatter
 * Для тестов используется запрос:
 * http://{report.host}:17051/yandsearch?rids=213&place=prime&text={query}&rids=213&pp=18&show-urls=decrypted&entities=product
 * {report.host} = report.tst.vs.market.yandex.net
 * {query} в каждом случае свой
 */
@RunWith(Parameterized.class)
public class ParseModelsFromReportTest {

    @Parameterized.Parameter(0)
    public String postfix;

    @Parameterized.Parameter(1)
    public int size;

    private Models models;

    private PrimeSearchResultParser<Models> parser = new PrimeSearchResultParser<>(
            new PartnerApiModelsPrimeReportConverter(1, 10)
    );

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> testData() {
        Object[][] objects = new Object[][]{
                // Проверка выдачи с большим количеством моделей
                // Запрос: "iPhone"
                {"iPhone", 10},
                // Проверка пустой выдачи
                // Запрос: "афафафа"
                {"EMPTY", 0},
                // Проверка выдачи с моделью без предложений и без цен
                // Запрос: "nokia 3410"
                {"WITHOUT_PRICES", 1},
        };
        return Arrays.asList(objects);
    }

    @Before
    public void setUp() throws IOException {
        InputStream is = this.getClass().getResourceAsStream(String.format("ReportPrimeResponse_%s.json", postfix));
        models = parser.parse(is);
    }


    @Test
    public void testModelsCount() {
        assertThat(models.getModels(), hasSize(size));
    }

    @Test
    public void testModels() {
        for (Model model : models.getModels()) {
            assertThat(model.getId(), greaterThan(0L));
            assertThat(model.getName(), not(isEmptyOrNullString()));
            assertThat(model.getPrices(), notNullValue());
            assertThat(model.getPrices().getAvg(), greaterThanOrEqualTo(BigDecimal.ZERO));
            assertThat(model.getPrices().getMax(), greaterThanOrEqualTo(BigDecimal.ZERO));
            assertThat(model.getPrices().getMin(), greaterThanOrEqualTo(BigDecimal.ZERO));
        }
    }

}
