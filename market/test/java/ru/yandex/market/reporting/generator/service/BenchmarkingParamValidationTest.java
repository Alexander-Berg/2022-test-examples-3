package ru.yandex.market.reporting.generator.service;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.reporting.common.domain.tree.Category;
import ru.yandex.market.reporting.common.domain.tree.Region;
import ru.yandex.market.reporting.generator.domain.BenchmarkingKind;
import ru.yandex.market.reporting.common.domain.Domain;
import ru.yandex.market.reporting.generator.domain.MarketReportParameters;
import ru.yandex.market.reporting.generator.domain.ReportComponents.Benchmarking;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author nettoyeur
 * @since 30.08.2017
 */
public class BenchmarkingParamValidationTest {

    private static final Locale RU = new Locale("ru");
    private MarketReportService marketReportService;

    @Before
    public void setUp() throws Exception {
        DictionaryService dictionaryService = mock(DictionaryService.class);
        TreeMap<String, Domain> domains = new TreeMap<String, Domain>() {{
            put("client.domain", new Domain("client.domain", new long[0]));
        }};
        IntStream.rangeClosed(1, 6).mapToObj(i -> String.format("domain%02d", i)).forEach(domain -> domains.put(domain, new Domain(domain, new long[0])));
        when(dictionaryService.getDomains()).thenReturn(domains);
        when(dictionaryService.getRegions(RU)).thenReturn(new Region(1L, "fake", 0L));
        when(dictionaryService.getCategories(RU)).thenReturn(new Category(1L, "fake", 0L, ""));
        marketReportService = new MarketReportService(null, null, null, null, null, dictionaryService, null, null);
    }

    @Test
    public void specificShouldHaveNonNullDomains() throws Exception {
        MarketReportParameters parameters = baseParams();
        Benchmarking benchmarking = Benchmarking.builder().benchmarkingKind(BenchmarkingKind.SPECIFIC).build();
        parameters.getComponents().setBenchmarking(benchmarking);

        benchmarking.setCompetitorsDomains(null);
        validationShouldThrowExceptionWithMsg(parameters, "You must specify domains of competitors");
    }

    @Test
    public void specificShouldHaveNonEmptyDomains() throws Exception {
        MarketReportParameters parameters = baseParams();
        Benchmarking benchmarking = Benchmarking.builder().benchmarkingKind(BenchmarkingKind.SPECIFIC).build();
        parameters.getComponents().setBenchmarking(benchmarking);

        benchmarking.setCompetitorsDomains(Collections.emptyList());
        validationShouldThrowExceptionWithMsg(parameters, "You must specify domains of competitors");
    }

    @Test
    public void specificShouldHaveMoreThanThreeDomains() throws Exception {
        MarketReportParameters parameters = baseParams();
        Benchmarking benchmarking = Benchmarking.builder().benchmarkingKind(BenchmarkingKind.SPECIFIC).build();
        parameters.getComponents().setBenchmarking(benchmarking);

        benchmarking.setCompetitorsDomains(Arrays.asList("domain01"));
        validationShouldThrowExceptionWithMsg(parameters, "You must specify 3 to 5 domains");

        benchmarking.setCompetitorsDomains(Arrays.asList("domain01", "domain02"));
        validationShouldThrowExceptionWithMsg(parameters, "You must specify 3 to 5 domains");
    }

    @Test
    public void specificShouldHaveMoreThanThreeDomainsWithoutDuplicates() throws Exception {
        MarketReportParameters parameters = baseParams();
        Benchmarking benchmarking = Benchmarking.builder().benchmarkingKind(BenchmarkingKind.SPECIFIC).build();
        parameters.getComponents().setBenchmarking(benchmarking);

        benchmarking.setCompetitorsDomains(Arrays.asList("domain01", "domain02", "domain01", "domain02"));
        validationShouldThrowExceptionWithMsg(parameters, "You must specify 3 to 5 domains");
    }

    @Test
    public void specificShouldHaveClientDomainInCompetitors() throws Exception {
        MarketReportParameters parameters = baseParams();
        Benchmarking benchmarking = Benchmarking.builder().benchmarkingKind(BenchmarkingKind.SPECIFIC).build();
        parameters.getComponents().setBenchmarking(benchmarking);

        benchmarking.setCompetitorsDomains(Arrays.asList("domain01", "domain02", "client.domain"));
        validationShouldThrowExceptionWithMsg(parameters, "You can't have a client's domain in competitors");
    }

    @Test
    public void specificShouldFilterOutEmptyDomains() throws Exception {
        MarketReportParameters parameters = baseParams();
        Benchmarking benchmarking = Benchmarking.builder().benchmarkingKind(BenchmarkingKind.SPECIFIC).build();
        parameters.getComponents().setBenchmarking(benchmarking);

        benchmarking.setCompetitorsDomains(Arrays.asList("domain01", "domain02", ""));
        validationShouldThrowExceptionWithMsg(parameters, "You must specify 3 to 5 domains");
    }

