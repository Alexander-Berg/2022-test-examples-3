package ru.yandex.market.gutgin.tms.config;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.gutgin.tms.service.CwResultCorrectionService;
import ru.yandex.market.gutgin.tms.service.datacamp.savemodels.update.MboPictureService;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.util.StringUtil;
import ru.yandex.market.partner.content.common.db.dao.SkipCwDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DcpPartnerPictureDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcCleanWebImageValidationDao;

/**
 * An partial configuration that comprises CW resources only, ready to be imported to
 * other bigger test configurations.
 * Note that this configuration is incomplete, notable because it depends on {@link CategoryDataHelper}.
 */
@Configuration
public class TestCleanWebSubConfig {

    @Value("${allow_cw_shop_ids:}") String allowCWShopIds;
    @Value("${use_allow_cw_shop_ids:false}") boolean useAllowCWShopIds;
    @Autowired
    CategoryDataKnowledge categoryDataKnowledge;

    @Value("${gg.autoPartsCategories:}")
    private String autoPartsCategories;

    @Bean
    public MboPictureService mboPictureService(DcpPartnerPictureDao dcpPartnerPictureDao,
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
            @Value("7811902,7812181") String ignoreEroticaCategoryIds
    ) {
        return new CwResultCorrectionService(
                StringUtil.splitIntoSet(ignoreEroticaCategoryIds, ",", Long::parseLong),
                Collections.emptySet(),
                Collections.singleton(12345L),
                Collections.singleton(12346L),
                Collections.singleton(12347L),
                Collections.singleton(12348L),
                Collections.singleton(12349L),
                true
        );
    }

}
