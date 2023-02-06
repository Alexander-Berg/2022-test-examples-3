package ru.yandex.market.gutgin.tms.config;

import java.util.HashSet;
import java.util.Set;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.gutgin.tms.manager.cleanweb.CleanWebSkipper;
import ru.yandex.market.gutgin.tms.service.CwResultCorrectionService;
import ru.yandex.market.ir.autogeneration.common.helpers.BookCategoryHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration.common.util.StringUtil;
import ru.yandex.market.partner.content.common.db.dao.CwSkippedTicketDao;
import ru.yandex.market.partner.content.common.db.dao.CwStatsDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DcpPartnerPictureDao;
import ru.yandex.market.partner.content.common.service.ApplicationPropertyService;

/**
 * A complete configuration for some tests. Unlike imported from here
 * {@link TestCleanWebSubConfig}, this configuration is not meant to be reusable.
 * @author danfertev
 * @since 16.09.2019
 */
@Configuration
@Import(TestCleanWebSubConfig.class)
public class TestCleanWebConfig {

    // Only additional resources here that keep this configuration complete.
    // All real Clean Web resources go into the sub-configuration which we reuse.

    @Bean
    public CategoryDataKnowledgeMock categoryDataKnowledgeMock() {
        return new CategoryDataKnowledgeMock();
    }

    @Bean
    BookCategoryHelper bookCategoryHelper() {
        return new BookCategoryHelper();
    }

    @Bean
    public CategoryDataHelper categoryDataHelper(
            CategoryDataKnowledgeMock categoryDataKnowledgeMock,
            BookCategoryHelper bookCategoryHelper
    ) {
        return new CategoryDataHelper(categoryDataKnowledgeMock, bookCategoryHelper);
    }

    @Bean
    public CleanWebSkipper cleanWebSkipper(CwResultCorrectionService cwResultCorrectionService,
                                           CwSkippedTicketDao cwSkippedValidationsDao,
                                           CwStatsDao cwStatsDao,
                                           DcpPartnerPictureDao dcpPartnerPictureDao,
                                           @Value("${gg.fashionCategories:}") String fashionCategoryIds,
                                           @Value("${gg.shoesCategories:}") String shoesCategoriesIds,
                                           @Value("${gg.accessoriesAnyCategories:}") String accessoriesAnyCategoryIds,
                                           @Value("${gg.accessoriesJewelryCategories:}") String accessoriesJewelryCategoryIds) {
        Set<Long> fashionCategories = new HashSet<>();
        fashionCategories.addAll(StringUtil.splitIntoSet(fashionCategoryIds, ",", Long::parseLong));
        fashionCategories.addAll(StringUtil.splitIntoSet(shoesCategoriesIds, ",", Long::parseLong));
        fashionCategories.addAll(StringUtil.splitIntoSet(accessoriesAnyCategoryIds, ",", Long::parseLong));
        fashionCategories.addAll(StringUtil.splitIntoSet(accessoriesJewelryCategoryIds, ",", Long::parseLong));
        return new CleanWebSkipper(cwResultCorrectionService, cwSkippedValidationsDao, cwStatsDao, Mockito.mock(ApplicationPropertyService.class),
                dcpPartnerPictureDao, fashionCategories);
    }
}