    @Test
    public void specificShouldFilterOutBlankDomains() throws Exception {
        MarketReportParameters parameters = baseParams();
        Benchmarking benchmarking = Benchmarking.builder().benchmarkingKind(BenchmarkingKind.SPECIFIC).build();
        parameters.getComponents().setBenchmarking(benchmarking);

        benchmarking.setCompetitorsDomains(Arrays.asList("domain01", "domain02", " "));
        validationShouldThrowExceptionWithMsg(parameters, "You must specify 3 to 5 domains");
    }

    @Test
    public void specificShouldFilterOutNullDomains() throws Exception {
        MarketReportParameters parameters = baseParams();
        Benchmarking benchmarking = Benchmarking.builder().benchmarkingKind(BenchmarkingKind.SPECIFIC).build();
        parameters.getComponents().setBenchmarking(benchmarking);

        benchmarking.setCompetitorsDomains(Arrays.asList("domain01", "domain02", null));
        validationShouldThrowExceptionWithMsg(parameters, "You must specify 3 to 5 domains");
    }

    @Test
    public void specificShouldHaveLessThanFiveDomains() throws Exception {
        MarketReportParameters parameters = baseParams();
        Benchmarking benchmarking = Benchmarking.builder().benchmarkingKind(BenchmarkingKind.SPECIFIC).build();
        parameters.getComponents().setBenchmarking(benchmarking);

        benchmarking.setCompetitorsDomains(Arrays.asList("domain01", "domain02", "domain03", "domain04", "domain05", "domain06"));
        validationShouldThrowExceptionWithMsg(parameters, "You must specify 3 to 5 domains");
    }

    @Test
    public void specificShouldHaveBetweenThreeAndFiveDomains3() throws Exception {
        MarketReportParameters parameters = baseParams();
        Benchmarking benchmarking = Benchmarking.builder().benchmarkingKind(BenchmarkingKind.SPECIFIC).build();
        parameters.getComponents().setBenchmarking(benchmarking);

        benchmarking.setCompetitorsDomains(Arrays.asList("domain01", "domain02", "domain03"));
        validationShouldNotThrowExceptionWithMsg(parameters);
    }

    @Test
    public void specificShouldHaveBetweenThreeAndFiveDomains4() throws Exception {
        MarketReportParameters parameters = baseParams();
        Benchmarking benchmarking = Benchmarking.builder().benchmarkingKind(BenchmarkingKind.SPECIFIC).build();
        parameters.getComponents().setBenchmarking(benchmarking);

        benchmarking.setCompetitorsDomains(Arrays.asList("domain01", "domain02", "domain03", "domain04"));
        validationShouldNotThrowExceptionWithMsg(parameters);
    }

    @Test
    public void specificShouldHaveBetweenThreeAndFiveDomains5() throws Exception {
        MarketReportParameters parameters = baseParams();
        Benchmarking benchmarking = Benchmarking.builder().benchmarkingKind(BenchmarkingKind.SPECIFIC).build();
        parameters.getComponents().setBenchmarking(benchmarking);

        benchmarking.setCompetitorsDomains(Arrays.asList("domain01", "domain02", "domain03", "domain04", "domain05"));
        validationShouldNotThrowExceptionWithMsg(parameters);
    }

    @Test
    public void top5ShouldNotHaveDomains() throws Exception {
        MarketReportParameters parameters = baseParams();
        Benchmarking benchmarking = Benchmarking.builder().benchmarkingKind(BenchmarkingKind.TOP5)
            .competitorsDomains(Arrays.asList("domain1", "domain2")).build();
        parameters.getComponents().setBenchmarking(benchmarking);
        validationShouldThrowExceptionWithMsg(parameters, "You must specify domains of competitors only for SPECIFIC request");
    }

    private void validationShouldThrowExceptionWithMsg(MarketReportParameters parameters, String msg) {
        try {
            marketReportService.validateParams(parameters);
            fail("Validation exception should be thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(msg));
        }
    }

    private void validationShouldNotThrowExceptionWithMsg(MarketReportParameters parameters) {
        try {
            marketReportService.validateParams(parameters);
        } catch (Exception e) {
            fail("Validation exception should not  be thrown");
        }
    }

    private MarketReportParameters baseParams() {
        MarketReportParameters parameters = new MarketReportParameters();
        parameters.setRegions(singletonList(1L));
        parameters.setCategories(singletonList(1L));
        parameters.setLanguage(RU);
        parameters.setDomain("client.domain");
        return parameters;
    }
}
