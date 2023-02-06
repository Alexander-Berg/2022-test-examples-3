package ru.yandex.market.adv.promo.mvc.promo.common.controller;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PartnerPromoManagementControllerTest extends FunctionalTest {
    @Autowired
    private DataCampClient dataCampClient;

    @Test
    @DisplayName("Проверка корректности изменения статуса для промокодовой акции")
    void changePromoStatusTest() {
        long partnerId = 12345;
        long businessId = 789;
        String promoId = "12345_promo_test";
        int newStatus = 1; //true
        boolean oldStatus = false;
        boolean expectedStatus = true;
        DataCampPromo.PromoType promoType = DataCampPromo.PromoType.MARKET_PROMOCODE;

        doReturn(createGetPromoBatchResponse(promoId, promoType, oldStatus))
                .when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));

        ArgumentCaptor<DataCampPromo.PromoDescription> argumentCapture =
                ArgumentCaptor.forClass(DataCampPromo.PromoDescription.class);

        changeStatus(partnerId, businessId, promoId, newStatus);

        verify(dataCampClient, times(1)).addPromo(argumentCapture.capture());
        DataCampPromo.PromoDescription newDescription = argumentCapture.getValue();

        Assertions.assertEquals(expectedStatus, newDescription.getConstraints().getEnabled());
    }

    @Test
    @DisplayName("Проверка корректности изменения статуса для кешбечной акции")
    void changePromoStatusForCashbackPromoTest() {
        long partnerId = 12345;
        long businessId = 789;
        String promoId = "12345_promo_test";
        int newStatus = 1; //true
        boolean oldStatus = false;
        boolean expectedStatus = true;
        DataCampPromo.PromoType promoType = DataCampPromo.PromoType.PARTNER_CUSTOM_CASHBACK;

        doReturn(createGetPromoBatchResponse(promoId, promoType, oldStatus))
                .when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));

        ArgumentCaptor<DataCampPromo.PromoDescription> argumentCapture =
                ArgumentCaptor.forClass(DataCampPromo.PromoDescription.class);

        changeStatus(partnerId, businessId, promoId, newStatus);

        verify(dataCampClient, times(1)).addPromo(argumentCapture.capture());
        DataCampPromo.PromoDescription newDescription = argumentCapture.getValue();

        Assertions.assertEquals(expectedStatus, newDescription.getConstraints().getEnabled());
    }

    @Test
    @DisplayName("Проверка корректности выключения всех кастомных кешбеков")
    void disableAllTest() {
        long partnerId = 12345;
        long businessId = 789;
        List<String> promoIds = List.of("promo_1", "promo_2", "promo_3");

        doReturn(createGetResponseForDisableAllRequest(promoIds))
                .when(dataCampClient).getPromos(any(PromoDatacampRequest.class));

        sendDisableAllRequest(partnerId, businessId);

        ArgumentCaptor<SyncGetPromo.UpdatePromoBatchRequest> captor =
                ArgumentCaptor.forClass(SyncGetPromo.UpdatePromoBatchRequest.class);
        verify(dataCampClient, times(1)).addPromo(captor.capture(), eq(businessId));
        SyncGetPromo.UpdatePromoBatchRequest updateRequest = captor.getValue();

        List<DataCampPromo.PromoDescription> promos = updateRequest.getPromos().getPromoList()
                .stream()
                .sorted(Comparator.comparing(x -> x.getPrimaryKey().getPromoId()))
                .collect(Collectors.toList());
        assertEquals(3, promos.size());

        DataCampPromo.PromoDescription promo1 = promos.get(0);
        DataCampPromo.PromoDescription promo2 = promos.get(1);
        DataCampPromo.PromoDescription promo3 = promos.get(2);
        assertEquals("promo_1", promo1.getPrimaryKey().getPromoId());
        assertFalse(promo1.getConstraints().getEnabled());
        assertEquals("promo_2", promo2.getPrimaryKey().getPromoId());
        assertFalse(promo2.getConstraints().getEnabled());
        assertEquals("promo_3", promo3.getPrimaryKey().getPromoId());
        assertFalse(promo3.getConstraints().getEnabled());
    }

    private void sendDisableAllRequest(long partnerId, long businessId) {

        FunctionalTestHelper.put(
                baseUrl() + "/partner/promo/cashback/custom/disable/all?partnerId="
                        + partnerId + "&businessId=" + businessId,
                new HttpEntity<>(null, getDefaultHeaders())
        );
    }

    private void changeStatus(long partnerId, long businessId, String promoId, int status) {
        FunctionalTestHelper.put(
                baseUrl() + "/partner/promo/enable-status-change?" +
                        "partnerId=" + partnerId + "&businessId=" + businessId +
                        "&promoId=" + promoId + "&status=" + status,
                new HttpEntity<>(null, getDefaultHeaders()));
    }

    private SyncGetPromo.GetPromoBatchResponse createGetPromoBatchResponse(
            String promoId,
            DataCampPromo.PromoType promoType,
            boolean oldStatus
    ) {
        DataCampPromo.PromoDescription promo = createPromo(
                promoId,
                promoType,
                oldStatus
        );
        DataCampPromo.PromoDescriptionBatch batch = DataCampPromo.PromoDescriptionBatch.newBuilder()
                .addPromo(promo)
                .build();

        return SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(batch)
                .build();
    }

    private SyncGetPromo.GetPromoBatchResponse createGetResponseForDisableAllRequest(List<String> promos) {
        DataCampPromo.PromoDescriptionBatch.Builder batchBuilder = DataCampPromo.PromoDescriptionBatch.newBuilder();
        promos.forEach(promoId -> batchBuilder.addPromo(
                createPromo(promoId, DataCampPromo.PromoType.PARTNER_CUSTOM_CASHBACK, true))
        );

        return SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(batchBuilder.build())
                .build();
    }

    private DataCampPromo.PromoDescription createPromo(
            String promoId,
            DataCampPromo.PromoType promoType,
            boolean oldStatus
    ) {
        DataCampPromo.PromoDescriptionIdentifier identifier = DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                .setPromoId(promoId)
                .build();

        DataCampPromo.PromoGeneralInfo generalInfo = DataCampPromo.PromoGeneralInfo.newBuilder()
                .setPromoType(promoType)
                .build();

        DataCampPromo.PromoConstraints constraints = DataCampPromo.PromoConstraints.newBuilder()
                .setEnabled(oldStatus)
                .build();

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(identifier)
                .setPromoGeneralInfo(generalInfo)
                .setConstraints(constraints)
                .build();
    }
}
