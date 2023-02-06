package ru.yandex.market.psku.postprocessor.service.migration.convertor;

import Market.DataCamp.DataCampUnitedOffer;
import com.google.common.collect.ImmutableList;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.http.ServiceClient;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.P1toP2Converter;
import ru.yandex.market.mbo.export.CategoryModelsService;
import ru.yandex.market.mbo.export.CategoryModelsServiceStub;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.CategoryParametersServiceStub;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.CategorySizeMeasureServiceStub;
import ru.yandex.market.mbo.http.ModelStorageService;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestInterceptor;
import ru.yandex.market.request.trace.Module;
import ru.yandex.yql.YqlDriver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.yandex.market.psku.postprocessor.service.migration.convertor.Psku10toPsku20ConverterServiceTest.newUnitedOffer;

/**
 * Утилита (не авто-тест) для ручного запуска и проверки конвертера pSku.
 */
@Ignore
public class Psku10toPsku20ConverterRunner {

    static List<Long> skus = ImmutableList.of(
        101095068709L
    );

    @Test
    public void check() {
        System.out.println("ok");
    }

    @Test
    public void converterRunner() {
        BeanFactory context = createSpringContext();
        PskuConverterRunService service = context.getBean(PskuConverterRunService.class);
        service.run(false);
        ConvertedPskuGroupDaoMock daoMock = context.getBean(ConvertedPskuGroupDaoMock.class);
        System.out.println("total ok : " + daoMock.countOk());
        System.out.println("total errors : " + daoMock.countErrors());
        System.out.println("total : " + daoMock.countTotal());
    }

    @Test
    public void testSimple() {
        BeanFactory context = createSpringContext();
        List<DataCampUnitedOffer.UnitedOffer> offerList = skus.stream()
            .map(skuId -> newUnitedOffer(1, "123", skuId))
            .collect(Collectors.toList());
        int size = offerList.size();
        Psku10toPsku20ConverterService convertorService = context.getBean(Psku10toPsku20ConverterService.class);
        List<DataCampUnitedOffer.UnitedOffer> unitedOffersToConvert = offerList.subList(0, size);
        long timeMillis = System.currentTimeMillis();
        Map<Long, Integer> convertResult = convertorService.convert(unitedOffersToConvert, false, 16);
        System.out.println(((System.currentTimeMillis() - timeMillis) / 1000) + " seconds for convert for size " + size);
        System.out.println("Size of result: " + convertResult.size());
    }

    @Test
    public void testMultipleSkus() {
        Psku10toPsku20ConverterService service = createSpringContext().getBean(Psku10toPsku20ConverterService.class);
        // pModel 290377941 (hid=13277088) has 3 skus: 100641303341, 100641321444, 100641321445
        List<DataCampUnitedOffer.UnitedOffer> offersFirstBatch = ImmutableList.of(
            newUnitedOffer(1, "100", 100450034252L),
            newUnitedOffer(1, "200", 100641321444L)
            // don't put third sku 100641321445, simulating that its offer is in next batch
        );
        Map<Long, Integer> convertResult = service.convert(offersFirstBatch, false, 16);
        Assertions.assertThat(convertResult.size()).isEqualTo(3);
        Assertions.assertThat(convertResult.values()).containsExactlyInAnyOrder(290377941, 290377941, 290377941);
        Assertions.assertThat(convertResult.keySet())
            .containsExactlyInAnyOrder(100641303341L, 100641321444L, 100641321445L);
    }

