package ru.yandex.market.adv.promo.mvc.promo.common.controller;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;
import ru.yandex.market.saas.search.response.SaasSearchResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.adv.promo.datacamp.utils.PromoStorageUtils.ANAPLAN_PREFIX;
import static ru.yandex.market.adv.promo.datacamp.utils.PromoStorageUtils.CATEGORY_FACE_PREFIX;
import static ru.yandex.market.adv.promo.utils.CheapestAsGiftMechanicTestUtils.createAnaplanCheapestAsGiftDescription;
import static ru.yandex.market.adv.promo.utils.CheapestAsGiftMechanicTestUtils.createPartnerCheapestAsGiftDescription;
import static ru.yandex.market.adv.promo.utils.CommonTestUtils.getResource;
import static ru.yandex.market.adv.promo.utils.CommonTestUtils.readJson;
import static ru.yandex.market.adv.promo.utils.DirectDiscountMechanicTestUtils.createAnaplanDirectDiscountDescription;
import static ru.yandex.market.adv.promo.utils.PromocodeMechanicTestUtils.createPartnerPromocodeDescription;

class CurrentAndFuturePromosControllerTest extends FunctionalTest {

    @Autowired
    private SaasService saasService;

    @Autowired
    private DataCampClient dataCampClient;

    @BeforeEach
    void before() {
        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance())
                .when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));
        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance())
                .when(dataCampClient).getPromos(any(PromoDatacampRequest.class));
        when(saasService.searchShopGroupings(anyLong(), anyInt(), any(), any(), anyInt(), anyInt(), isNull()))
                .thenReturn(new SaasSearchResponse());
    }

    @Test
    @DisplayName("Кейс с пустым ответом ручки: нет ни одной подходящей для страницы акции")
    void getCurrentAndFuturePromosForPartner_noPromosTest() throws IOException {
        // В ответе саас возвращаются какие-то промки, которые после не возвращаются из ОХ (например, прошедшие).
        // Такие не должны учитываться/отдаваться дальше.
        mockSaasResponse("noPromosTest_saas_response.json");

        long businessId = 1001;
        long partnerId = 1111;
        ResponseEntity<String> response = getCurrentAndFuturePromosForPartner(businessId, partnerId);

        String expected = getResource(this.getClass(), "noPromosTest_response.json");
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    @Test
    @DisplayName("Кейс, когда есть только партнерские акции (с ассортиментом и без ассортимента)")
    void getCurrentAndFuturePromosForPartner_onlyPartnerPromosTest() throws IOException {
        // Из саас возщвращается часть партнерских промо (у которых есть ассорт), а также несколько маркетплейсных,
        // которые после не возвращаются из ОХ (например, прошедшие). Такие не должны учитываться/отдаваться дальше.
        mockSaasResponse("onlyPartnerPromosTest_saas_response.json");
        onlyPartnerPromosTest_mockPartnerPromosFromDataCampResponse();

        long businessId = 1001;
        long partnerId = 1111;
        ResponseEntity<String> response = getCurrentAndFuturePromosForPartner(businessId, partnerId);

        // Намеренно не проверяем маппинг описаний промо
        String expected = getResource(this.getClass(), "onlyPartnerPromosTest_response.json");
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    private void onlyPartnerPromosTest_mockPartnerPromosFromDataCampResponse() {
        DataCampPromo.PromoDescription partnerPromocodeWithAssortment = createPartnerPromocodeDescription(
                "1111_RTFGERS"
        );
        DataCampPromo.PromoDescription partnerPromocodeWithoutAssortment = createPartnerPromocodeDescription(
                "1111_AABBCCD"
        );
        DataCampPromo.PromoDescription partnerCheapestAsGift = createPartnerCheapestAsGiftDescription(
                "1111_CAG_123456789"
        );
        mockPartnerPromosFromDataCampResponse(
                List.of(
                        partnerPromocodeWithAssortment,
                        partnerPromocodeWithoutAssortment,
                        partnerCheapestAsGift
                )
        );
    }

    @Test
    @DisplayName("Кейс, когда есть только маркетплейсные акции и все они из Ритейл Поводов")
    void getCurrentAndFuturePromosForPartner_onlyMarketplacePromosInGroupsTest() throws IOException {
        mockSaasResponse("onlyMarketplacePromosInGroupsTest_saas_response.json");
        onlyMarketplacePromosInGroupsTest_mockAnaplanPromosFromDataCampResponse();
        onlyMarketplacePromosInGroupsTest_mockParentPromosFromDataCampResponse();

        long businessId = 1001;
        long partnerId = 1111;
        ResponseEntity<String> response = getCurrentAndFuturePromosForPartner(businessId, partnerId);

        // Намеренно не проверяем маппинг описаний промо
        String expected = getResource(this.getClass(), "onlyMarketplacePromosInGroupsTest_response.json");
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    private void onlyMarketplacePromosInGroupsTest_mockAnaplanPromosFromDataCampResponse() {
        DataCampPromo.PromoDescription anaplanCAG1 = addParentPromo(
                "SP#201",
                createAnaplanCheapestAsGiftDescription("#1111")
        );
        DataCampPromo.PromoDescription anaplanCAG2 = addParentPromo(
                "SP#201",
                createAnaplanCheapestAsGiftDescription("#2222")
        );
        DataCampPromo.PromoDescription anaplanCAG3 = addParentPromo(
                "SP#201",
                createAnaplanCheapestAsGiftDescription("#3333")
        );
        DataCampPromo.PromoDescription anaplanCAG4 = addParentPromo(
                "SP#205",
                createAnaplanCheapestAsGiftDescription("#4444")
        );
        mockAnaplanPromosFromDataCampResponse(List.of(anaplanCAG1, anaplanCAG2, anaplanCAG3, anaplanCAG4));
    }

    private void onlyMarketplacePromosInGroupsTest_mockParentPromosFromDataCampResponse() {
        DataCampPromo.PromoDescription parentPromo1 = createParentPromo("SP#201", "Promo group SP#201");
        DataCampPromo.PromoDescription parentPromo2 = createParentPromo("SP#205", "Promo group SP#205");
        mockParentPromosFromDataCampResponse(List.of(parentPromo1, parentPromo2));
    }

    @Test
    @DisplayName("Кейс, когда есть только маркетплейсные акции без Ритейл Поводов")
    void getCurrentAndFuturePromosForPartner_onlyMarketplacePromosWithoutGroupsTest() throws IOException {
        mockSaasResponse("onlyMarketplacePromosWithoutGroupsTest_saas_response.json");
        // У некоторых промо проставлены родительские, но они не предназначены для отображения в ПИ
        // (нет в ОХ/не выставлен нужный флаг)
        onlyMarketplacePromosWithoutGroupsTest_mockAnaplanPromosFromDataCampResponse();
        onlyMarketplacePromosWithoutGroupsTest_mockParentPromosFromDataCampResponse();

        long businessId = 1001;
        long partnerId = 1111;
        ResponseEntity<String> response = getCurrentAndFuturePromosForPartner(businessId, partnerId);

        // Намеренно не проверяем маппинг описаний промо
        String expected = getResource(this.getClass(), "onlyMarketplacePromosWithoutGroupsTest_response.json");
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    @Test
    @DbUnitDataSet(before = "CurrentAndFuturePromosControllerTest/onlyMarketplaceCF.before.csv")
    @DisplayName("Кейс, когда есть акции с информацией только для ПИ из КИ")
    void getCurrentAndFuturePromosForPartner_onlyCFpromos() throws IOException {
        mockSaasResponse("onlyMarketplaceCF_saas_response.json");
        DataCampPromo.PromoDescription cfPromo = createAnaplanDirectDiscountDescription("cf_1111");
        cfPromo = cfPromo.toBuilder()
                .setAdditionalInfo(
                        cfPromo.getAdditionalInfo().toBuilder()
                                .clearStrategyType()
                )
                .build();

        mockAnaplanPromosFromDataCampResponse(List.of(cfPromo));
        mockParentPromosFromDataCampResponse(Collections.emptyList());

        long businessId = 1001;
        long partnerId = 1111;
        ResponseEntity<String> response = getCurrentAndFuturePromosForPartner(businessId, partnerId);

        // Намеренно не проверяем маппинг описаний промо
        String expected = getResource(this.getClass(), "onlyMarketplaceCF_response.json");
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    private void onlyMarketplacePromosWithoutGroupsTest_mockAnaplanPromosFromDataCampResponse() {
        DataCampPromo.PromoDescription anaplanCAG1 = addParentPromo(
                "SP#11",
                createAnaplanCheapestAsGiftDescription("#1111")
        );
        DataCampPromo.PromoDescription anaplanCAG2 = addParentPromo(
                "SP#22",
                createAnaplanCheapestAsGiftDescription("#2222")
        );
        DataCampPromo.PromoDescription anaplanCAG3 = createAnaplanCheapestAsGiftDescription("#3333");
        mockAnaplanPromosFromDataCampResponse(List.of(anaplanCAG1, anaplanCAG2, anaplanCAG3));
    }

    private void onlyMarketplacePromosWithoutGroupsTest_mockParentPromosFromDataCampResponse() {
        DataCampPromo.PromoDescription parentPromo = createParentPromo("SP#11", "Promo group SP#11").toBuilder()
                .clearMechanicsData()
                .build();
        mockParentPromosFromDataCampResponse(List.of(parentPromo));
    }

    @Test
    @DbUnitDataSet(before = "CurrentAndFuturePromosControllerTest/allPromoTypesTest.before.csv")
    @DisplayName("Кейс, когда есть все типы акций: партнерские, маркетплейсные с РП и без РП")
    void getCurrentAndFuturePromosForPartner_allPromoTypesTest() throws IOException {
        mockSaasResponse("allPromoTypesTest_saas_response.json");
        allPromoTypesTest_mockPartnerPromosFromDataCampResponse();
        allPromoTypesTest_mockAnaplanPromosFromDataCampResponse();
        allPromoTypesTest_mockParentPromosFromDataCampResponse();

        long businessId = 1001;
        long partnerId = 1111;
        ResponseEntity<String> response = getCurrentAndFuturePromosForPartner(businessId, partnerId);

        // Намеренно не проверяем маппинг описаний промо
        String expected = getResource(this.getClass(), "allPromoTypesTest_response.json");
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    private void allPromoTypesTest_mockPartnerPromosFromDataCampResponse() {
        DataCampPromo.PromoDescription partnerPromocode = createPartnerPromocodeDescription(
                "1111_PROMOCODE"
        );
        mockPartnerPromosFromDataCampResponse(List.of(partnerPromocode));
    }

    private void allPromoTypesTest_mockAnaplanPromosFromDataCampResponse() {
        DataCampPromo.PromoDescription anaplanCAG1 = addParentPromo(
                "SP#201",
                createAnaplanCheapestAsGiftDescription("#1111")
        );
        DataCampPromo.PromoDescription anaplanCAG2 = addParentPromo(
                "SP#201",
                createAnaplanCheapestAsGiftDescription("#2222")
        );
        DataCampPromo.PromoDescription anaplanCAG3 = createAnaplanCheapestAsGiftDescription("#3333");
        mockAnaplanPromosFromDataCampResponse(List.of(anaplanCAG1, anaplanCAG2, anaplanCAG3));
    }

    private void allPromoTypesTest_mockParentPromosFromDataCampResponse() {
        DataCampPromo.PromoDescription parentPromo = createParentPromo("SP#201", "Promo group SP#201");
        mockParentPromosFromDataCampResponse(List.of(parentPromo));
    }

    @Test
    @DisplayName("Получение списка механик при отсутствии подходящих акций")
    void getMechanicsForCurrentAndFuturePromos_noPromosTest() throws IOException {
        // В ответе саас возвращаются какие-то промки, которые после не возвращаются из ОХ (например, прошедшие).
        // Такие не должны учитываться/отдаваться дальше.
        mockSaasResponse("noPromosTest_saas_response.json");

        long businessId = 1001;
        long partnerId = 1111;
        ResponseEntity<String> response = getMechanicsForCurrentAndFuturePromos(businessId, partnerId);

        String expected = "{\"mechanics\":[]}";
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    @Test
    @DisplayName("Получение списка механик, когда есть все типы акций: партнерские, маркетплейсные с РП и без РП")
    void getMechanicsForCurrentAndFuturePromos_allPromoTypesTest() throws IOException {
        mockSaasResponse("allPromoTypesTest_saas_response.json");
        allPromoTypesTest_mockPartnerPromosFromDataCampResponse();
        allPromoTypesTest_mockAnaplanPromosFromDataCampResponse();
        allPromoTypesTest_mockParentPromosFromDataCampResponse();

        long businessId = 1001;
        long partnerId = 1111;
        ResponseEntity<String> response = getMechanicsForCurrentAndFuturePromos(businessId, partnerId);

        String expected = getResource(this.getClass(), "mechanics_allPromoTypesTest_response.json");
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    @Test
    @DisplayName("Получение всех акций, в которых участвует конкретный оффер")
    void getCurrentAndFuturePromosForPartner_withOfferId() throws IOException {
        int businessId = 1001;
        int partnerId = 1111;
        String offerId = "offerId1";
        mockSaasResponse("withOfferIdTest_first_saas_response.json");
        mockSaasWithOfferIdResponse("withOfferIdTest_second_saas_response.json");
        withOfferId_mockParentPromosFromDataCampResponse();
        withOfferId_mockAnaplanPromosFromDataCampResponse();

        ResponseEntity<String> response = getCurrentAndFuturePromosForPartner(businessId, partnerId, offerId);

        String expected = getResource(this.getClass(), "withOfferIdTest_response.json");
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    private void mockSaasWithOfferIdResponse(String fileName) throws IOException {
        SaasSearchResponse saasResponse = readJson(this.getClass(), fileName, SaasSearchResponse.class);
        when(saasService.searchShopGroupings(anyLong(), eq(0), any(), any(), anyInt(), anyInt(), isNotNull()))
                .thenReturn(saasResponse);
    }

    private void withOfferId_mockParentPromosFromDataCampResponse() {
        DataCampPromo.PromoDescription parentPromo = createParentPromo("SP#201", "Promo group SP#201");
        mockParentPromosFromDataCampResponse(List.of(parentPromo));
    }

    private void withOfferId_mockAnaplanPromosFromDataCampResponse() {
        DataCampPromo.PromoDescription anaplanCAG1 = addParentPromo(
                "SP#201",
                createAnaplanCheapestAsGiftDescription("#1111")
        );
        DataCampPromo.PromoDescription anaplanCAG3 = createAnaplanCheapestAsGiftDescription("#3333");
        DataCampPromo.PromoDescription partnerPromocode = createPartnerPromocodeDescription(
                "1111_PROMOCODE"
        );
        mockAnaplanAndPartnerPromosDataCampResponse(List.of(partnerPromocode, anaplanCAG3, anaplanCAG1));
    }

    private void mockAnaplanAndPartnerPromosDataCampResponse(Collection<DataCampPromo.PromoDescription> promos) {
        ArgumentMatcher<GetPromoBatchRequestWithFilters> ordinaryAnaplanPromosRequest = request ->
                request.getRequest().getEntriesCount() == 3;
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(
                                DataCampPromo.PromoDescriptionBatch.newBuilder()
                                        .addAllPromo(promos)
                                        .build()
                        )
                        .build()
        ).when(dataCampClient).getPromos(argThat(ordinaryAnaplanPromosRequest));
    }

    private void mockSaasResponse(String fileName) throws IOException {
        SaasSearchResponse saasResponse = readJson(this.getClass(), fileName, SaasSearchResponse.class);
        when(saasService.searchShopGroupings(anyLong(), eq(0), any(), any(), anyInt(), anyInt(), isNull()))
                .thenReturn(saasResponse);
    }

    private void mockPartnerPromosFromDataCampResponse(Collection<DataCampPromo.PromoDescription> promos) {
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(
                                DataCampPromo.PromoDescriptionBatch.newBuilder()
                                        .addAllPromo(promos)
                                        .build()
                        )
                        .build()
        ).when(dataCampClient).getPromos(any(PromoDatacampRequest.class));
    }

    private void mockAnaplanPromosFromDataCampResponse(Collection<DataCampPromo.PromoDescription> promos) {
        // Отличаем запрос за описаниями обычных маркетплейсных промок от запроса родительских
        // по формату идентификатора первой акции.
        ArgumentMatcher<GetPromoBatchRequestWithFilters> ordinaryAnaplanPromosRequest = request ->
                request.getRequest().getEntries(0).getPromoId().startsWith(ANAPLAN_PREFIX) ||
                        request.getRequest().getEntries(0).getPromoId().startsWith(CATEGORY_FACE_PREFIX);
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(
                                DataCampPromo.PromoDescriptionBatch.newBuilder()
                                        .addAllPromo(promos)
                                        .build()
                        )
                        .build()
        ).when(dataCampClient).getPromos(argThat(ordinaryAnaplanPromosRequest));
    }

    private void mockParentPromosFromDataCampResponse(Collection<DataCampPromo.PromoDescription> promos) {
        // Отличаем запрос за описаниями обычных маркетплейсных промок от запроса родительских
        // по формату идентификатора первой акции.
        ArgumentMatcher<GetPromoBatchRequestWithFilters> ordinaryAnaplanPromosRequest = request ->
                !request.getRequest().getEntries(0).getPromoId().startsWith(ANAPLAN_PREFIX) &&
                        !request.getRequest().getEntries(0).getPromoId().startsWith(CATEGORY_FACE_PREFIX);
        doReturn(
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(
                                DataCampPromo.PromoDescriptionBatch.newBuilder()
                                        .addAllPromo(promos)
                                        .build()
                        )
                        .build()
        ).when(dataCampClient).getPromos(argThat(ordinaryAnaplanPromosRequest));
    }

    private DataCampPromo.PromoDescription addParentPromo(
            String parentPromoId,
            DataCampPromo.PromoDescription originalDescription
    ) {
        DataCampPromo.PromoAdditionalInfo updatedAdditionalInfo = originalDescription.getAdditionalInfo().toBuilder()
                .setParentPromoId(parentPromoId)
                .build();
        return originalDescription.toBuilder()
                .setAdditionalInfo(updatedAdditionalInfo)
                .build();
    }

    private DataCampPromo.PromoDescription createParentPromo(
            String parentPromoId,
            String parentPromoName
    ) {
        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(parentPromoId)
                                .build()
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.PARENT_PROMO)
                                .build()
                )
                .setAdditionalInfo(
                        DataCampPromo.PromoAdditionalInfo.newBuilder()
                                .setName(parentPromoName)
                                .buildPartial()
                )
                .setMechanicsData(
                        DataCampPromo.PromoMechanics.newBuilder()
                                .setPiParentPromo(
                                        DataCampPromo.PromoMechanics.PiParentPromo.newBuilder()
                                                .setToTopInPi(true)
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    private ResponseEntity<String> getCurrentAndFuturePromosForPartner(
            long businessId,
            long partnerId
    ) {
        return FunctionalTestHelper.get(
                baseUrl() + "/partner/promo/description/current-and-future-promos?" +
                        "partnerId=" + partnerId + "&businessId=" + businessId
        );
    }

    private ResponseEntity<String> getCurrentAndFuturePromosForPartner(
            long businessId,
            long partnerId,
            String offerId
    ) {
        return FunctionalTestHelper.get(
                baseUrl() + "/partner/promo/description/current-and-future-promos?" +
                        "partnerId=" + partnerId + "&businessId=" + businessId + "&ssku=" + offerId
        );
    }

    private ResponseEntity<String> getMechanicsForCurrentAndFuturePromos(
            long businessId,
            long partnerId
    ) {
        return FunctionalTestHelper.get(
                baseUrl() + "/partner/promo/description/current-and-future-promos/mechanics?" +
                        "partnerId=" + partnerId + "&businessId=" + businessId
        );
    }
}
