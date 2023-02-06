package ru.yandex.market.gutgin.tms.manual.config;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.gutgin.tms.service.goodcontent.ParamValueHelper;
import ru.yandex.market.gutgin.tms.utils.ParameterCreator;
import ru.yandex.market.gutgin.tms.utils.goodcontent.GoodParameterCreator;
import ru.yandex.market.http.ServiceClient;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.BookCategoryHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.excel.generator.CategoryParametersFormParser;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.CategoryParametersServiceStub;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.CategorySizeMeasureServiceStub;
import ru.yandex.market.partner.content.common.csku.KnownTags;
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestInterceptor;
import ru.yandex.market.request.httpclient.trace.TraceHttpResponseInterceptor;
import ru.yandex.market.request.trace.Module;

@Configuration
public class ManualExternalProductionCategoryServices {

    @Value("${mbo.http-exporter.url}/categoryParameters/")
    String categoryParametersServiceHost;
    @Value("${mbo.http-exporter.url}/categoryModels/")
    String categoryModelsServiceHost;
    @Value("${mbo.http-exporter.url}/categorySizeMeasure/")
    String categorySizeMeasureServiceHost;
    @Value("${user.agent}")
    String defaultUserAgent;
    int defaultTriesBeforeFail = 1;
    int defaultSleepBetweenTries = 200;
    int defaultConnectionTimeoutMillis = 300;

    @Bean
    BookCategoryHelper bookCategoryHelper() {
        return new BookCategoryHelper();
    }

    @Bean
    CategoryParametersService categoryParametersService() {
        CategoryParametersServiceStub categoryParametersService = new CategoryParametersServiceStub();
        categoryParametersService.setHost(categoryParametersServiceHost);
        categoryParametersService.setTriesBeforeFail(defaultTriesBeforeFail);
        categoryParametersService.setSleepBetweenTries(defaultSleepBetweenTries);
        categoryParametersService.setConnectionTimeoutMillis(defaultConnectionTimeoutMillis);
        initServiceClient(categoryParametersService, Module.MBO_HTTP_EXPORTER);

        return categoryParametersService;
    }

    @Bean
    CategoryDataKnowledge categoryDataKnowledge(
        CategoryParametersService categoryParametersService,
        CategorySizeMeasureService categorySizeMeasureService,
        @Value("${categoryDataRefreshersCount:1}") int categoryDataRefreshersCount
    ) {
        CategoryDataKnowledge categoryDataKnowledge = new CategoryDataKnowledge();
        categoryDataKnowledge.setCategoryParametersService(categoryParametersService);
        categoryDataKnowledge.setCategoryDataRefreshersCount(categoryDataRefreshersCount);
        categoryDataKnowledge.setCategorySizeMeasureService(categorySizeMeasureService);
        categoryDataKnowledge.setTagsToProcess(Set.of(KnownTags.MATERIAL.getName()));
        return categoryDataKnowledge;
    }

    @Bean
    CategorySizeMeasureService categorySizeMeasureService() {
        CategorySizeMeasureServiceStub categorySizeMeasureService = new CategorySizeMeasureServiceStub();
        initServiceClient(categorySizeMeasureService, Module.MBO_HTTP_EXPORTER);

        categorySizeMeasureService.setTriesBeforeFail(defaultTriesBeforeFail);
        categorySizeMeasureService.setSleepBetweenTries(defaultSleepBetweenTries);
        categorySizeMeasureService.setConnectionTimeoutMillis(defaultConnectionTimeoutMillis);
        categorySizeMeasureService.setHost(categorySizeMeasureServiceHost);
        return categorySizeMeasureService;
    }

    @Bean
    CategoryDataHelper categoryDataHelper(CategoryDataKnowledge categoryDataKnowledge,
                                          BookCategoryHelper bookCategoryHelper) {
        return new CategoryDataHelper(categoryDataKnowledge, bookCategoryHelper);
    }

    private void initServiceClient(ServiceClient serviceClient, Module traceModule) {
        serviceClient.setUserAgent(defaultUserAgent);
        if (traceModule != null) {
            serviceClient.setHttpRequestInterceptor(new TraceHttpRequestInterceptor(traceModule));
            serviceClient.setHttpResponseInterceptor(new TraceHttpResponseInterceptor());
        }
    }


    @Bean
    CategoryInfoProducer categoryInfoProducer(
        CategoryDataKnowledge categoryDataKnowledge,
        CategoryParametersFormParser categoryParametersFormParser
    ) {
        return new CategoryInfoProducer(
            categoryDataKnowledge, categoryParametersFormParser
        );
    }

    @Bean
    public GoodParameterCreator goodParameterCreator(
        ParameterCreator parameterCreator
    ) {
        return new GoodParameterCreator(parameterCreator);
    }


    @Bean
    ParamValueHelper paramValueHelper(
        CategoryDataHelper categoryDataHelper,
        GoodParameterCreator goodParameterCreator
    ) {
        return new ParamValueHelper(
            categoryDataHelper,
            goodParameterCreator
        );
    }
}
