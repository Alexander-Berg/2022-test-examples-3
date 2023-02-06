package ru.yandex.market.adv.promo.mvc.promo.common.controller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import com.google.protobuf.Timestamp;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.adv.promo.datacamp.utils.PromoStorageUtils.BUSINESS_ID_ANAPLAN;
import static ru.yandex.market.adv.promo.utils.CheapestAsGiftMechanicTestUtils.createAnaplanCheapestAsGiftDescription;
import static ru.yandex.market.adv.promo.utils.CommonTestUtils.getResource;
import static ru.yandex.market.adv.promo.utils.DirectDiscountMechanicTestUtils.createAnaplanDirectDiscountDescription;
import static ru.yandex.market.adv.promo.utils.PromoMechanicTestUtils.addUpdateTimeToPromoDescription;
import static ru.yandex.market.adv.promo.utils.PromocodeMechanicTestUtils.createPartnerPromocodeDescription;

class PromoDescriptionControllerTest extends FunctionalTest {
    @Autowired
    private DataCampClient dataCampClient;

    @BeforeEach
    void before() {
        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance())
                .when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
    }

    @Test
    @DisplayName("Базовая проверка запроса (корректность обработки входных данных)")
    void getPromoDescriptionByIdBaseTest() {
        long businessId = 1001;
        long partnerId = 1111;

        String promo1 = "#1001";
        String promo2 = "#10457205";
        String promo3 = "1111_TDBFGHDTE";
        String requestBody = "" +
                "{\"ids\": [" +
                "\""+ promo1 + "\", " +
                "\"" + promo2 + "\", " +
                "\"" + promo3 + "\"" +
                "]}";
        getPromoDescriptionById(businessId, partnerId, false, false, requestBody);

        ArgumentCaptor<GetPromoBatchRequestWithFilters> captor =
                ArgumentCaptor.forClass(GetPromoBatchRequestWithFilters.class);
        verify(dataCampClient, times(1)).getPromos(captor.capture());
        SyncGetPromo.GetPromoBatchRequest request = captor.getValue().getRequest();
        assertEquals(3, request.getEntriesCount());

        Map<String, DataCampPromo.PromoDescriptionIdentifier> identifierByPromoId =
                request.getEntriesList().stream()
                        .collect(
                                Collectors.toMap(
                                        DataCampPromo.PromoDescriptionIdentifier::getPromoId,
                                        Function.identity()
                                )
                        );

        DataCampPromo.PromoDescriptionIdentifier identifier1 = identifierByPromoId.get(promo1);
        assertEquals(promo1, identifier1.getPromoId());
        assertEquals(BUSINESS_ID_ANAPLAN, identifier1.getBusinessId());
        assertEquals(NMarket.Common.Promo.Promo.ESourceType.ANAPLAN, identifier1.getSource());

        DataCampPromo.PromoDescriptionIdentifier identifier2 = identifierByPromoId.get(promo2);
        assertEquals(promo2, identifier2.getPromoId());
        assertEquals(BUSINESS_ID_ANAPLAN, identifier2.getBusinessId());
        assertEquals(NMarket.Common.Promo.Promo.ESourceType.ANAPLAN, identifier2.getSource());

        DataCampPromo.PromoDescriptionIdentifier identifier3 = identifierByPromoId.get(promo3);
        assertEquals(promo3, identifier3.getPromoId());
        assertEquals(businessId, identifier3.getBusinessId());
        assertEquals(NMarket.Common.Promo.Promo.ESourceType.PARTNER_SOURCE, identifier3.getSource());
    }

    @Test
    @DisplayName("Получение только незавершенных промо")
    void getOnlyUnfinishedPromoDescriptionByIdTest() {
        getPromoDescriptionByIdTest(
                true,
                "getOnlyUnfinishedPromoDescriptionByIdTest_response.json"
        );
    }

    @Test
    @DisplayName("Получение всех промо")
    void getAllPromoDescriptionByIdTest() {
        getPromoDescriptionByIdTest(
                false,
                "getAllPromoDescriptionByIdTest_response.json"
        );
    }

    private void getPromoDescriptionByIdTest(
            boolean onlyUnfinished,
            String resultFilename
    ) {
        long businessId = 1001;
        long partnerId = 1111;
        String requestBody = onlyUnfinishedFilter_mockPromoStorageResponse(businessId, partnerId);

        ResponseEntity<String> response =
                getPromoDescriptionById(businessId, partnerId, onlyUnfinished, false, requestBody);

        String expected = getResource(this.getClass(), resultFilename);
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    private String onlyUnfinishedFilter_mockPromoStorageResponse(long businessId, long partnerId) {
        String endedPromocodeId = partnerId + "_TGDYEKD";
        String disabledPromocodeId = partnerId + "_HDNR678DJD5H";
        String currentPromocodeId = partnerId + "_TGD67DHE";
        String futurePromocodeId = partnerId + "_RT453DG4G";

        String endedCheapestAsGiftFomAnaplanId = "#1001";
        String currentCheapestAsGiftFomAnaplanId = "#1002";
        String futureCheapestAsGiftFomAnaplanId = "#1003";

        // Закончившийся промокод
        DataCampPromo.PromoDescription endedPromocode = createPartnerPromocodeDescription(
                businessId,
                endedPromocodeId,
                LocalDateTime.now().minus(25, ChronoUnit.DAYS),
                LocalDateTime.now().minus(15, ChronoUnit.DAYS),
                true
        );
        endedPromocode = addMetaToPromoGeneralInfo(endedPromocode, 1609102800);
        // Завершенный принудительно промокод
        DataCampPromo.PromoDescription disabledPromocode = createPartnerPromocodeDescription(
                businessId,
                disabledPromocodeId,
                LocalDateTime.now().minus(10, ChronoUnit.DAYS),
                LocalDateTime.now().plus(20, ChronoUnit.DAYS),
                false
        );
        disabledPromocode = addMetaToPromoGeneralInfo(disabledPromocode, 1640379600);
        // Идущий в данный момент промокод
        DataCampPromo.PromoDescription currentPromocode = createPartnerPromocodeDescription(
                businessId,
                currentPromocodeId,
                LocalDateTime.now().minus(5, ChronoUnit.DAYS),
                LocalDateTime.now().plus(25, ChronoUnit.DAYS),
                true
        );
        currentPromocode = addUpdateTimeToPromoDescription(currentPromocode, 1635704000, 1637830800);
        // Промокод, который еще не начался
        DataCampPromo.PromoDescription futurePromocode = createPartnerPromocodeDescription(
                businessId,
                futurePromocodeId,
                LocalDateTime.now().plus(10, ChronoUnit.DAYS),
                LocalDateTime.now().plus(20, ChronoUnit.DAYS),
                true
        );
        futurePromocode = addUpdateTimeToPromoDescription(futurePromocode, 1640179600, 1640638800);
        // Завершившаяся "Самый дешевый в подарок" из анаплана
        DataCampPromo.PromoDescription endedCheapestAsGift = createAnaplanCheapestAsGiftDescription(
                endedCheapestAsGiftFomAnaplanId,
                LocalDateTime.now().minus(20, ChronoUnit.DAYS),
                LocalDateTime.now().minus(10, ChronoUnit.DAYS)
        );
        // Идущая "Самый дешевый в подарок" из анаплана
        DataCampPromo.PromoDescription currentCheapestAsGift = createAnaplanCheapestAsGiftDescription(
                currentCheapestAsGiftFomAnaplanId,
                LocalDateTime.now().minus(2, ChronoUnit.DAYS),
                LocalDateTime.now().plus(5, ChronoUnit.DAYS)
        );
        // "Самый дешевый в подарок" из анаплана, которая еще не началась
        DataCampPromo.PromoDescription futureCheapestAsGift = createAnaplanCheapestAsGiftDescription(
                futureCheapestAsGiftFomAnaplanId,
                LocalDateTime.now().plus(2, ChronoUnit.DAYS),
                LocalDateTime.now().plus(5, ChronoUnit.DAYS)
        );

        ArgumentMatcher<GetPromoBatchRequestWithFilters> onlyUnfinishedPromosMatcher = request ->
                BooleanUtils.isTrue(request.getOnlyUnfinished()) && BooleanUtils.isTrue(request.getEnabled());
        ArgumentMatcher<GetPromoBatchRequestWithFilters> allPromosMatcher = request ->
                request.getOnlyUnfinished() == null && request.getEnabled() == null;


        doReturn(
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(
                                DataCampPromo.PromoDescriptionBatch.newBuilder()
                                        .addPromo(currentPromocode)
                                        .addPromo(futurePromocode)
                                        .addPromo(currentCheapestAsGift)
                                        .addPromo(futureCheapestAsGift)
                                        .build()
                        )
                        .build()
        ).when(dataCampClient).getPromos(argThat(onlyUnfinishedPromosMatcher));
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(
                                DataCampPromo.PromoDescriptionBatch.newBuilder()
                                        .addPromo(endedPromocode)
                                        .addPromo(disabledPromocode)
                                        .addPromo(currentPromocode)
                                        .addPromo(futurePromocode)
                                        .addPromo(endedCheapestAsGift)
                                        .addPromo(currentCheapestAsGift)
                                        .addPromo(futureCheapestAsGift)
                                        .build()
                        )
                        .build()
        ).when(dataCampClient).getPromos(argThat(allPromosMatcher));

        return "{\"ids\": [" +
                "\"" + endedPromocodeId + "\", " +
                "\"" + disabledPromocodeId + "\", " +
                "\"" + currentPromocodeId + "\", " +
                "\"" + futurePromocodeId + "\", " +
                "\"" + endedCheapestAsGiftFomAnaplanId + "\", " +
                "\"" + currentCheapestAsGiftFomAnaplanId + "\", " +
                "\"" + futureCheapestAsGiftFomAnaplanId + "\"" +
                "]}";
    }

    private DataCampPromo.PromoDescription addMetaToPromoGeneralInfo(
            DataCampPromo.PromoDescription description,
            long seconds
    ) {
        return description.toBuilder()
                .setPromoGeneralInfo(
                        description.getPromoGeneralInfo().toBuilder()
                                .setMeta(
                                        DataCampOfferMeta.UpdateMeta.newBuilder()
                                                .setTimestamp(
                                                        Timestamp.newBuilder()
                                                                .setSeconds(seconds)
                                                                .build()
                                                )
                                                .build()
                                )
                )
                .build();
    }

    @Test
    @DisplayName("Получение скидочного промо со списком категорий")
    public void getPromoDescriptionByIdWithCategoriesTest() {
        filterByCategoriesTest(
                false,
                "getPromoDescriptionByIdWithCategoriesTest_response.json"
        );
    }

    @Test
    @DisplayName("Получение скидочного промо без списка категорий")
    public void getPromoDescriptionByIdWithoutCategoriesTest() {
        filterByCategoriesTest(
                true,
                "getPromoDescriptionByIdWithoutCategoriesTest_response.json"
        );
    }

    private void filterByCategoriesTest(
            boolean withoutCategories,
            String resultFilename
    ) {
        long businessId = 1001;
        long partnerId = 1111;
        String promoId = "#12345";
        categoriesFilter_mockPromoStorageResponse(promoId);

        String body = "{\"ids\": [\""+ promoId + "\"]}";
        ResponseEntity<String> response =
                getPromoDescriptionById(businessId, partnerId, false, withoutCategories, body);

        String expected = getResource(this.getClass(), resultFilename);
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    private void categoriesFilter_mockPromoStorageResponse(String promoId) {
        Set<Pair<Long, Integer>> categoriesInfo =
                Set.of(
                        Pair.of(111L, 13),
                        Pair.of(222L, 6),
                        Pair.of(333L, 45)
                );
        DataCampPromo.PromoDescription directDiscountDescription =
                createAnaplanDirectDiscountDescription(promoId, categoriesInfo);

        doReturn(
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(
                                DataCampPromo.PromoDescriptionBatch.newBuilder()
                                        .addPromo(directDiscountDescription)
                                        .build()
                        )
                        .build()
        ).when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
    }

    private ResponseEntity<String> getPromoDescriptionById(
            long businessId,
            long partnerId,
            boolean onlyUnfinished,
            boolean withoutCategories,
            Object body
    ) {
        String requestOnlyUnfinished = onlyUnfinished ? "&onlyUnfinished=1" : "";
        String requestWithoutCategories = withoutCategories ? "&withoutCategories=1" : "";
        return FunctionalTestHelper.post(
                baseUrl() + "/partner/promo/description/by-id?" +
                        "partnerId=" + partnerId + "&businessId=" + businessId +
                        requestOnlyUnfinished + requestWithoutCategories,
                new HttpEntity<>(body, getDefaultHeaders())
        );
    }
}
