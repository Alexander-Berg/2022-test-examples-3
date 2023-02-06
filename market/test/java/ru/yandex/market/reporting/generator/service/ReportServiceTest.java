package ru.yandex.market.reporting.generator.service;

import com.google.common.collect.ImmutableSortedMap;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.reporting.common.domain.tree.Category;
import ru.yandex.market.reporting.common.domain.tree.Region;
import ru.yandex.market.reporting.generator.domain.CompetitorsMapKind;
import ru.yandex.market.reporting.generator.domain.DatePeriod;
import ru.yandex.market.reporting.common.domain.Domain;
import ru.yandex.market.reporting.generator.domain.MarketReportParameters;
import ru.yandex.market.reporting.generator.domain.ReportComponents;

import java.time.YearMonth;
import java.util.Collections;
import java.util.Locale;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
public class ReportServiceTest {

    private MarketReportService reportService;

    private static MarketReportService reportService() {
        DictionaryService dictionaryService = dictionaryService();
        return new MarketReportService(null, null, null, null, null, dictionaryService, null, null);
    }

    private static DictionaryService dictionaryService() {
        DictionaryService dictionaryService = mock(DictionaryService.class);
        when(dictionaryService.getCategories(new Locale("ru"))).thenReturn(new Category(90402L, "mobile", 0L, ""));
        when(dictionaryService.getDomains()).thenReturn((new TreeMap<>(ImmutableSortedMap.of("mts.ru", new Domain("mts.ru", new long[0])))));
        when(dictionaryService.getRegions(new Locale("ru"))).thenReturn(new Region(225L, "voscow", 0L));
        return dictionaryService;
    }

    @Before
    public void setUp() throws Exception {
        reportService = reportService();
    }

    @Test
    public void gatherReportData() throws Exception {
        DatePeriod period = new DatePeriod(YearMonth.of(2016, 9), YearMonth.of(2016, 9));
        MarketReportParameters params = reportParams();

        ReportComponents.CpcSlide1 cpcSlide1 = cpcSlide1(period);
        params.getComponents().setCpcSlide1(cpcSlide1);

        ReportComponents.CpcSlide2 cpcSlide2 = cpcSlide2(period);
        params.getComponents().setCpcSlide2(cpcSlide2);

    }

    @Test
    public void cpcSlide2ValidationTest() throws Exception {
        MarketReportParameters params = reportParams();
        DatePeriod period = new DatePeriod(YearMonth.of(2016, 9), YearMonth.of(2016, 9));

        ReportComponents.CpcSlide2 cpcSlide2 = cpcSlide2(period);
        params.getComponents().setCpcSlide2(cpcSlide2);

        cpcSlide2.setBrandVndId(123L);
        cpcSlide2.setMapKind(CompetitorsMapKind.ASSORTMENT);

        try {
            reportService.validateParams(params);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat("exception message", e.getMessage(), is("You may specify Brand only for brand report"));
        }
    }

    @Test
    public void cpcSlide2ValidationBrandTest() throws Exception {
        MarketReportParameters params = reportParams();

        DatePeriod period = new DatePeriod(YearMonth.of(2016, 9), YearMonth.of(2016, 9));

        ReportComponents.CpcSlide2 cpcSlide2 = cpcSlide2(period);
        params.getComponents().setCpcSlide2(cpcSlide2);

        cpcSlide2.setBrandVndId(null);
        cpcSlide2.setMapKind(CompetitorsMapKind.BRAND);

        try {
            reportService.validateParams(params);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat("exception message", e.getMessage(), is("You must specify Brand for brand report"));
        }
    }

    private ReportComponents.CpcSlide1 cpcSlide1(final DatePeriod period) {
        ReportComponents.CpcSlide1 cpcSlide1 = new ReportComponents.CpcSlide1();
        cpcSlide1.setCategoryDynamicDiagramPeriod(period);
        cpcSlide1.setClicksShareDiagramPeriod(period);
        cpcSlide1.setClicksShareDynamicDiagramPeriod(period);
        return cpcSlide1;
    }

    private MarketReportParameters reportParams() {
        MarketReportParameters params = new MarketReportParameters();
        params.setShop("MTC");
        params.setDomain("mts.ru");
        params.setRegions(Collections.singletonList(225L));
        params.setCategories(Collections.singletonList(90402L));
        params.setLanguage(new Locale("ru"));
        return params;
    }

    private ReportComponents.CpcSlide2 cpcSlide2(final DatePeriod period) {
        ReportComponents.CpcSlide2 cpcSlide2 = new ReportComponents.CpcSlide2();
        cpcSlide2.setCompetitionMapDiagramPeriod(period);
        cpcSlide2.setMapKind(CompetitorsMapKind.CATEGORY);
        cpcSlide2.setBrandVndId(null);

        return cpcSlide2;
    }
}
