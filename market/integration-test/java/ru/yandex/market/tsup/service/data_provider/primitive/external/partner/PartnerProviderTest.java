package ru.yandex.market.tsup.service.data_provider.primitive.external.partner;

import java.util.EnumSet;
import java.util.List;

import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.tpl.common.data_provider.meta.FrontHttpRequestMeta;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.data_provider.filter.impl.PartnerFilter;
import ru.yandex.market.tsup.service.data_provider.primitive.external.lms.partner.PartnerProvider;
import ru.yandex.market.tsup.service.data_provider.primitive.external.lms.partner.dto.PartnerDto;

@DbUnitConfiguration
class PartnerProviderTest extends AbstractContextualTest {
    private static final EnumSet<PartnerType> ALLOWED_PARTNER_TYPE = EnumSet.of(
            PartnerType.FULFILLMENT,
            PartnerType.SORTING_CENTER,
            PartnerType.DISTRIBUTION_CENTER
    );

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private PartnerProvider partnerProvider;

    @Test
    void testProvide() {
        Mockito.when(lmsClient.searchPartners(searchPartnerFilter())).thenReturn(
                partnerResponse()
        );

        PartnerDto expected = PartnerDto.builder()
                .id(1L)
                .name("Софьино")
                .partnerType(PartnerType.FULFILLMENT)
                .build();

        Assertions.assertThat(partnerProvider.provide(PartnerFilter.builder()
                        .types(ALLOWED_PARTNER_TYPE)
                        .build(), new FrontHttpRequestMeta())).hasSize(1)
                .contains(expected);
    }

    private SearchPartnerFilter searchPartnerFilter() {
        return SearchPartnerFilter.builder()
                .setTypes(ALLOWED_PARTNER_TYPE)
                .build();
    }

    private List<PartnerResponse> partnerResponse() {
        return List.of(
                PartnerResponse.newBuilder()
                        .partnerType(PartnerType.FULFILLMENT)
                        .id(1L)
                        .name("Софьино")
                        .build()
        );
    }
}
