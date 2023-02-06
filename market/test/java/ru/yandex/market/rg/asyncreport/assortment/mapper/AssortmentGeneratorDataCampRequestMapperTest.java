package ru.yandex.market.rg.asyncreport.assortment.mapper;

import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.filter.PartnerContentStatus;
import ru.yandex.market.mbi.datacamp.model.search.filter.PartnerSupplyPlan;
import ru.yandex.market.mbi.datacamp.model.search.filter.ResultContentStatus;
import ru.yandex.market.mbi.datacamp.model.search.filter.ResultOfferStatus;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;
import ru.yandex.market.rg.asyncreport.assortment.model.AssortmentParams;
import ru.yandex.market.rg.config.FunctionalTest;

/**
 * Тесты для {@link AssortmentGeneratorDataCampRequestMapper}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class AssortmentGeneratorDataCampRequestMapperTest extends FunctionalTest {

    @Autowired
    private AssortmentGeneratorDataCampRequestMapper assortmentGeneratorDataCampRequestMapper;

    @Test
    @DisplayName("Конвертация AssortmentParams в SearchBusinessOffersRequest для выгрузки текущего ассортимента")
    void testConvertForCurrentPlacement() {
        AssortmentParams params = getCommonParamsWithResultStatuses();

        SearchBusinessOffersRequest actual = assortmentGeneratorDataCampRequestMapper.convert(2001L, 1001L, params);

        SearchBusinessOffersRequest expected = SearchBusinessOffersRequest.builder()
                .setBusinessId(2001L)
                .setPartnerId(1001L)
                .setPageRequest(SeekSliceRequest.firstN(200))
                .setText("query_test*")
                .addVendors(Set.of("vendor1", "vendor2"))
                .addCategoryIds(Set.of(100L, 101L))
                .addResultOfferStatuses(Set.of(ResultOfferStatus.NOT_PUBLISHED_CHECKING))
                .addResultContentStatuses(Set.of(ResultContentStatus.HAS_CARD_MARKET))
                .addSupplyPlan(PartnerSupplyPlan.WILL_SUPPLY)
                .setWithRetry(true)
                .addVerdicts(Set.of(1111L, 2222L))
                .build();

        Assertions.assertThat(actual)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Конвертация AssortmentParams в SearchBusinessOffersRequest для миграции офферов")
    void testConvertForMigration() {
        AssortmentParams params = getCommonParamsWithResultStatuses();

        params.setPrefillWithServiceId(9001L);
        params.setIncludeServiceIds(Set.of(123L, 234L));
        params.setExcludeServiceIds(Set.of(987L, 876L));

        SearchBusinessOffersRequest actual = assortmentGeneratorDataCampRequestMapper.convert(2001L, 1001L, params);

        SearchBusinessOffersRequest expected = SearchBusinessOffersRequest.builder()
                .setBusinessId(2001L)
                .setPrefillWithServiceId(9001L)
                .addIncludePartnerIds(Set.of(123L, 234L))
                .addExcludePartnerIds(Set.of(987L, 876L))
                .setPageRequest(SeekSliceRequest.firstN(200))
                .setText("query_test*")
                .addVendors(Set.of("vendor1", "vendor2"))
                .addCategoryIds(Set.of(100L, 101L))
                .setWithRetry(true)
                .build();

        Assertions.assertThat(actual)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Конвертация AssortmentOfferFilterParams в SearchBusinessOffersRequest со старыми статусами")
    void testConvertWithOldStatuses() {
        AssortmentParams params = getCommonParams();
        params.setContentStatusesPartner(Set.of(PartnerContentStatus.AVAILABLE));

        SearchBusinessOffersRequest actual = assortmentGeneratorDataCampRequestMapper.convert(2001L, 1001L, params);

        SearchBusinessOffersRequest expected = SearchBusinessOffersRequest.builder()
                .setBusinessId(2001L)
                .setPartnerId(1001L)
                .setPageRequest(SeekSliceRequest.firstN(200))
                .setText("query_test*")
                .addVendors(Set.of("vendor1", "vendor2"))
                .addCategoryIds(Set.of(100L, 101L))
                .addContentStatusesPartner(Set.of(PartnerContentStatus.AVAILABLE))
                .addSupplyPlan(PartnerSupplyPlan.WILL_SUPPLY)
                .setWithRetry(true)
                .build();

        Assertions.assertThat(actual)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Конвертация AssortmentOfferFilterParams в SearchBusinessOffersRequest с интегральным статусом")
    void testConvertWithNewStatuses() {
        AssortmentParams params = getCommonParamsWithResultStatuses();

        SearchBusinessOffersRequest actual = assortmentGeneratorDataCampRequestMapper.convert(2001L, 1001L, params);

        SearchBusinessOffersRequest expected = SearchBusinessOffersRequest.builder()
                .setBusinessId(2001L)
                .setPartnerId(1001L)
                .setPageRequest(SeekSliceRequest.firstN(200))
                .setText("query_test*")
                .addVendors(Set.of("vendor1", "vendor2"))
                .addCategoryIds(Set.of(100L, 101L))
                .addResultOfferStatuses(Set.of(ResultOfferStatus.NOT_PUBLISHED_CHECKING))
                .addResultContentStatuses(Set.of(ResultContentStatus.HAS_CARD_MARKET))
                .addSupplyPlan(PartnerSupplyPlan.WILL_SUPPLY)
                .setWithRetry(true)
                .addVerdicts(Set.of(1111L,2222L))
                .build();

        Assertions.assertThat(actual)
                .isEqualTo(expected);
    }

    private AssortmentParams getCommonParamsWithResultStatuses() {
        AssortmentParams params = getCommonParams();
        params.setResultOfferStatuses(Set.of(ResultOfferStatus.NOT_PUBLISHED_CHECKING));
        params.setResultContentStatuses(Set.of(ResultContentStatus.HAS_CARD_MARKET));
        params.setVerdicts(Set.of(1111L, 2222L));
        return params;
    }

    private AssortmentParams getCommonParams() {
        AssortmentParams params = new AssortmentParams();
        params.setEntityId(1001L);
        params.setCategoryIds(Set.of(100L, 101L));
        params.setVendors(Set.of("vendor1", "vendor2"));
        params.setSupplyPlans(Set.of(PartnerSupplyPlan.WILL_SUPPLY));
        params.setQuery("query_test");

        return params;
    }
}
