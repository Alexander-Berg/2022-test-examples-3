package ru.yandex.market.gutgin.tms.config;

import java.util.Collections;
import java.util.Set;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.gutgin.tms.service.CwResultCorrectionService;
import ru.yandex.market.gutgin.tms.service.SskuLockService;
import ru.yandex.market.gutgin.tms.service.datacamp.scheduling.FastOffersProcessingStrategy;
import ru.yandex.market.gutgin.tms.service.datacamp.scheduling.OfferProcessingStrategy;
import ru.yandex.market.gutgin.tms.service.goodcontent.ParamValueHelper;
import ru.yandex.market.gutgin.tms.utils.ParameterCreator;
import ru.yandex.market.gutgin.tms.utils.goodcontent.GoodParameterCreator;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.BookCategoryHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.util.StringUtil;
import ru.yandex.market.ir.autogeneration_api.http.service.FormalizerServiceMock;
import ru.yandex.market.ir.autogeneration_api.http.service.UltraControllerServiceMock;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.excel.generator.CategoryParametersFormParser;
import ru.yandex.market.ir.http.MarkupService;
import ru.yandex.market.ir.http.MarkupServiceStub;
import ru.yandex.market.mbo.export.CategoryModelsService;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.CategoryParametersServiceStub;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.CategorySizeMeasureServiceStub;
import ru.yandex.market.mbo.http.ModelStorageService;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;
import ru.yandex.market.partner.content.common.config.TestDataBaseConfiguration;
import ru.yandex.market.partner.content.common.csku.KnownTags;
import ru.yandex.market.partner.content.common.db.dao.FileDataProcessRequestDao;
import ru.yandex.market.partner.content.common.db.dao.FileProcessMessageService;
import ru.yandex.market.partner.content.common.db.dao.ProtocolMessageDao;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.dao.SskuLockDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcTicketProcessDao;
import ru.yandex.market.partner.content.common.mocks.CategoryParametersFormParserMock;
import ru.yandex.market.partner.content.common.partner.content.SourceController;
import ru.yandex.market.partner.content.common.service.mappings.PartnerShopService;

import static ru.yandex.market.gutgin.tms.config.OfferSchedulingConfig.OFFER_INFO_IS_NEW_PREDICATE;

@SuppressWarnings("MagicNumber")
@Configuration
@Import({
        TestDataBaseConfiguration.class,
        CommonDaoConfig.class,
        OfferSchedulingConfig.class
})
public class TestServiceConfig {

    public static final long FASHION_CATEGORY_ID = 12346L;
    public static final long SHOES_CATEGORY_ID = 12347L;
    public static final long INTIM_CATEGORY_ID = 12345L;
    public static final long ACCESSORIES_ANY_CATEGORY_ID = 12348L;
    public static final long ACCESSORIES_JEWELRY_CATEGORY_ID = 12349L;

    @Bean
    ModelStorageService modelStorageService() {
        ModelStorageServiceStub result = new ModelStorageServiceStub();
        result.setHost("http://mbo01et.market.yandex.net:33402/modelStorage/");
        result.setConnectionTimeoutMillis(300000);
        result.setTriesBeforeFail(1);
        return result;
    }

    @Bean
    CategoryParametersService categoryParametersService() {
        CategoryParametersServiceStub categoryParametersService = new CategoryParametersServiceStub();
        categoryParametersService.setHost("http://mbo-http-exporter.tst.vs.market.yandex.net:8084/categoryParameters/");
        categoryParametersService.setConnectionTimeoutMillis(300000);
        categoryParametersService.setTriesBeforeFail(1);

        return categoryParametersService;
    }

    @Bean
    MarkupService markupService(
    ) {
        MarkupServiceStub markupService = new MarkupServiceStub();
        markupService.setHost("http://cs-markup-worker01vt.market.yandex.net:34535/");
        return markupService;
    }

    @Bean
    CategoryDataKnowledge categoryDataKnowledge(
            CategoryParametersService categoryParametersService
    ) {
        CategoryDataKnowledge categoryDataKnowledge = new CategoryDataKnowledge();
        categoryDataKnowledge.setCategoryParametersService(categoryParametersService);
        categoryDataKnowledge.setCategoryDataRefreshersCount(1);
        categoryDataKnowledge.setTagsToProcess(Set.of(KnownTags.MATERIAL.getName()));

        return categoryDataKnowledge;
    }

