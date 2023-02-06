package ru.yandex.market.offers_draft;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.lang.NonNull;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.yt.indexer.YtFactory;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OffersDraftExecutorFunctionalTest extends FunctionalTest {

    private static final Logger log = LoggerFactory.getLogger(OffersDraftExecutor.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd_MM_yyyy");
    private static final String INSERT_METADATA_SQL = "" +
            "insert into shops_web.draft_offers_metadata(supplier_id, offers_count)" +
            "   values(:supplierId, :offersCount)";

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Test
    void testRetry() {
        NamedParameterJdbcTemplate yqlMock = mock(NamedParameterJdbcTemplate.class);
        RetryListener listener = mock(RetryListener.class);
        when(listener.open(any(), any())).thenReturn(true);
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.registerListener(listener);
        Map<String, String> parameters = getParameters();
        YtFactory ytFactory = mock(YtFactory.class);
        doReturn("hahn")
                .when(ytFactory).getYtCluster("stratocaster");
        doReturn("arnold")
                .when(ytFactory).getYtCluster("gibson");
        Map<String, YtTemplate> ytTemplateMap = new HashMap<>();
        YtTemplate mockYtTemplate1 = mock(YtTemplate.class);
        YtTemplate mockYtTemplate2 = mock(YtTemplate.class);
        ytTemplateMap.put("stratocaster", mockYtTemplate1);
        ytTemplateMap.put("gibson", mockYtTemplate2);
        OffersDraftParameters offersDraftParameters =
                new OffersDraftParameters(parameters, ytFactory, new HashSet<>(Arrays.asList("stratocaster", "gibson")));
        OffersDraftExecutor scheduler = new OffersDraftExecutor(
                yqlMock, retryTemplate, mock(NamedParameterJdbcTemplate.class), ytTemplateMap,
                offersDraftParameters, transactionTemplate);

        scheduler.initQueries();

        when(yqlMock.update(anyString(), any(Map.class)))
                .thenThrow(RuntimeException.class)
                .thenThrow(RuntimeException.class)
                .thenReturn(1)
                .thenReturn(1);

        when(yqlMock.query(anyString(), anyMap(), any(ResultSetExtractor.class)))
                .thenReturn(new HashMap<Long, Integer>());

        scheduler.doJob(mock(JobExecutionContext.class));

        verify(listener, times(2)).onError(any(), any(), any());
        verify(yqlMock, times(6)).update(anyString(), any(Map.class));

        verify(mockYtTemplate1, atLeastOnce()).runInYt(any());
        verify(mockYtTemplate2, atLeastOnce()).runInYt(any());
    }

    @Test
    public void testJob() {
        NamedParameterJdbcTemplate mockYqlTemplate = mockedYqlTemplate();
        YtTemplate mockYtTemplate = mockedYtTemplate(invocation -> {
            String recent = invocation.getArgument(1).toString();
            Assert.assertTrue(recent.endsWith("recent"));
            return null;
        });

        NamedParameterJdbcTemplate mockJdbcTemplate = mockJdbcTemplate();
        YtFactory ytFactory = mock(YtFactory.class);
        doReturn("hahn")
                .when(ytFactory).getYtCluster("stratocaster");
        Map<String, String> parameters = getParameters();
        Map<String, YtTemplate> ytTemplateMap = new HashMap<>();
        ytTemplateMap.put("stratocaster", mockYtTemplate);
        OffersDraftParameters offersDraftParameters =
                new OffersDraftParameters(parameters, ytFactory, new HashSet<>(Collections.singletonList("stratocaster")));
        OffersDraftExecutor offersDraftScheduler = new OffersDraftExecutor(
                mockYqlTemplate, mockJdbcTemplate, ytTemplateMap,
                offersDraftParameters, transactionTemplate);
        offersDraftScheduler.initQueries();
        offersDraftScheduler.doJob(mock(JobExecutionContext.class));

        verify(mockYtTemplate).runInYt(any());
        final String dateText = LocalDate.now(ZoneId.of("UTC")).format(FORMATTER);
        parameters.put("offersTable", String.format("`//b/%s/test1`", "stratocaster"));
        parameters.put("resultTable", String.format("`//result_%s`", dateText));
        parameters.put("metadataTable", String.format("`//metadata_%s`", dateText));
        String accountOffersQuery = loadParametrizedStubQuery("account_white_offers", "hahn", parameters);
        String metadataWhiteOffersQuery = loadParametrizedStubQuery("metadata_white_offers", "hahn", parameters);
        verify(mockYqlTemplate).update(eq(accountOffersQuery), anyMap());
        verify(mockYqlTemplate).update(eq(metadataWhiteOffersQuery), anyMap());
        verify(mockYqlTemplate).query(eq(loadParametrizedStubQuery("read_metadata", "hahn", parameters)), anyMap(),
                any(ResultSetExtractor.class));
        verify(mockJdbcTemplate).batchUpdate(any(String.class), any(SqlParameterSource[].class));
    }

    @Test
    public void testJobFail() {
        RetryTemplate retryTemplate = new RetryTemplate();
        TransactionTemplate spy = spy(transactionTemplate);
        doThrow(new RuntimeException("Just an exception."))
                .when(spy).execute(any());
        OffersDraftParameters offersDraftParameters = mock(OffersDraftParameters.class);
        doReturn(Set.of("stratocaster", "gibson"))
                .when(offersDraftParameters).getClusters();
        Map<String, YtTemplate> ytTemplateMap = new HashMap<>();
        ytTemplateMap.put("stratocaster", mock(YtTemplate.class));
        OffersDraftExecutor scheduler = new OffersDraftExecutor(
                mock(NamedParameterJdbcTemplate.class), retryTemplate,
                mock(NamedParameterJdbcTemplate.class), ytTemplateMap,
                offersDraftParameters, spy);

        final String expectedExceptionSubString =
                "Execution of offersDraftExecutor isn't success for all of clusters:";
        try {
            scheduler.doJob(mock(JobExecutionContext.class));
            Assert.fail("Exception hasn't been thrown!");
        } catch (RuntimeException ex) {
            log.info(expectedExceptionSubString);
            Assertions.assertTrue(ex.getMessage().contains(expectedExceptionSubString));
        }
    }

    @Test
    @DbUnitDataSet(after = "metadataInsertion.after.csv")
    public void testMetadataInsertion() {
        NamedParameterJdbcTemplate mockYqlTemplate = mockedYqlTemplateForMetadataInsertion();
        YtTemplate mockYtTemplate = mockedYtTemplate(invocation -> {
            String recent = invocation.getArgument(1).toString();
            Assert.assertTrue(recent.endsWith("recent"));
            return null;
        });

        YtFactory ytFactory = mock(YtFactory.class);
        doReturn("hahn")
                .when(ytFactory).getYtCluster("stratocaster");
        Map<String, String> parameters = getParameters();
        Map<String, YtTemplate> ytTemplateMap = new HashMap<>();
        ytTemplateMap.put("stratocaster", mockYtTemplate);
        OffersDraftParameters offersDraftParameters =
                new OffersDraftParameters(parameters, ytFactory, new HashSet<>(Collections.singletonList("stratocaster")));
        OffersDraftExecutor offersDraftScheduler = new OffersDraftExecutor(
                mockYqlTemplate, namedParameterJdbcTemplate, ytTemplateMap,
                offersDraftParameters, transactionTemplate);
        offersDraftScheduler.initQueries();
        offersDraftScheduler.doJob(mock(JobExecutionContext.class));

        final String dateText = LocalDate.now(ZoneId.of("UTC")).format(FORMATTER);
        parameters.put("offersTable", String.format("`//b/%s/test1`", "stratocaster"));
        parameters.put("resultTable", String.format("`//result_%s`", dateText));
        parameters.put("metadataTable", String.format("`//metadata_%s`", dateText));
        String accountOffersQuery = loadParametrizedStubQuery("account_white_offers", "hahn", parameters);
        String metadataWhiteOffersQuery = loadParametrizedStubQuery("metadata_white_offers", "hahn", parameters);
        verify(mockYqlTemplate).update(eq(accountOffersQuery), anyMap());
        verify(mockYqlTemplate).update(eq(metadataWhiteOffersQuery), anyMap());
        verify(mockYqlTemplate).query(eq(loadParametrizedStubQuery("read_metadata", "hahn", parameters)), anyMap(),
                any(ResultSetExtractor.class));
    }

    private YtTemplate mockedYtTemplate(Answer answer) {
        Yt ytMock = mock(Yt.class);
        Cypress cypressMock = mock(Cypress.class);
        doReturn(cypressMock)
                .when(ytMock).cypress();
        doAnswer(answer).when(cypressMock).link(any(), any());

        return spy(new YtTemplate(new YtCluster[]{
                new YtCluster(".", ytMock)
        }));
    }

    @NonNull
    private NamedParameterJdbcTemplate mockJdbcTemplate() {
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            SqlParameterSource[] sources = invocation.getArgument(1);
            Assertions.assertEquals(INSERT_METADATA_SQL, sql);
            SqlParameterSource source = sources[0];
            Assertions.assertEquals(1L, source.getValue("supplierId"));
            Assertions.assertEquals(1L, source.getValue("offersCount"));
            return null;
        }).when(namedParameterJdbcTemplate).batchUpdate(any(String.class), any(SqlParameterSource[].class));
        return namedParameterJdbcTemplate;
    }

    @NonNull
    private NamedParameterJdbcTemplate mockedYqlTemplateForMetadataInsertion() {
        NamedParameterJdbcTemplate mockYqlTemplate = mock(NamedParameterJdbcTemplate.class);
        doAnswer(invocation -> {
            ResultSetExtractor<Map<Long, Integer>> extractor = invocation.getArgument(2);
            AtomicBoolean val = new AtomicBoolean(true);
            ResultSet rs = mock(ResultSet.class);
            doAnswer(invocation1 -> {
                return val.get();
            }).when(rs).next();
            AtomicInteger counterOne = new AtomicInteger(0);
            AtomicInteger counterTwo = new AtomicInteger(0);
            doAnswer(invocation1 -> {
                if (counterOne.getAndIncrement() == 0) {
                    return 100000000001L;
                } else {
                    val.set(false);
                    return 100000000002L;
                }
            }).when(rs).getLong("supplier_id");
            doAnswer(invocation1 -> {
                if (counterTwo.getAndIncrement() == 0) {
                    return 1L;
                } else {
                    return 2L;
                }
            }).when(rs).getObject("count");
            return extractor.extractData(rs);
        }).when(mockYqlTemplate).query(anyString(), anyMap(), any(ResultSetExtractor.class));
        return mockYqlTemplate;
    }

    @Nonnull
    private NamedParameterJdbcTemplate mockedYqlTemplate() {
        NamedParameterJdbcTemplate mockYqlTemplate = mock(NamedParameterJdbcTemplate.class);
        doAnswer(invocation -> {
            ResultSetExtractor<Map<Long, Integer>> extractor = invocation.getArgument(2);
            AtomicBoolean val = new AtomicBoolean(true);
            ResultSet rs = mock(ResultSet.class);
            doAnswer(invocation1 -> {
                return val.get();
            }).when(rs).next();
            doAnswer(invocation1 -> {
                val.set(false);
                return 1L;
            }).when(rs).getLong("supplier_id");
            doReturn(1L)
                    .when(rs).getObject("count");
            Map<Long, Integer> metadataMap = extractor.extractData(rs);
            Assert.assertNotNull(metadataMap);
            Assert.assertTrue(metadataMap.containsKey(1L));
            Assert.assertTrue(metadataMap.containsValue(1l));
            return metadataMap;
        }).when(mockYqlTemplate).query(anyString(), anyMap(), any(ResultSetExtractor.class));
        return mockYqlTemplate;
    }

    public String loadStubQuery(String queryName, String clusterName) {
        DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd_MM_yyyy");
        String dateText = LocalDate.now(ZoneId.of("UTC")).format(FORMATTER);
        InputStream stream = OffersDraftExecutorFunctionalTest
                .class
                .getClassLoader()
                .getResourceAsStream(String.format("ru/yandex/market/offers_draft/%s_%s.yql", queryName, clusterName));

        if (stream == null) {
            throw new RuntimeException(
                    String.format("Cannot find yql file \"%s_%s.yql\" in the same package", queryName, clusterName));
        }

        try {
            return new String(stream.readAllBytes(), Charset.defaultCharset())
                    .replace(":date", dateText);
        } catch (IOException e) {
            log.error(String.format("Cannot read yql file \"%s_%s.yql\": ", queryName, clusterName), e);
            throw new RuntimeException(e);
        }
    }

    public String loadParametrizedStubQuery(String queryName, String clusterName, Map<String, String> parameters) {
        InputStream stream = OffersDraftExecutorFunctionalTest
                .class
                .getClassLoader()
                .getResourceAsStream(String.format("ru/yandex/market/offers_draft/%s.yql", queryName));

        if (stream == null) {
            throw new RuntimeException(
                    String.format("Cannot find yql file \"%s_%s.yql\" in the same package", queryName, clusterName));
        }

        try {
            String query = new String(stream.readAllBytes(), Charset.defaultCharset());
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                query = query.replace(":" + entry.getKey(), clusterName + "." + entry.getValue());
            }
            return query;
        } catch (IOException e) {
            log.error(String.format("Cannot read yql file \"%s_%s.yql\": ", queryName, clusterName), e);
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    private static Map<String, String> getParameters() {
        return new HashMap<>() {{
            put("categoriesTable", "`//a/test2`");
            put("offersTable", "`//b/%s/test1`");
            put("resultTable", "`//result_%s`");
            put("mappingTable", "`//shop_mapping`");
            put("metadataTable", "`//metadata_%s`");
            put("mbocOffersTable", "`//mboc_offers`");
            put("mboSkuTable", "`//mbo_sku`");
            put("mstatShops", "`//mstat_shops`");
            put("mstatSuppliers", "`//mstat_suppliers`");
            put("mstatDatasources", "`//mstat_datasources`");
            put("mstatParams", "`//mstat_params`");
            put("partnerInfoTable", "`//partner_info`");
            put("shopCRMTable", "`//shop_crm`");
            put("organizationInfoTable", "`//organization_info`");
            put("suppliersFullTable", "`//suppliers_full`");
            put("extendedRequestHistoryTable", "`//extended_request_history`");
            put("regionsTable", "`//regionsTable`");
            put("shopsExcludedTable", "`//shops_excluded`");
        }};
    }
}
