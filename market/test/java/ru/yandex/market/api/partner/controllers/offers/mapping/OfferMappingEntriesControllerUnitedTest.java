package ru.yandex.market.api.partner.controllers.offers.mapping;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferPictures;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.PartnerCategoryOuterClass;
import Market.DataCamp.SyncAPI.SyncCategory;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.logbroker.event.datacamp.DatacampMessageLogbrokerEvent;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static ru.yandex.market.api.partner.helper.PartnerApiFunctionalTestHelper.postForJson;
import static ru.yandex.market.core.logbroker.util.DatacampMessageLogbrokerTestUtil.assertAndGetOffers;
import static ru.yandex.market.core.logbroker.util.DatacampMessageLogbrokerTestUtil.getLogbrokerEvents;

/**
 * Тест на {@link OfferMappingEntriesController} для магазинов в ЕОХ с включенной настройкой
 * Date: 13.01.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "united/csv/before.csv")
class OfferMappingEntriesControllerUnitedTest extends OfferMappingEntriesTest {

    private static final String USER_ID = "67282295";

    @DisplayName("Отправка события об изменение ассортимента в ЕОХ через логброкер")
    @Test
    void saveMappings_sendAllFieldInRequest_sendToLogbrokerWithOkReturn() {
        mockCatsResponse("datacamp.categories.response.json");

        List<DatacampMessageLogbrokerEvent> actualEvents = sendAndCheckRequest("106669", "updatesAllField.request");
        Map<DataCampOfferIdentifiers.OfferIdentifiers, DataCampUnitedOffer.UnitedOffer> offers =
                assertAndGetOffers(actualEvents, 0, 6669, ProtoTestUtil.getIgnoredFields(".*status.*"));

        Assertions.assertThat(offers)
                .hasSize(2);

        assertOffer(offers, "first");
        assertOffer(offers, "second");

        checkCatsInOffer(offers.values(), List.of(
                Pair.of("category-name-supplier1", 100L),
                Pair.of("category-name-supplier2", 200L)
        ));
    }

    @Test
    @DisplayName("В запросе часть категорий - новые, часть уже есть под бизнесом. Создаем новые")
    void testCreateNewCats() {
        mockCatsResponse("datacamp.cat100.response.json");
        mockCatsCreationResponse("datacamp.cat200.response.json");

        List<DatacampMessageLogbrokerEvent> actualEvents = sendAndCheckRequest("106669", "updatesAllField.request");
        Map<DataCampOfferIdentifiers.OfferIdentifiers, DataCampUnitedOffer.UnitedOffer> offers =
                assertAndGetOffers(actualEvents, 0, 6669, ProtoTestUtil.getIgnoredFields(".*status.*"));

        Assertions.assertThat(offers)
                .hasSize(2);

        assertOffer(offers, "first");
        assertOffer(offers, "second");

        checkCatsInOffer(offers.values(), List.of(
                Pair.of("category-name-supplier1", 100L),
                Pair.of("category-name-supplier2", 200L)
        ), "category-name-supplier2");
    }

    @Test
    @DisplayName("В запросе все категории новые. Создаем их")
    void testCreateAllCats() {
        mockCatsResponse("datacamp.empty-cat.response.json");
        mockCatsCreationResponse("datacamp.categories.response.json");

        List<DatacampMessageLogbrokerEvent> actualEvents = sendAndCheckRequest("106669", "updatesAllField.request");
        Map<DataCampOfferIdentifiers.OfferIdentifiers, DataCampUnitedOffer.UnitedOffer> offers =
                assertAndGetOffers(actualEvents, 0, 6669, ProtoTestUtil.getIgnoredFields(".*status.*"));

        Assertions.assertThat(offers)
                .hasSize(2);

        assertOffer(offers, "first");
        assertOffer(offers, "second");

        checkCatsInOffer(offers.values(), List.of(
                Pair.of("category-name-supplier1", 100L),
                Pair.of("category-name-supplier2", 200L)
        ), "category-name-supplier1", "category-name-supplier2");
    }

    @DisplayName("Отправка события об изменение ассортимента в ЕОХ через логброкер с репликацией по складам")
    @Test
    void saveMappings_replicationByWarehouse_sendToLogbrokerWithOkReturn() {
        mockCatsResponse("datacamp.categories.response.json");

        List<DatacampMessageLogbrokerEvent> actualEvents = sendAndCheckRequest("108032", "updatesAllField.request");
        Map<DataCampOfferIdentifiers.OfferIdentifiers, DataCampUnitedOffer.UnitedOffer> offers =
                assertAndGetOffers(actualEvents, 0, 8032, ProtoTestUtil.getIgnoredFields(".*status.*"));

        Assertions.assertThat(offers)
                .hasSize(8);

        assertOffer(offers, "firstCrossdock");
        assertOffer(offers, "firstDropship");
        assertOffer(offers, "firstDbs");
        assertOffer(offers, "secondCrossdock");
        assertOffer(offers, "secondDropship");
        assertOffer(offers, "secondDbs");

        checkCatsInOffer(offers.values(), List.of(
                Pair.of("category-name-supplier1", 100L),
                Pair.of("category-name-supplier1", 100L),
                Pair.of("category-name-supplier1", 100L),
                Pair.of("category-name-supplier1", 100L),
                Pair.of("category-name-supplier2", 200L),
                Pair.of("category-name-supplier2", 200L),
                Pair.of("category-name-supplier2", 200L),
                Pair.of("category-name-supplier2", 200L)
        ));
    }

    private void checkCatsInOffer(Collection<DataCampUnitedOffer.UnitedOffer> offers,
                                  List<Pair<String, Long>> expected,
                                  String... createdCats) {
        // вытаскиваем категории, которые передали в ЕОХ в оффере в топик
        List<Pair<String, Long>> cats = offers.stream()
                .map(DataCampUnitedOffer.UnitedOffer::getBasic)
                .map(DataCampOffer.Offer::getContent)
                .map(DataCampOfferContent.OfferContent::getPartner)
                .map(DataCampOfferContent.PartnerContent::getOriginal)
                .map(DataCampOfferContent.OriginalSpecification::getCategory)
                .map(e -> Pair.of(e.getName(), e.getId()))
                .collect(Collectors.toList());
        Assertions.assertThat(cats)
                .containsExactlyInAnyOrderElementsOf(expected);

        // Проверяем, какие из категорий мы создали перед отправкой
        if (createdCats.length > 0) {
            var requestCaptor = ArgumentCaptor.forClass(SyncCategory.UpdatePartnerCategories.class);
            Mockito.verify(dataCampClient).addNewCategoriesToBusiness(requestCaptor.capture(), any());
            Set<String> catsInReq = requestCaptor.getAllValues().stream()
                    .map(SyncCategory.UpdatePartnerCategories::getCategories)
                    .map(PartnerCategoryOuterClass.PartnerCategoriesBatch::getCategoriesList)
                    .flatMap(Collection::stream)
                    .map(PartnerCategoryOuterClass.PartnerCategory::getName)
                    .collect(Collectors.toSet());
            Assertions.assertThat(catsInReq)
                    .containsExactlyInAnyOrder(createdCats);
        } else {
            Mockito.verify(dataCampClient, Mockito.never()).addNewCategoriesToBusiness(any(), any());
        }
    }

    void mockCatsResponse(String path) {
        SyncCategory.PartnerCategoriesResponse catResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncCategory.PartnerCategoriesResponse.class,
                path,
                getClass()
        );
        Mockito.when(dataCampClient.getCategories(anyLong(), any())).thenReturn(catResponse);
    }

    void mockCatsCreationResponse(String path) {
        SyncCategory.PartnerCategoriesResponse catResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncCategory.PartnerCategoriesResponse.class,
                path,
                getClass()
        );
        Mockito.when(dataCampClient.addNewCategoriesToBusiness(any(), any())).thenReturn(catResponse);
    }

    @SuppressWarnings("SameParameterValue")
    @Nonnull
    private List<DatacampMessageLogbrokerEvent> sendAndCheckRequest(String campaignId, String test) {
        assertResponse(campaignId, test);

        List<DatacampMessageLogbrokerEvent> defaultLogbrokerEvents =
                getLogbrokerEvents(assortmentLogbrokerService, 1);

        Assertions.assertThat(defaultLogbrokerEvents)
                .hasSize(1);

        return defaultLogbrokerEvents;
    }

    protected void assertResponse(String campaignId, String test) {
        ResponseEntity<String> response = postForJson(getUrl(campaignId), fileToString(test), USER_ID);

        Assertions.assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
        MbiAsserts.assertJsonEquals(fileToString("successful.response"), response.getBody());
    }

    @Nonnull
    private String getUrl(String campaignId) {
        return urlBasePrefix + "/campaigns/" + campaignId + "/offer-mapping-entries/updates.json";
    }

    @Nonnull
    private String fileToString(String test) {
        return StringTestUtil.getString(this.getClass(), "united/json/" + test + ".json");
    }

    private void assertOffer(Map<DataCampOfferIdentifiers.OfferIdentifiers, DataCampUnitedOffer.UnitedOffer> offers,
                             String test) {
        DataCampOfferIdentifiers.OfferIdentifiers offerIdentifiers = ProtoTestUtil.getProtoMessageByJson(
                DataCampOfferIdentifiers.OfferIdentifiers.class,
                "united/proto/" + test + "ShopSkuOfferIdentifier.json",
                getClass()
        );
        DataCampUnitedOffer.UnitedOffer offer = offers.get(offerIdentifiers);
        Assertions.assertThat(offer)
                .isNotNull();

        DataCampOfferStatus.OfferStatus serviceStatus = ProtoTestUtil.getProtoMessageByJson(
                DataCampOfferStatus.OfferStatus.class,
                "united/proto/serviceStatus.json",
                getClass()
        );
        DataCampOfferPictures.OfferPictures offerPictures = ProtoTestUtil.getProtoMessageByJson(
                DataCampOfferPictures.OfferPictures.class,
                "united/proto/offerPictures.json",
                getClass()
        );

        DataCampOffer.Offer basic = offer.getBasic();
        Assertions.assertThat(basic.getStatus().hasUnitedCatalog())
                .isFalse();

        DataCampOffer.Offer service = offer.getServiceMap().get(offerIdentifiers.getShopId());
        ProtoTestUtil.assertThat(service.getStatus())
                .ignoringFieldsMatchingRegexes(".*timestamp_.*")
                .isEqualTo(serviceStatus);
        ProtoTestUtil.assertThat(service.getPictures())
                .ignoringFieldsMatchingRegexes(".*timestamp_.*")
                .isEqualTo(offerPictures);
    }
}
