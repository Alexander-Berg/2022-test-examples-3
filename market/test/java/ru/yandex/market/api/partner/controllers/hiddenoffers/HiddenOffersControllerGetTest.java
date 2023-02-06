package ru.yandex.market.api.partner.controllers.hiddenoffers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.helper.PartnerApiFunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.model.search.filter.HidingSource;
import ru.yandex.market.mbi.datacamp.saas.impl.SaasDatacampService;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.DisabledBySourceAttribute;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mbi.datacamp.model.search.filter.HidingSource.MARKET_PRICELABS;
import static ru.yandex.market.mbi.datacamp.model.search.filter.HidingSource.PUSH_PARTNER_API;

/**
 * Тесты на получение скрытых офферов из ручек {@link HiddenOffersController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@DbUnitDataSet(before = "HiddenOffersController/get/csv/get.before.csv")
class HiddenOffersControllerGetTest extends AbstractHiddenOffersControllerFunctionalTest {

    private static final Set<HidingSource> DISABLED_FLAGS = Set.of(PUSH_PARTNER_API, MARKET_PRICELABS);

    private static final Predicate<SearchBusinessOffersRequest> TEST_REQUEST_WITH_POSITION =
            request -> request.getOffset() == null && "392f".equals(request.getPageRequest().seekKey().orElse(null));
    private static final Predicate<SearchBusinessOffersRequest> TEST_REQUEST_WITH_OFFSET =
            request -> request.getPageRequest().seekKey().isEmpty() && Integer.valueOf(20).equals(request.getOffset());

    @Qualifier("dataCampShopClient")
    @Autowired
    private DataCampClient dataCampShopClient;
    @Autowired
    private SaasDatacampService saasService;

    @Test
    @DisplayName("Получение скрытых офферов для белого пуша")
    @DbUnitDataSet(before = "HiddenOffersController/get/csv/shopPush.PL.before.csv")
    void testShopPush() {
        assertResponse(101003L, "testShopPush", 0);
    }

    @Test
    @DisplayName("Получение скрытых офферов для белого пулла")
    @DbUnitDataSet(before = "HiddenOffersController/get/csv/shopPull.PL.before.csv")
    void testShopPull() {
        assertResponse(101002L, "testShopPull", 0);
    }

    @Test
    @DisplayName("Получение скрытых офферов для синего")
    @DbUnitDataSet(before = "HiddenOffersController/get/csv/supplier.PL.before.csv")
    void testSupplier() {
        assertResponse(101004L, "testSupplier", 0);
    }

    @Test
    @DisplayName("Получение скрытых офферов для белого пуша из ЕОХ, если вернулось 0 результатов")
    @DbUnitDataSet(
            before = "HiddenOffersController/get/csv/env.csv"
    )
    void getHiddenOffers_emptyInfoFromDatacamp_emptyResult() {
        mockSaasService(0, 1003L);
        mockDatacampShopClient("emptyProto", TEST_REQUEST_WITH_POSITION);

        assertResponse("emptyResult", getJsonUrl(101003L, getParams()));
        verify(saasService, only()).searchBusinessOffers(any());
    }

    @Test
    @DisplayName("Получение скрытых офферов для белого пуша из ЕОХ без данных в БД")
    @DbUnitDataSet(
            before = "HiddenOffersController/get/csv/env.csv"
    )
    void getHiddenOffers_shopPushWithoutInfoInDb_withoutComment() {
        mockSaasService(3, 1003L);
        mockDatacampShopClient("shopPartner", TEST_REQUEST_WITH_POSITION);

        assertResponse("datacampShopWithoutDb", getJsonUrl(101003L, getParams()));
        verify(saasService, only()).searchBusinessOffers(any());
    }

    @Test
    @DisplayName("Получение скрытых офферов для белого пуша из ЕОХ")
    @DbUnitDataSet(
            before = {
                    "HiddenOffersController/get/csv/env.csv",
                    "HiddenOffersController/get/csv/datacampShop.before.csv"
            }
    )
    void getHiddenOffers_shopPush_fullInfo() {
        mockSaasService(3, 1003L);
        mockDatacampShopClient("shopPartner", TEST_REQUEST_WITH_POSITION);

        assertResponse("datacampShop", getJsonUrl(101003L, getParams()));
        verify(saasService, only()).searchBusinessOffers(any());
    }

    @Test
    @DisplayName("У оффера есть неактивное приоритетное скрытие. Не должны его учитывать")
    @DbUnitDataSet(
            before = {
                    "HiddenOffersController/get/csv/env.csv",
                    "HiddenOffersController/get/csv/datacampShop.before.csv"
            }
    )
    void getHiddenOffers_shopPush_disabledHiding() {
        mockSaasService(3, 1003L);
        mockDatacampShopClient("shopPartnerWithDisabledHiding", TEST_REQUEST_WITH_POSITION);

        assertResponse("shopPartnerWithDisabledHiding", getJsonUrl(101003L, getParams()));
        verify(saasService, only()).searchBusinessOffers(any());
    }

    @Test
    @DisplayName("Получение скрытых офферов для белого пуша из ЕОХ с передачей offset")
    @DbUnitDataSet(
            before = {
                    "HiddenOffersController/get/csv/env.csv",
                    "HiddenOffersController/get/csv/datacampShop.before.csv"
            }
    )
    void getHiddenOffers_shopPushWithOffset_fullInfo() {
        mockSaasService(3, 1003L);
        mockDatacampShopClient("shopPartnerWithOffset", TEST_REQUEST_WITH_OFFSET);

        assertResponse(101003L, "datacampShopWithOffset", 20);
        verify(saasService, only()).searchBusinessOffers(any());
    }

    @Test
    @DisplayName("Получение скрытых офферов для синего пуша из ЕОХ")
    @DbUnitDataSet(
            before = {
                    "HiddenOffersController/get/csv/env.csv",
                    "HiddenOffersController/get/csv/datacampSupplier.before.csv"
            }
    )
    void getHiddenOffers_supplierPush_fullInfo() {
        mockSaasService(197, 1004L);
        Mockito.doReturn(getUnitedOffersResponse("supplierPartner"))
                .when(dataCampShopClient)
                .searchBusinessOffers(
                        Mockito.argThat(request ->
                                Long.valueOf(1004L).equals(request.getPartnerId())
                                        && DISABLED_FLAGS.equals(new HashSet<>(request.getDisabledFlags()))
                                        && Integer.valueOf(500).equals(request.getPageRequest().limit())
                                        && request.getPageRequest().seekKey().isEmpty()
                                        && request.getOffset() == null
                                        && request.getOfferIds().isEmpty()
                        )
                );

        assertResponse("datacampSupplier", getUrl(101004L, Format.JSON));
        verify(saasService, only()).searchBusinessOffers(any());
    }

    private void mockSaasService(int total, long partnerId) {
        SaasSearchResult result = SaasSearchResult.builder()
                .setTotalCount(total)
                .build();
        Mockito.doReturn(result)
                .when(saasService)
                .searchBusinessOffers(Mockito.argThat(filter ->
                        filter.getFiltersMap()
                                .containsKey(new DisabledBySourceAttribute(partnerId,
                                        DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
                                )
                ));
    }

    private void mockDatacampShopClient(String proto, Predicate<SearchBusinessOffersRequest> validate) {
        Mockito.doReturn(getUnitedOffersResponse(proto))
                .when(dataCampShopClient)
                .searchBusinessOffers(
                        Mockito.argThat(request ->
                                Long.valueOf(1003L).equals(request.getPartnerId())
                                        && DISABLED_FLAGS.equals(new HashSet<>(request.getDisabledFlags()))
                                        && Integer.valueOf(10).equals(request.getPageRequest().limit())
                                        && validate.test(request)
                                        && request.getOfferIds().isEmpty()
                        )
                );
    }

    @Nonnull
    private Multimap<String, Object> getParams() {
        Multimap<String, Object> params = LinkedListMultimap.create();
        params.put("page_token", encodePageToken());
        params.put("limit", 10);
        return params;
    }

    private String encodePageToken() {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("392f".getBytes(StandardCharsets.UTF_8));
    }

    private void assertResponse(long campaignId, String file, int offset) {
        Multimap<String, Object> params = LinkedListMultimap.create();
        params.put("offset", offset);
        params.put("limit", 10);

        assertResponse(file, getJsonUrl(campaignId, params));
    }

    private void assertResponse(String test, String url) {
        ResponseEntity<String> response = PartnerApiFunctionalTestHelper.getForJson(url, USER_ID);

        Assertions.assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        MbiAsserts.assertJsonEquals(fileToString("get", test), response.getBody());
    }

    private SearchBusinessOffersResult getUnitedOffersResponse(String fileName) {
        SyncGetOffer.GetUnitedOffersResponse strollerResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "HiddenOffersController/get/proto/" + fileName + ".json",
                HiddenOffersControllerGetTest.class
        );
        return DataCampStrollerConversions.fromStrollerResponse(strollerResponse);
    }
}
