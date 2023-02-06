package ru.yandex.market.ff.service.implementation;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.XDocPartnerRelationResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogisticManagementServiceImplTest {

    private LMSClient lmsClient = mock(LMSClient.class);
    private LogisticManagementServiceImpl logisticManagementService = new LogisticManagementServiceImpl(lmsClient);

    @Test
    void getXDocSupplyAdditionalDateInterval() {
        when(lmsClient.getXDocPartnerRelations(1L)).thenReturn(getPartnerRelations());
        Optional<Long> maybeXDocSupplyAdditionalDateInterval =
                logisticManagementService.getXDocSupplyAdditionalDateInterval(2L, 1L);
        assertTrue(maybeXDocSupplyAdditionalDateInterval.isPresent());
        assertEquals(10, maybeXDocSupplyAdditionalDateInterval.get().longValue());
    }

    private ArrayList<XDocPartnerRelationResponse> getPartnerRelations() {
        ArrayList<XDocPartnerRelationResponse> xDocPartnerRelationResponseList = new ArrayList<>();
        XDocPartnerRelationResponse partnerRelationResponse =
            new XDocPartnerRelationResponse(1L, 1L, 2L, "pek", "10", true);
        XDocPartnerRelationResponse partnerRelationResponse2 =
            new XDocPartnerRelationResponse(1L, 1L, 3L, "kek", "20", true);
        xDocPartnerRelationResponseList.add(partnerRelationResponse);
        xDocPartnerRelationResponseList.add(partnerRelationResponse2);
        return xDocPartnerRelationResponseList;
    }
}
