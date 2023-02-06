package ru.yandex.market.core.delivery;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RetryableMbiLmsClientTest extends FunctionalTest {

    @Autowired
    RetryableMbiLmsClient retryableMbiLmsClient;

    @Autowired
    LMSClient lmsClient;

    private static final PartnerResponse PARTNER_RESPONSE_101 = PartnerResponse.newBuilder().id(101).build();
    private static final PartnerResponse PARTNER_RESPONSE_102 = PartnerResponse.newBuilder().id(102).build();
    private static final PartnerResponse PARTNER_RESPONSE_199 = PartnerResponse.newBuilder().id(199).build();
    private static final Set<Long> MBI_PARTNERS = Set.of(501L, 502L, 503L);

    @Test
    void testGetPartnerRetries() {
        Mockito.when(lmsClient.getPartner(Mockito.any())).thenThrow(new RestClientException("msg"));
        Assertions.assertThrows(
                RestClientException.class,
                () -> retryableMbiLmsClient.getPartner(123L));
        Mockito.verify(lmsClient, times(3)).getPartner(Mockito.any());
    }

    @Test
    @DbUnitDataSet(before = "RetryableMbiLmsClientTest.bulkRequest.before.csv")
    void testGetBulkRequest() {
        Mockito.when(lmsClient.searchPartners(Mockito.any()))
                .thenReturn(List.of(PARTNER_RESPONSE_101, PARTNER_RESPONSE_102, PARTNER_RESPONSE_199));
        Map<Long, List<PartnerResponse>> responses = retryableMbiLmsClient.getLmsPartnersByMbiPartners(MBI_PARTNERS);
        Assertions.assertEquals(Map.of(501L, List.of(PARTNER_RESPONSE_101),
                502L, List.of(PARTNER_RESPONSE_102),
                503L, List.of(PARTNER_RESPONSE_199)), responses);
        ArgumentCaptor<SearchPartnerFilter> searchPartnerCaptor = ArgumentCaptor.forClass(SearchPartnerFilter.class);
        verify(lmsClient).searchPartners(searchPartnerCaptor.capture());
        Assertions.assertEquals(searchPartnerCaptor.getValue().getIds(), Set.of(101L, 102L, 199L));
    }

    @Test
    void testUpdateRetries() {
        Mockito.when(lmsClient.changePartnerStatus(Mockito.any(), Mockito.any())).thenThrow(new RestClientException(
                "msg"));
        Assertions.assertThrows(
                RestClientException.class,
                () -> retryableMbiLmsClient.changeLogisticPartnerStatus(123L, PartnerStatus.ACTIVE));
        Mockito.verify(lmsClient, times(3)).changePartnerStatus(Mockito.any(), Mockito.any());
        Mockito.verify(lmsClient, times(0)).updatePartnerSettings(Mockito.any(), Mockito.any());
    }
}
