package ru.yandex.market.rg.asyncreport.content.mapper;

import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.filter.PartnerSupplyPlan;
import ru.yandex.market.mbi.datacamp.model.search.filter.ResultContentStatus;
import ru.yandex.market.mbi.datacamp.model.search.filter.ResultOfferStatus;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;
import ru.yandex.market.rg.asyncreport.content.PartnerContentParams;
import ru.yandex.market.rg.config.FunctionalTest;

/**
 * Тесты для {@link ContentTemplateGeneratorDataCampRequestMapper}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ContentTemplateGeneratorDataCampRequestMapperTest extends FunctionalTest {

    @Autowired
    private ContentTemplateGeneratorDataCampRequestMapper contentTemplateGeneratorDataCampRequestMapper;

    @Test
    @DisplayName("Конвертация PartnerContentParams в SearchBusinessOffersRequest")
    void testConvert() {
        PartnerContentParams params = new PartnerContentParams();
        params.setEntityId(1001L);
        params.setCategoryIds(Set.of(100L, 101L));
        params.setTemplateCategoryId(1004);
        params.setVendors(Set.of("vendor1", "vendor2"));
        params.setSupplyPlans(Set.of(PartnerSupplyPlan.WILL_SUPPLY));
        params.setResultOfferStatuses(Set.of(ResultOfferStatus.NOT_PUBLISHED_CHECKING));
        params.setResultContentStatuses(Set.of(ResultContentStatus.HAS_CARD_MARKET));

        SearchBusinessOffersRequest actual = contentTemplateGeneratorDataCampRequestMapper.convert(2001L, 1001L, params);

        SearchBusinessOffersRequest expected = SearchBusinessOffersRequest.builder()
                .setBusinessId(2001L)
                .setPartnerId(1001L)
                .setPageRequest(SeekSliceRequest.firstN(200))
                .addVendors(Set.of("vendor1", "vendor2"))
                .addCategoryIds(Set.of(100L, 101L))
                .addMarketCategoryId(1004L)
                .addResultOfferStatuses(Set.of(ResultOfferStatus.NOT_PUBLISHED_CHECKING))
                .addResultContentStatuses(Set.of(ResultContentStatus.HAS_CARD_MARKET))
                .addSupplyPlan(PartnerSupplyPlan.WILL_SUPPLY)
                .setWithRetry(true)
                .setAllowModelCreateUpdate(true)
                .build();

        Assertions.assertThat(actual)
                .isEqualTo(expected);
    }
}
