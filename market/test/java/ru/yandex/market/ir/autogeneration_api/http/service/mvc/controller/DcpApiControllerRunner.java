package ru.yandex.market.ir.autogeneration_api.http.service.mvc.controller;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryModelsHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration_api.http.service.mvc.bean.ContentDataPojo;
import ru.yandex.market.ir.autogeneration_api.http.service.mvc.bean.DcpCategoryPojo;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.excel.generator.CategoryParametersFormParser;
import ru.yandex.market.ir.excel.generator.CategoryParametersFormParserImpl;
import ru.yandex.market.mbo.export.CategoryModelsService;
import ru.yandex.market.mbo.export.CategoryModelsServiceStub;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.CategoryParametersServiceStub;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.CategorySizeMeasureServiceStub;
import ru.yandex.market.mbo.http.ModelStorageService;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;
import ru.yandex.market.partner.content.common.csku.judge.Judge;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Утилита (не авто-тест) для ручной проверки ручек autogeneration-api.
 */
@Ignore
public class DcpApiControllerRunner {
    @Test
    public void testContent() {
        DcpApiController dcpApiController =
                createSpringContext().getBean(DcpApiController.class);
        ContentDataPojo contentDataPojo = dcpApiController.getContentData(91491L, "14206636");

        assertThat(contentDataPojo).isNotNull();
    }

    @Test
    public void testCategory() {
        long categoryId = 13793401L;
        DcpApiController dcpApiController =
                createSpringContext().getBean(DcpApiController.class);
        DcpCategoryPojo dcpCategoryPojo = dcpApiController.getCategory(categoryId, null, "");

        assertThat(dcpCategoryPojo).isNotNull();
    }

    private static BeanFactory createSpringContext() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(LiveConfiguration.class);
        applicationContext.refresh();
        return applicationContext;
    }

    private static class LiveConfiguration {
        private static final String HTTP_EXPORTER_URL = "http://mbo-http-exporter.tst.vs.market.yandex.net:8084";
        private final String modelStorageUrl = "http://ag-mbo-card-api.tst.vs.market.yandex.net/modelStorage/";

        @Bean
        CategoryParametersFormParser categoryParametersFormParser() {
            return new CategoryParametersFormParserImpl(HTTP_EXPORTER_URL);
        }

        @Bean
        CategoryModelsService categoryModelsService() {
            CategoryModelsServiceStub categoryModelsServiceImpl = new CategoryModelsServiceStub();
            categoryModelsServiceImpl.setHost(HTTP_EXPORTER_URL + "/categoryModels/");
            return categoryModelsServiceImpl;
        }

        @Bean
        CategoryDataKnowledge categoryDataKnowledge(CategoryParametersService categoryParametersService,
                                                    CategorySizeMeasureService categorySizeMeasureService
        ) {
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
            categoryParametersService.setUserAgent("junit @beefeather");
            return categoryParametersService;
        }

        @Bean
        CategorySizeMeasureService categorySizeMeasureService() {
            CategorySizeMeasureServiceStub measureService = new CategorySizeMeasureServiceStub();
            measureService.setHost(HTTP_EXPORTER_URL + "/categorySizeMeasure/");
            return measureService;
        }

        @Bean
        DcpApiController dcpApiController(CategoryDataKnowledge categoryDataKnowledge,
                                          CategoryInfoProducer categoryInfoProducer,
                                          ModelStorageHelper modelStorageHelper,
                                          CategoryModelsHelper categoryModelsHelper) {
            return new DcpApiController(categoryDataKnowledge, categoryInfoProducer, modelStorageHelper,
                    categoryModelsHelper, new Judge(),
                    "testing");
        }

        @Bean
        CategoryInfoProducer categoryInfoProducer(CategoryDataKnowledge categoryDataKnowledge,
                                                  CategoryParametersFormParser categoryParametersFormParser) {
            return new CategoryInfoProducer(categoryDataKnowledge, categoryParametersFormParser);
        }

        @Bean(name = "model.storage.service.with.retry")
        ModelStorageService modelStorageService() {
            ModelStorageServiceStub result = new ModelStorageServiceStub();
            result.setHost(modelStorageUrl);
            return result;
        }

        @Bean(name = "model.storage.service.without.retry")
        ModelStorageService modelStorageWithoutRetryService() {
            ModelStorageServiceStub result = new ModelStorageServiceStub();
            result.setHost(modelStorageUrl);
            result.setTriesBeforeFail(1);
            return result;
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

        @Bean
        CategoryModelsHelper categoryModelsHelper(
                CategoryModelsService categoryModelsService
        ) {
            return new CategoryModelsHelper(categoryModelsService);
        }
    }
}
