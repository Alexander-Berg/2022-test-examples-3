package ru.yandex.market.logistics.management.client.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectToQueryParamsConverterTest {

    @Test
    void convert() {
        SearchPartnerFilter filter = SearchPartnerFilter.builder()
            .setTypes(ImmutableSet.of(PartnerType.DELIVERY, PartnerType.SORTING_CENTER))
            .setMarketIds(ImmutableSet.of(12L, 23L))
            .setPlatformClientIds(ImmutableSet.of(34L, 45L))
            .setStatuses(ImmutableSet.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING))
            .setIds(ImmutableSet.of(1L, 45L, 12L))
            .build();

        assertThat(ObjectToQueryParamsConverter.convert(filter))
            .isNotNull()
            .hasSize(5)
            .containsEntry("types", ImmutableList.of("DELIVERY", "SORTING_CENTER"))
            .containsEntry("marketIds", ImmutableList.of("12", "23"))
            .containsEntry("platformClientIds", ImmutableList.of("34", "45"))
            .containsEntry("statuses", ImmutableList.of("ACTIVE", "TESTING"))
            .containsEntry("ids", ImmutableList.of("1", "45", "12"));
    }

}
