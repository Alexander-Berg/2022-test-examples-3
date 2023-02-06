package ru.yandex.market.logistics.iris.service.lms;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.entity.partner.FulfillmentPartner;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class LmsServiceTest extends AbstractContextualTest {

    @Autowired
    private LmsService lmsService;

    @After
    public void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    public void shouldReturnAllPartnersWhenWhenLmsReturnOkPartners() {
        when(lmsClient.searchPartners(any(SearchPartnerFilter.class))).thenReturn(ImmutableList.of(getPartnerResponse(1L)));

        List<FulfillmentPartner> fulfillmentPartners = lmsService.getFulfillmentPartners();

        assertSoftly(assertions ->
            assertions.assertThat(fulfillmentPartners.size()).isEqualTo(1)
        );

        FulfillmentPartner fulfillmentPartner = fulfillmentPartners.get(0);
        assertSoftly(assertions -> {
            assertions.assertThat(fulfillmentPartner.getId()).isEqualTo(1);
            assertions.assertThat(fulfillmentPartner.getPartnerType()).isEqualTo(LmsPartnerType.FULFILLMENT);
        });

        verify(lmsClient).searchPartners(any(SearchPartnerFilter.class));
    }

    @Test
    public void shouldReturnEmptyListIfLmsReturnEmptyList() {
        List<FulfillmentPartner> fulfillmentPartners = lmsService.getFulfillmentPartners();

        assertSoftly(assertions ->
            assertions.assertThat(fulfillmentPartners).isEmpty()
        );

        verify(lmsClient).searchPartners(any(SearchPartnerFilter.class));
    }

    @Test
    public void shouldReturnOnePartnerIfLmsReturnPartnersOneOfWhichIsNull() {
        List<PartnerResponse> partnerResponses = Arrays.asList(
            getPartnerResponse(2L),
            null
        );

        when(lmsClient.searchPartners(any(SearchPartnerFilter.class))).thenReturn(partnerResponses);

        List<FulfillmentPartner> fulfillmentPartners = lmsService.getFulfillmentPartners();

        assertSoftly(assertions ->
            assertions.assertThat(fulfillmentPartners.size()).isEqualTo(1)
        );

        verify(lmsClient).searchPartners(any(SearchPartnerFilter.class));
    }

    private PartnerResponse getPartnerResponse(Long id) {
        return PartnerResponse.newBuilder()
            .id(id)
            .partnerType(PartnerType.FULFILLMENT)
            .name("MARKET_ROSTOV")
            .status(PartnerStatus.ACTIVE)
            .build();
    }
}
