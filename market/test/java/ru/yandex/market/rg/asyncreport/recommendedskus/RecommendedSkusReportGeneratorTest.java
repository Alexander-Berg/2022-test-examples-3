package ru.yandex.market.rg.asyncreport.recommendedskus;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.fulfillment.mds.ReportsMdsStorage;
import ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils;
import ru.yandex.market.core.yt.YtHttpFactory;
import ru.yandex.market.core.yt.YtTablesMockUtils;
import ru.yandex.market.rg.asyncreport.ReportFunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "RecommendedSkusReportGeneratorTest.db.data.before.csv")
public class RecommendedSkusReportGeneratorTest extends ReportFunctionalTest {

    @Autowired
    RecommendedSkusReportGenerator generator;

    @Autowired
    YtHttpFactory ytHttpFactory;

    @Autowired
    ReportsMdsStorage<ReportsType> mdsStorage;

    private final Yt yt = mock(Yt.class);

    @Test
    void testGenerateReport() throws IOException {
        when(yt.transactions()).thenReturn(mock(YtTransactions.class));
        YtTablesMockUtils.mockYt(yt, getClass(), "RecommendedSkusReportGeneratorTest.yt.data.json", "recomend_orders");
        when(ytHttpFactory.getYt(any())).thenReturn(yt);

        checkExcelReport("001", 1L, generator, sheet -> {
            assertHeader(sheet);
            assertSheet(sheet);
        });
    }

    private static void assertHeader(Sheet sheet) {
        List<List<Object>> tableHeader = Collections.singletonList(
                Arrays.asList("Регион склада",
                              "Город склада",
                              "SKU на Маркете",
                              "Раздел",
                              "Категория",
                              "Производитель",
                              "Название товара",
                              "Рекомендуемая стоимость",
                              "Спрос на товар.\n\r(где 1 - самый высокий спрос)",
                              "Штрихкод"));
        ExcelTestUtils.assertCellValues(tableHeader, sheet, 0, 0);
    }

    private static void assertSheet(Sheet sheet) {
        List<List<Object>> rows = Arrays.asList(
                Arrays.asList(
                        "Москва и Московская область",
                        "Москва",
                        "107979",
                        "Бытовая техника",
                        "Холодильники, морозильники, винные шкафы",
                        "Бирюса",
                        "Холодильник Бирюса M6027 M6027 Silver .",
                        "14607.0",
                        "1972",
                        "4630000764488"
                        ),
                Arrays.asList(
                        "Москва и Московская область",
                        "Москва",
                        "9380830",
                        "Строительство и ремонт",
                        "Смесители",
                        "Grohe",
                        "Смеситель для ванны, Grohe, BauEdge, короткий излив, с картриджем, хром, 23334000",
                        "6675.0",
                        "3625",
                        "4005176934322"
                ),
                Arrays.asList(
                        "Москва и Московская область",
                        "Москва",
                        "9380999",
                        "",
                        "",
                        "",
                        "Душ для ванны",
                        "",
                        "912",
                        ""
                ),
                Arrays.asList(
                        "Москва и Московская область",
                        "Москва",
                        "9041829",
                        "Одежда, обувь и аксессуары",
                        "Бижутерия",
                        "",
                        "Серьги с амальгамным покрытием",
                        "",
                        "804",
                        "4099900011148"
                )
        );
        ExcelTestUtils.assertCellValues(rows, sheet, 1, 0);
    }

    @Disabled("Интеграционный тест на проверку получения данных из YT. Перед запуском прописать токен в методе getToken()")
    void integrationYtTest() {
        YtHttpFactory localHttpFactory = (clusterMap) -> YtUtils.http(Objects.requireNonNull(clusterMap), getToken());
        String tablePath = "//home/market/testing/mbi/test_recomend_orders";
        RecommendedSkusReportService service = new RecommendedSkusReportService(
                                                        Collections.singletonList("hahn.yt.yandex.net"),
                                                        tablePath,
                                          3000,
                                                        localHttpFactory);
        RecommendedSkusReportGenerator reportGenerator = new RecommendedSkusReportGenerator(mdsStorage, "xlsx", service);

        checkExcelReport("001", 1L, reportGenerator, sheet -> {
            assertHeader(sheet);
            assertSheet(sheet);
        });
    }

    /**
     * Для запуска интеграционного теста надо положить продовый токен (не забыть стереть, чтоб не закомитить)
     */
    private String getToken() {
        return "${mbi.robot.yt.token}";
    }
}