    @Bean
    BookCategoryHelper bookCategoryHelper() {
        return new BookCategoryHelper();
    }

    @Bean
    CategoryDataHelper categoryDataHelper(CategoryDataKnowledge categoryDataKnowledge,
                                          BookCategoryHelper bookCategoryHelper) {
        return new CategoryDataHelper(categoryDataKnowledge, bookCategoryHelper);
    }

    @Bean
    ModelStorageHelper modelStorageHelper(ModelStorageService modelStorageService) {
        return new ModelStorageHelper(modelStorageService, modelStorageService);
    }

    @Bean
    CategoryModelsService categoryModelsService() {
        return new CategoryModelsServiceMock();
    }

    @Bean
    FileProcessMessageService fileProcessMessageService(
            @Qualifier("jooq.config.configuration") org.jooq.Configuration configuration,
            ProtocolMessageDao protocolMessageDao
    ) {
        return new FileProcessMessageService(configuration, protocolMessageDao);
    }

    @Bean
    CategoryParametersFormParser categoryParametersFormParser(
    ) {
        return new CategoryParametersFormParserMock();
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
    @Lazy
    CategorySizeMeasureService categorySizeMeasureService() {
        CategorySizeMeasureServiceStub categorySizeMeasureService = new CategorySizeMeasureServiceStub();

        categorySizeMeasureService.setHost(
                "http://mbo-http-exporter.tst.vs.market.yandex.net:8084/categorySizeMeasure/"
        );
        categorySizeMeasureService.setConnectionTimeoutMillis(300000);
        categorySizeMeasureService.setTriesBeforeFail(1);
        return categorySizeMeasureService;
    }

    @Bean
    PartnerShopService partnerShopService(SourceDao sourceDao,
                                          FileDataProcessRequestDao fileDataProcessRequestDao,
                                          GcTicketProcessDao gcTicketProcessDao) {
        return new PartnerShopService(sourceDao, fileDataProcessRequestDao, gcTicketProcessDao);
    }

    @Bean
    public ParameterCreator parameterCreator() {
        return new ParameterCreator();
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

    @Bean
    SskuLockDao sskuLockDao(@Qualifier("jooq.config.configuration") org.jooq.Configuration configuration) {
        return new SskuLockDao(configuration);
    }

    @Bean
    SskuLockService sskuLockService(SskuLockDao sskuLockDao,
                                    GcSkuTicketDao gcSkuTicketDao) {
        return new SskuLockService(sskuLockDao, gcSkuTicketDao);
    }

    @Bean
    SourceController sourceController(SourceDao sourceDao) {
        return new SourceController(sourceDao);
    }

    @Bean
    FormalizerServiceMock formalizerServiceMock() {
        return new FormalizerServiceMock();
    }

    @Bean
    UltraControllerServiceMock ultraControllerServiceMock() {
        return new UltraControllerServiceMock();
    }

    @Bean
    public JdbcTemplate yqlJdbcTemplate() {
        return Mockito.mock(JdbcTemplate.class);
    }

    @Bean
    public CwResultCorrectionService cwImageResultsCorrectionService(
            @Value("7811902,7812181") String ignoreEroticaCategoryIds
    ) {
        return new CwResultCorrectionService(
                StringUtil.splitIntoSet(ignoreEroticaCategoryIds, ",", Long::parseLong),
                Collections.emptySet(),
                Collections.singleton(INTIM_CATEGORY_ID),
                Collections.singleton(FASHION_CATEGORY_ID),
                Collections.singleton(SHOES_CATEGORY_ID),
                Collections.singleton(ACCESSORIES_ANY_CATEGORY_ID),
                Collections.singleton(ACCESSORIES_JEWELRY_CATEGORY_ID),
                true
        );
    }

    @Bean
    OfferProcessingStrategy fastCreateStrategy() {
        return new FastOffersProcessingStrategy(
                OFFER_INFO_IS_NEW_PREDICATE,
                OfferProcessingStrategy.Priority.HIGH
        );
    }

    @Bean
    OfferProcessingStrategy fastEditStrategy() {
        return new FastOffersProcessingStrategy(
                OFFER_INFO_IS_NEW_PREDICATE.negate(),
                OfferProcessingStrategy.Priority.DEFAULT);
    }
}
