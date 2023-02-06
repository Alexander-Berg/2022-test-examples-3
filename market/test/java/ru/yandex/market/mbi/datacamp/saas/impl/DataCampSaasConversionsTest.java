package ru.yandex.market.mbi.datacamp.saas.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampOfferMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.filter.HidingSource;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.DisabledBySourceAttribute;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferFilter;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;
import ru.yandex.market.saas.search.keys.SaasSearchAttribute;

/**
 * Date: 12.02.2021
 * Project: arcadia-market_mbi_datacamp-client
 *
 * @author alexminakov
 */
class DataCampSaasConversionsTest {

    @Test
    @DisplayName("Проверка конвертации для аттрибут типа скрытия по источнику")
    void fromSearchBusinessOffersRequest_disabledBySource_successful() {
        SearchBusinessOffersRequest request = SearchBusinessOffersRequest.builder()
                .setBusinessId(100L)
                .setPartnerId(100L)
                .setPageRequest(SeekSliceRequest.firstN(1))
                .addDisabledFlags(List.of(HidingSource.PUSH_PARTNER_API, HidingSource.MARKET_PRICELABS))
                .build();
        SaasOfferFilter saasOfferFilter = DataCampSaasConversions.fromSearchBusinessOffersRequest(request);

        Map<SaasSearchAttribute, ? extends Collection<String>> filtersMap = saasOfferFilter.getFiltersMap();
        Assertions.assertEquals(3, filtersMap.size());
        Assertions.assertTrue(filtersMap
                .containsKey(new DisabledBySourceAttribute(100L, DataCampOfferMeta.DataSource.PUSH_PARTNER_API))
        );

        String disabledFlags = String.join(",",
                filtersMap.get(new DisabledBySourceAttribute(100L, DataCampOfferMeta.DataSource.PUSH_PARTNER_API))
        );
        Assertions.assertEquals("10,3", disabledFlags);
    }
}
