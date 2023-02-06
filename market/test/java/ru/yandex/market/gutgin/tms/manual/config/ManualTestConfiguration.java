package ru.yandex.market.gutgin.tms.manual.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.gutgin.tms.service.CwResultCorrectionService;
import ru.yandex.market.gutgin.tms.service.datacamp.savemodels.update.MboPictureService;
import ru.yandex.market.gutgin.tms.utils.ParameterCreator;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.P1toP2Converter;
import ru.yandex.market.ir.autogeneration.common.util.StringUtil;
import ru.yandex.market.ir.excel.generator.CategoryParametersFormParser;
import ru.yandex.market.ir.excel.generator.CategoryParametersFormParserImpl;
import ru.yandex.market.partner.content.common.config.CommonTestConfig;
import ru.yandex.market.partner.content.common.db.dao.SkipCwDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DcpPartnerPictureDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcCleanWebImageValidationDao;

@Configuration
@Import({
    ManualExternalProductionCategoryServices.class,
    ManualExternalProductionModelsServices.class,
    ManualExternalProductionMappingServices.class,
    CommonTestConfig.class
})
public class ManualTestConfiguration {
    @Value("${allow_cw_shop_ids:}") String allowCWShopIds;
    @Value("${use_allow_cw_shop_ids:false}") boolean useAllowCWShopIds;
    @Value("${gg.bookSupportEnabled:false}") boolean bookSupportEnabled;

    @Value("${gg.autoPartsCategories:}")
    private String autoPartsCategories;

    @Bean
    public ParameterCreator parameterCreator() {
        return new ParameterCreator();
    }

    @Bean
    public MboPictureService mboPictureService(
        DcpPartnerPictureDao dcpPartnerPictureDao,
        GcCleanWebImageValidationDao gcCleanWebImageValidationDao,
        CwResultCorrectionService cwResultCorrectionService,
        SkipCwDao skipCwDao
    ) {
        return new MboPictureService(dcpPartnerPictureDao,
            gcCleanWebImageValidationDao,
            skipCwDao,
            StringUtil.splitIntoSet(allowCWShopIds, ",", Integer::parseInt),
            useAllowCWShopIds,
            StringUtil.splitIntoSet(autoPartsCategories, ",", Long::parseLong),
                cwResultCorrectionService);
    }

    @Bean
    public CwResultCorrectionService cwImageResultsCorrectionService(
            @Value("${gg.ignoreEroticaCategoryIds:}") String ignoreEroticaCategoryIds,
            @Value("${gg.ignoreLawViolationCategoryIds:}") String ignoreLawViolationCategoryIds,
            @Value("${gg.intimFlowCategoryIds:}") String intimFlowCategoryIds,
            @Value("${gg.fashionCategories:}") String fashionCategoryIds,
            @Value("${gg.shoesCategories:}") String shoesCategoriesIds,
            @Value("${gg.accessoriesAnyCategories:}") String accessoriesAnyCategoryIds,
            @Value("${gg.accessoriesJewelryCategories:}") String accessoriesJewelryCategoryIds
    ) {
        return new CwResultCorrectionService(
                StringUtil.splitIntoSet(ignoreEroticaCategoryIds, ",", Long::parseLong),
                StringUtil.splitIntoSet(ignoreLawViolationCategoryIds, ",", Long::parseLong),
                StringUtil.splitIntoSet(intimFlowCategoryIds, ",", Long::parseLong),
                StringUtil.splitIntoSet(fashionCategoryIds, ",", Long::parseLong),
                StringUtil.splitIntoSet(shoesCategoriesIds, ",", Long::parseLong),
                StringUtil.splitIntoSet(accessoriesAnyCategoryIds, ",", Long::parseLong),
                StringUtil.splitIntoSet(accessoriesJewelryCategoryIds, ",", Long::parseLong),
                bookSupportEnabled
        );
    }

    @Bean
    CategoryParametersFormParser categoryParametersFormParser(
        @Value("${mbo.http-exporter.url}") String exporterUrl
    ) {
        return new CategoryParametersFormParserImpl(exporterUrl);
    }

    @Bean
    P1toP2Converter p1toP2Converter(ModelStorageHelper modelStorageHelper) {
        return new P1toP2Converter(modelStorageHelper);
    }
}
