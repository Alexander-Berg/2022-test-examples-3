package ru.yandex.market.fulfillment.stockstorage.service.lms;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.FulfillmentPartner;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LmsServiceTest extends AbstractContextualTest {

    public static final int NUMBER_OF_SUPPLIERS_PER_WAREHOUSE = 5;

    @Autowired
    private LmsService lmsService;

    @Test
    public void successGetFulfillmentPartners() {
        setActiveWarehouses(1);

        List<FulfillmentPartner> fulfillmentPartners = lmsService.getFulfillmentPartners();

        assertNotNull(fulfillmentPartners);
        assertEquals(1, fulfillmentPartners.size());

        FulfillmentPartner fulfillmentPartner = fulfillmentPartners.get(0);
        assertEquals(1, fulfillmentPartner.getId());
        assertEquals(LmsPartnerType.FULFILLMENT, fulfillmentPartner.getPartnerType());

        verify(fulfillmentLmsClient, times(2)).searchPartners(any(SearchPartnerFilter.class));
    }

    @Test
    @DatabaseSetup("classpath:database/states/lms/additional_partner_types.xml")
    public void successGetFulfillmentPartnersWithAdditionalType() throws JsonProcessingException {
        mockSearchPartners(Arrays.asList(
                partnerResponse(1, PartnerType.FULFILLMENT),
                partnerResponse(2, PartnerType.DROPSHIP_BY_SELLER),
                partnerResponse(3, PartnerType.DELIVERY))
        );

        List<FulfillmentPartner> fulfillmentPartners = lmsService.getFulfillmentPartners();

        softly.assertThat(fulfillmentPartners).isNotNull();
        softly.assertThat(fulfillmentPartners.size()).isEqualTo(3);

        FulfillmentPartner fulfillmentPartner = fulfillmentPartners.get(0);
        softly.assertThat(fulfillmentPartner.getId()).isEqualTo(1);
        softly.assertThat(fulfillmentPartner.getPartnerType()).isEqualTo(LmsPartnerType.FULFILLMENT);

        fulfillmentPartner = fulfillmentPartners.get(1);
        softly.assertThat(fulfillmentPartner.getId()).isEqualTo(2);
        softly.assertThat(fulfillmentPartner.getPartnerType()).isEqualTo(LmsPartnerType.DROPSHIP_BY_SELLER);

        fulfillmentPartner = fulfillmentPartners.get(2);
        softly.assertThat(fulfillmentPartner.getId()).isEqualTo(3);
        softly.assertThat(fulfillmentPartner.getPartnerType()).isEqualTo(LmsPartnerType.DELIVERY);

        ArgumentCaptor<SearchPartnerFilter> captor = ArgumentCaptor.forClass(SearchPartnerFilter.class);
        verify(fulfillmentLmsClient).searchPartners(captor.capture());
        SearchPartnerFilter filter = captor.getValue();
        softly.assertThat(filter.getTypes()).containsExactlyInAnyOrder(
                PartnerType.FULFILLMENT, PartnerType.SUPPLIER, PartnerType.DROPSHIP,
                PartnerType.DROPSHIP_BY_SELLER, PartnerType.DELIVERY
        );
    }

    @Test
    public void shouldReturnEmptyListIfLmsReturnEmptyList() {
        List<FulfillmentPartner> fulfillmentPartners = lmsService.getFulfillmentPartners();

        assertNotNull(fulfillmentPartners);
        assertTrue(fulfillmentPartners.isEmpty());

        verify(fulfillmentLmsClient, times(1)).searchPartners(any(SearchPartnerFilter.class));
    }

    @Test
    public void shouldReturnOnePartnerIfLmsReturnPartnersOneOfWhichIsNull() throws JsonProcessingException {
        mockSearchPartners(Arrays.asList(
                PartnerResponse.newBuilder()
                        .id(2)
                        .partnerType(PartnerType.FULFILLMENT)
                        .name("MARKET_ROSTOV")
                        .status(PartnerStatus.ACTIVE)
                        .build(),
                null
        ));

        List<FulfillmentPartner> fulfillmentPartners = lmsService.getFulfillmentPartners();

        assertNotNull(fulfillmentPartners);
        assertEquals(1, fulfillmentPartners.size());

        verify(fulfillmentLmsClient, times(1)).searchPartners(any(SearchPartnerFilter.class));
    }

    @Test
    public void getCrossdockSuppliersIdsHappyPath() throws JsonProcessingException {
        mockSearchPartnerRelations();
        mockSearchPartners();

        Map<Long, Set<Long>> result = lmsService.getCrossdockSuppliersIdsByFulfillmentId();

        ImmutableMap<Long, Set<Long>> expected = ImmutableMap.of(
                1L, LongStream.rangeClosed(1, 5).boxed().collect(Collectors.toSet()),
                2L, LongStream.rangeClosed(1, 5).boxed().collect(Collectors.toSet()));

        assertNotNull(result);
        assertEquals(expected, result);
    }

    private void mockSearchPartnerRelations() {
        List<PartnerRelationEntityDto> relationFixture =
                IntStream.rangeClosed(1, NUMBER_OF_SUPPLIERS_PER_WAREHOUSE)
                        .mapToObj(idFrom ->
                                IntStream.rangeClosed(1, 3)
                                        .mapToObj(idTo -> partnerRelationFromTo(idFrom, idTo)))
                        .flatMap(Function.identity())
                        .collect(Collectors.toList());
        when(lmsClient.searchPartnerRelation(any())).thenReturn(relationFixture);
    }

    private void mockSearchPartners() {
        mockSearchPartners(Arrays.asList(
                partnerResponse(1, PartnerType.FULFILLMENT),
                partnerResponse(2, PartnerType.FULFILLMENT),
                partnerResponse(3, PartnerType.DELIVERY))
        );
    }

    private PartnerResponse partnerResponse(int i, PartnerType fulfillment) {
        return PartnerResponse.newBuilder().id(i).partnerType(fulfillment).build();
    }

    private PartnerRelationEntityDto partnerRelationFromTo(long from, long to) {
        return PartnerRelationEntityDto.newBuilder().fromPartnerId(from).toPartnerId(to).build();
    }
}