    private static BeanFactory createSpringContext() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(LiveConfiguration.class);
        applicationContext.refresh();
        return applicationContext;
    }

    private static class LiveConfiguration {
        //        private static final String HTTP_EXPORTER_URL = "http://mbo-http-exporter.tst.vs.market.yandex
        //        .net:8084";
//        private static final String MODEL_STORAGE_URL = "http://ag-mbo-card-api.tst.vs.market.yandex
//        .net/modelStorage/";
        private static final String USER_AGENT = "AG psku converter @n-mago";

        private static final String HTTP_EXPORTER_URL = "http://mbo-http-exporter.yandex.net:8084";
        private static final String MODEL_STORAGE_URL = "http://mbo-card-api.http.yandex.net:33714/modelStorage/";
//        private static final String MODEL_STORAGE_URL = "http://ag-mbo-card-api.vs.market.yandex.net:33714/modelStorage/";


        @Bean
        JdbcTemplate yqlJdbcTemplate() {
            String yqlUsername = System.getProperty("user.name"); // username
            String ytCluster = "hahn";
            String yqlToken = getDevToken();
            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setDriverClassName(YqlDriver.class.getName());
            dataSource.setUrl("jdbc:yql://yql.yandex.net:443/" + ytCluster + "?syntaxVersion=1");
            dataSource.setUsername(yqlUsername);
            dataSource.setPassword(yqlToken);
            dataSource.setValidationQuery("select 1");
            return new JdbcTemplate(dataSource);
        }

        private String getDevToken() {
            String userHome = System.getProperty("user.home");
            File ytTokenFile = new File(userHome + "/.yql/token");
            if (!ytTokenFile.exists()) {
                throw new RuntimeException("Yql token not found, check file '" + ytTokenFile + "' is present");
            }
            try {
                return FileUtils.readFileToString(ytTokenFile, StandardCharsets.UTF_8.toString()).trim();
            } catch (IOException ioe) {
                throw new RuntimeException("Can't read yql token from file " + ytTokenFile, ioe);
            }
        }

        @Bean
        PppKeyValueStoreDaoMock pppKeyValueStoreDaoMock() {
            return new PppKeyValueStoreDaoMock();
        }

        @Bean
        PskuConverterRunService pskuConverterRunService(JdbcTemplate yqlJdbcTemplate,
                                                        Psku10toPsku20ConverterService psku10toPsku20ConverterService,
                                                        PppKeyValueStoreDaoMock pppKeyValueStoreDaoMock) {
            return new PskuConverterRunService(
                yqlJdbcTemplate,
                psku10toPsku20ConverterService,
                pppKeyValueStoreDaoMock,
                "//home/market/users/eyastrebov/psku10-for-convertation"
            );
        }

        @Bean
        ConvertedPskuGroupDaoMock convertedPskuGroupDaoMock() {
            return new ConvertedPskuGroupDaoMock();
        }

        @Bean
        P1toP2Converter p1toP2Converter(
            ModelStorageHelper modelStorageHelper
        ) {
            return new P1toP2Converter(modelStorageHelper);
        }

        @Bean
        Psku10toPsku20ConverterService psku10toPsku20ConverterService(ModelStorageHelper modelStorageHelper,
                                                                      CategoryDataKnowledge categoryDataKnowledge,
                                                                      ConvertedPskuGroupDaoMock convertedPskuGroupDaoMock,
                                                                      P1toP2Converter p1toP2Converter) {
            return new Psku10toPsku20ConverterService(modelStorageHelper, categoryDataKnowledge,
                convertedPskuGroupDaoMock, p1toP2Converter);
        }

        @Bean
        CategoryDataKnowledge categoryDataKnowledge(CategoryParametersService categoryParametersService,
                                                    CategorySizeMeasureService categorySizeMeasureService) {
            CategoryDataKnowledge categoryDataKnowledge = new CategoryDataKnowledge();
            categoryDataKnowledge.setCategoryDataRefreshersCount(1000000);
            categoryDataKnowledge.setCategoryParametersService(categoryParametersService);
            categoryDataKnowledge.setCategorySizeMeasureService(categorySizeMeasureService);
            categoryDataKnowledge.setCacheStrategy(CategoryDataKnowledge.CacheStrategy.ALWAYS_USE_CACHE);
            return categoryDataKnowledge;
        }

        @Bean
        CategoryParametersService categoryParametersService() {
            CategoryParametersServiceStub categoryParametersService = new CategoryParametersServiceStub();
            categoryParametersService.setHost(HTTP_EXPORTER_URL + "/categoryParameters/");
            categoryParametersService.setUserAgent(USER_AGENT);
            return categoryParametersService;
        }

        @Bean
        CategorySizeMeasureService categorySizeMeasureService() {
            CategorySizeMeasureServiceStub measureService = new CategorySizeMeasureServiceStub();
            measureService.setHost(HTTP_EXPORTER_URL + "/categorySizeMeasure/");
            return measureService;
        }

        @Bean(name = "model.storage.service.with.retry")
        ModelStorageService modelStorageService() {
            ModelStorageServiceStub result = new ModelStorageServiceStub();
            initServiceClient(result, MODEL_STORAGE_URL, Module.MBO_CARD_API);
            return result;
        }

        @Bean(name = "model.storage.service.without.retry")
        ModelStorageService modelStorageWithoutRetryService() {
            ModelStorageServiceStub result = new ModelStorageServiceStub();
            initServiceClient(result, MODEL_STORAGE_URL, Module.MBO_CARD_API);
            result.setTriesBeforeFail(1);
            return result;
        }

        @Bean
        CategoryModelsService categoryModelsService() {
            CategoryModelsServiceStub categoryModelsServiceImpl = new CategoryModelsServiceStub();
            categoryModelsServiceImpl.setHost(HTTP_EXPORTER_URL + "/categoryModels/");
            return categoryModelsServiceImpl;
        }

        @Bean
        ModelStorageHelper modelStorageHelper(
            @Qualifier("model.storage.service.with.retry") ModelStorageService modelStorageServiceWithRetry,
            @Qualifier("model.storage.service.without.retry") ModelStorageService modelStorageServiceWithoutRetry
        ) {
            return new ModelStorageHelper(
                modelStorageServiceWithRetry,
                modelStorageServiceWithoutRetry
            );
        }

        private void initServiceClient(ServiceClient serviceClient, String serviceHost, Module traceModule) {
            serviceClient.setUserAgent(USER_AGENT);
            serviceClient.setTriesBeforeFail(5);
            serviceClient.setSleepBetweenTries(200);
            serviceClient.setConnectionTimeoutMillis(30000);
            serviceClient.setHost(serviceHost);
            if (traceModule != null) {
                serviceClient.setHttpRequestInterceptor(new TraceHttpRequestInterceptor(traceModule));
                serviceClient.setHttpResponseInterceptor(new TraceMarketInterceptor());
            }
        }
    }
}
