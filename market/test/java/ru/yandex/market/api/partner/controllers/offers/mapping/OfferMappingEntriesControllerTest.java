package ru.yandex.market.api.partner.controllers.offers.mapping;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import com.googlecode.protobuf.format.JsonFormat;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriTemplate;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerServiceStub;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mdm.http.MasterDataProto;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты на {@link OfferMappingEntriesController}.
 */
class OfferMappingEntriesControllerTest extends OfferMappingEntriesTest {

    @Autowired
    private UltraControllerServiceStub ultraControllerClient;

    private String getFileContent(String filename) {
        return StringTestUtil.getString(this.getClass().getResourceAsStream(filename));
    }

    @Test
    @DbUnitDataSet(before = {
            "OfferMappingEntriesControllerUnitedCatalogPartnerTest.before.csv",
            "OfferMappingEntriesControllerTest.categories.csv"
    })
    void getMappingTestXmlEkat() throws IOException {
        SyncGetOffer.GetUnitedOffersResponse.Builder builder = SyncGetOffer.GetUnitedOffersResponse.newBuilder();
        JsonFormat.merge(getFileContent("datacampOffers.json"), builder);
        Mockito.when(dataCampClient.searchBusinessOffers(any()))
                .thenReturn(DataCampStrollerConversions.fromStrollerResponse(builder.build()));
        ResponseEntity<String> response = getEntries(Format.XML, "?shop_sku=no_here");
        MatcherAssert.assertThat(response, MoreMbiMatchers.responseBodyMatches(
                MbiMatchers.xmlEquals(getFileContent("response/GetUnitedMappingTest.expected.xml")
                )));

        ArgumentCaptor<SearchBusinessOffersRequest> captor =
                ArgumentCaptor.forClass(SearchBusinessOffersRequest.class);
        verify(dataCampClient).searchBusinessOffers(captor.capture());
        Assertions.assertEquals(333, captor.getValue().getBusinessId());
        assertThat(captor.getValue().getOfferIds()).containsExactlyInAnyOrder("no_here");
    }

    @Test
    @DbUnitDataSet(before = {
            "OfferMappingEntriesControllerUnitedCatalogPartnerTest.before.csv",
            "OfferMappingEntriesControllerTest.categories.csv"
    })
    void getMappingTestJsonEkat() throws IOException {
        SyncGetOffer.GetUnitedOffersResponse.Builder builder = SyncGetOffer.GetUnitedOffersResponse.newBuilder();
        JsonFormat.merge(getFileContent("datacampOffers.json"), builder);
        Mockito.when(dataCampClient.searchBusinessOffers(any()))
                .thenReturn(DataCampStrollerConversions.fromStrollerResponse(builder.build()));
        ResponseEntity<String> response = getEntries(Format.JSON, "?shop_sku=no_here");
        MatcherAssert.assertThat(response, MoreMbiMatchers.responseBodyMatches(
                MbiMatchers.jsonEquals(getFileContent("response/GetUnitedMappingTest.expected.json")
                )));

        ArgumentCaptor<SearchBusinessOffersRequest> captor =
                ArgumentCaptor.forClass(SearchBusinessOffersRequest.class);
        verify(dataCampClient).searchBusinessOffers(captor.capture());
        Assertions.assertEquals(333, captor.getValue().getBusinessId());
        assertThat(captor.getValue().getOfferIds()).containsExactlyInAnyOrder("no_here");
    }

    @Test
    void postMappingNullPositiveTestXml() {
        //language=xml
        String request = "<offer-mapping-entries-update></offer-mapping-entries-update>";
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.XML.formatName());
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.XML, request);

        MbiAsserts.assertXmlEquals(XML_OK, response.getBody());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerTest.isEmptyOffersError.before.csv")
    void postMappingNullBadRequestTestXml() {
        //language=xml
        String request = "<offer-mapping-entries-update></offer-mapping-entries-update>";
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.XML.formatName());
        var ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.XML, request)
        );

        MbiAsserts.assertXmlEquals(XML_BAD_REQUEST, ex.getResponseBodyAsString());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    void postMappingEmptyPositiveTestXml() {
        //language=xml
        String request =
                "<offer-mapping-entries-update>" +
                        "    <offer-mapping-entries/>" +
                        "</offer-mapping-entries-update>";
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.XML.formatName());
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.XML, request);

        MbiAsserts.assertXmlEquals(XML_OK, response.getBody());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    void postMappingNullPositiveTestJson() {
        //language=json
        String request = "{}";
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.JSON.formatName());
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request);

        MbiAsserts.assertJsonEquals(JSON_OK, response.getBody());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    void postMappingEmptyPositiveTestJson() {
        //language=json
        String request = "{\"offerMappingEntries\":[]}";
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.JSON.formatName());
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request);

        MbiAsserts.assertJsonEquals(JSON_OK, response.getBody());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerTest.isEmptyOffersError.before.csv")
    void postMappingEmptyBadRequestTestJson() {
        //language=json
        String request = "{\"offerMappingEntries\":[]}";
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.JSON.formatName());
        var ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request)
        );

        MbiAsserts.assertJsonEquals(JSON_BAD_REQUEST, ex.getResponseBodyAsString());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerUnitedCatalogPartnerTest.before.csv")
    void postMappingAmbiguousErrorTest() {
        prepareMocks(emptyList(), emptyList());

        String request = getFileContent("request/PostMappingAmbiguousError.request.json");
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, SUPPLIER_CAMPAIGN_ID, Format.JSON.formatName());
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request));

        String expected = getFileContent("response/PostMappingAmbiguousError.expected.json");
        MbiAsserts.assertJsonEquals(expected, exception.getResponseBodyAsString());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    void postMappingWithFilledProcessingStateError() {
        prepareMocks(emptyList(), emptyList());
        String request = getFileContent("request/PostMappingWithFilledProcessingStateError.request.json");
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.JSON.formatName());
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request)
        );

        String expected = getFileContent("response/PostMappingWithFilledProcessingStateError.expected.json");
        MbiAsserts.assertJsonEquals(expected, exception.getResponseBodyAsString());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    void postMappingNPEOffer1TestJson() {
        prepareMocks(emptyList(), emptyList());
        //language=json
        String request = "{ " +
                "  \"offerMappingEntries\": [ " +
                "    { " +
                "      \"offer\": { " +
                "      }, " +
                "      \"mapping\": { " +
                "        \"marketSku\": 123456789 " +
                "      } " +
                "    }" +
                "  ]" +
                "}";
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.JSON.formatName());
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request)
        );

        //language=json
        String expected = "{\"status\": \"ERROR\"," +
                "  \"errors\": [" +
                "    {" +
                "      \"code\": \"MISSING_OFFER_ID\"," +
                "      \"message\": \"Offer at position 1 should have either shop-sku or id\"" +
                "    }" +
                "  ]" +
                "}";
        MbiAsserts.assertJsonEquals(expected, exception.getResponseBodyAsString());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerUnitedCatalogPartnerTest.before.csv")
    void postMappingNpeOfferForSupplierTestJson() {
        prepareMocks(emptyList(), emptyList());
        //language=json
        String request = "{ " +
                "  \"offerMappingEntries\": [ " +
                "    { " +
                "      \"mapping\": { " +
                "        \"marketSku\": 123456789 " +
                "      }" +
                "    }" +
                "  ]" +
                "}";
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, SUPPLIER_CAMPAIGN_ID, Format.JSON.formatName());
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request)
        );

        //language=json
        String expected = "{\"status\": \"ERROR\"," +
                "  \"errors\": [" +
                "    {" +
                "      \"code\": \"INVALID_SHOP_SKU\"," +
                "      \"message\": \"Offer at position 1 should have either shop-sku or id\"" +
                "    }" +
                "  ]" +
                "}";
        MbiAsserts.assertJsonEquals(expected, exception.getResponseBodyAsString());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerUnitedCatalogPartnerTest.before.csv")
    void postUpdateOfferMappingsUnitedCatalogPartner() {
        prepareMocks(emptyList(), emptyList());
        String request = getFileContent("request/PostMappingPositiveForSupplier.request.json");
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, SUPPLIER_CAMPAIGN_ID, Format.JSON.formatName());
        var response = FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(assortmentLogbrokerService, times(1)).publishEvent(any());
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerUnitedCatalogPartnerTest.before.csv")
    void postNullMappingMapping1TestJson() {
        prepareMocks(emptyList(), emptyList());

        //language=json
        String request = "{ " +
                "  \"offerMappingEntries\": [ " +
                "    { " +
                "      \"offer\": { " +
                "        \"shopSku\": \"SKU.1\", " +
                "         \"barcodes\": [null] " +
                "      } " +
                "    }" +
                "  ]" +
                "}";
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, SUPPLIER_CAMPAIGN_ID, Format.JSON.formatName());
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request)
        );

        //language=json
        String expected = "{" +
                "  \"status\": \"ERROR\"," +
                "  \"errors\": [" +
                "    {" +
                "      \"code\": \"NO_REQUIRED_FIELDS\"," +
                "      \"message\": \"Offer at position 1 should have at least one manufacturer country\"" +
                "    }," +
                "    {" +
                "      \"code\": \"NO_REQUIRED_FIELDS\"," +
                "      \"message\": \"Offer at position 1 should have at least one url\"" +
                "    }," +
                "    {" +
                "      \"code\": \"CONSTRAINT_VIOLATION\"," +
                "      \"message\": \"Offer at position 1 should not have null value in list of barcodes\"" +
                "    }" +
                "  ]" +
                "}";
        MbiAsserts.assertJsonEquals(expected, exception.getResponseBodyAsString());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerUnitedCatalogPartnerTest.before.csv")
    void postMappingForSupplierWithNullsInCountries() {
        prepareMocks(emptyList(), emptyList());
        String request = getFileContent("request/PostMappingForSupplierWithNullsInCountries.request.json");
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, SUPPLIER_CAMPAIGN_ID, Format.JSON.formatName());
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request)
        );

        //language=json
        String expected = "{" +
                "  \"status\":\"ERROR\"," +
                "  \"errors\":[{" +
                "         \"code\":\"CONSTRAINT_VIOLATION\"," +
                "         \"message\":\"Offer at position 1 should not have null value in list of manufacturer " +
                "countries\"" +
                "  }]" +
                "}";
        MbiAsserts.assertJsonEquals(expected, exception.getResponseBodyAsString());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerUnitedCatalogPartnerTest.before.csv")
    void postMappingSomeErrorsTestXml() {
        prepareMocks(emptyList(), emptyList());
        prepareAddProductInfoErrorMock();

        String request = getFileContent("request/PostMappingSomeErrors.request.xml");
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, SUPPLIER_CAMPAIGN_ID, Format.XML.formatName());

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.XML, request)
        );

        String expected = getFileContent("response/PostMappingSomeErrors.expected.xml");
        MbiAsserts.assertXmlEquals(expected, exception.getResponseBodyAsString());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerUnitedCatalogPartnerTest.before.csv")
    void postMappingSomeErrorsTestJson() {
        prepareMocks(emptyList(), emptyList());
        prepareAddProductInfoErrorMock();
        String request = getFileContent("request/PostMappingSomeErrors.request.json");
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, SUPPLIER_CAMPAIGN_ID, Format.JSON.formatName());

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request)
        );

        prepareAddProductInfoErrorMock();
        String expected = getFileContent("response/PostMappingSomeErrors.expected.json");
        MbiAsserts.assertJsonEquals(expected, exception.getResponseBodyAsString());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerUnitedCatalogPartnerTest.before.csv")
    void postAvaliabilityUpdateJson() {
        prepareMocks(emptyList(), emptyList());
        Mockito.when(patientMboMappingsService.updateAvailability(Mockito.any()))
                .thenReturn(MboMappings.UpdateAvailabilityResponse.newBuilder()
                        .setStatus(MboMappings.UpdateAvailabilityResponse.Status.OK)
                        .build());
        //language=json
        String request = "{\n" +
                "    \"availability\": \"INACTIVE\",\n" +
                "    \"offers\": [{\"shopSku\": \"aaa\"}, {\"shopSku\": \"bbb\"}]\n" +
                "}";
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/availability-updates.{format}")
                .expand(urlBasePrefix, SUPPLIER_CAMPAIGN_ID, Format.JSON.formatName());

        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request);
        MbiAsserts.assertJsonEquals(JSON_OK, response.getBody());

        verify(patientMboMappingsService).updateAvailability(MboMappings.UpdateAvailabilityRequest
                .newBuilder()
                .setAvailability(SupplierOffer.Availability.INACTIVE)
                .addMappings(MboMappings.UpdateAvailabilityRequest.Locator.newBuilder()
                        .setSupplierId(SUPPLIER_ID)
                        .setShopSku("aaa")
                        .build())
                .addMappings(MboMappings.UpdateAvailabilityRequest.Locator.newBuilder()
                        .setSupplierId(SUPPLIER_ID)
                        .setShopSku("bbb")
                        .build())
                .build()
        );
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerUnitedCatalogPartnerTest.before.csv")
    void postAvailabilityUpdateXml() {
        prepareMocks(emptyList(), emptyList());
        Mockito.when(patientMboMappingsService.updateAvailability(Mockito.any()))
                .thenReturn(MboMappings.UpdateAvailabilityResponse.newBuilder()
                        .setStatus(MboMappings.UpdateAvailabilityResponse.Status.OK)
                        .build());
        //language=xml
        String request =
                "<offer-mapping-availability-update>\n" +
                        "    <availability>INACTIVE</availability>\n" +
                        "    <offers>\n" +
                        "        <offer>\n" +
                        "            <shop-sku>aaa</shop-sku>\n" +
                        "        </offer>\n" +
                        "        <offer>\n" +
                        "            <shop-sku>bbb</shop-sku>\n" +
                        "        </offer>\n" +
                        "    </offers>\n" +
                        "</offer-mapping-availability-update>";

        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/availability-updates.{format}")
                .expand(urlBasePrefix, SUPPLIER_CAMPAIGN_ID, Format.XML.formatName());

        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.XML, request);
        MbiAsserts.assertXmlEquals(XML_OK, response.getBody());

        verify(patientMboMappingsService).updateAvailability(MboMappings.UpdateAvailabilityRequest
                .newBuilder()
                .setAvailability(SupplierOffer.Availability.INACTIVE)
                .addMappings(MboMappings.UpdateAvailabilityRequest.Locator.newBuilder()
                        .setSupplierId(SUPPLIER_ID)
                        .setShopSku("aaa")
                        .build())
                .addMappings(MboMappings.UpdateAvailabilityRequest.Locator.newBuilder()
                        .setSupplierId(SUPPLIER_ID)
                        .setShopSku("bbb")
                        .build())
                .build()
        );
    }

    @Test
    void postAvailabilityUpdateSupplierRequireError() {
        prepareMocks(emptyList(), emptyList());
        Mockito.when(patientMboMappingsService.updateAvailability(Mockito.any()))
                .thenReturn(MboMappings.UpdateAvailabilityResponse.newBuilder()
                        .setStatus(MboMappings.UpdateAvailabilityResponse.Status.OK)
                        .build());
        //language=json
        String request = "{\n" +
                "    \"availability\": \"INACTIVE\",\n" +
                "    \"offers\": [{\"shopSku\": \"aaa\"}, {\"shopSku\": \"bbb\"}]\n" +
                "}";
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/availability-updates.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.JSON.formatName());

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request)
        );
        MbiAsserts.assertJsonEquals(
                "{\n" +
                        "  \"status\": \"ERROR\",\n" +
                        "  \"errors\": [\n" +
                        "    {\n" +
                        "      \"code\": \"BAD_REQUEST\",\n" +
                        "      \"message\": \"Campaign is not supplier\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}", exception.getResponseBodyAsString());
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerFilterTest.BusinessMigration.postAvailability.error.csv")
    void postAvailabilityUpdateXmlFailOnActiveBusinessMigration() {
        try {
            prepareMocks(emptyList(), emptyList());
            Mockito.when(patientMboMappingsService.updateAvailability(Mockito.any()))
                    .thenReturn(MboMappings.UpdateAvailabilityResponse.newBuilder()
                            .setStatus(MboMappings.UpdateAvailabilityResponse.Status.OK)
                            .build());
            //language=json
            String request = "{\n" +
                    "    \"availability\": \"INACTIVE\",\n" +
                    "    \"offers\": [{\"shopSku\": \"aaa\"}, {\"shopSku\": \"bbb\"}]\n" +
                    "}";
            URI uri =
                    new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/availability-updates.{format}")
                            .expand(urlBasePrefix, CAMPAIGN_ID, Format.JSON.formatName());

            FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request);
            Assertions.fail("Migration check failed: check ignored");
        } catch (HttpClientErrorException ex) {
            Assertions.assertEquals(HttpStatus.LOCKED, ex.getStatusCode());
            Assertions.assertEquals("Locked", ex.getStatusText());
            MbiAsserts.assertJsonEquals("{\n" +
                    "  \"status\": \"ERROR\",\n" +
                    "  \"errors\": [\n" +
                    "    {\n" +
                    "      \"code\": \"LOCKED\",\n" +
                    "      \"message\": \"Partner is in business migration: 774\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}", ex.getResponseBodyAsString());
        }
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerUnitedCatalogPartnerTest.before.csv")
    void postSupplierMappingsWithoutRequiredFields() {
        prepareMocks(emptyList(), emptyList());
        //language=json
        String request = "{ " +
                "  \"offerMappingEntries\": [ " +
                "    { " +
                "      \"offer\": {" +
                "          \"shop-sku\": \"cakes\"" +
                "        }, " +
                "      \"mapping\": { " +
                "        \"marketSku\": 123456789 " +
                "      } " +
                "    }" +
                "  ]" +
                "}";
        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/updates.{format}")
                .expand(urlBasePrefix, SUPPLIER_CAMPAIGN_ID, Format.JSON.formatName());
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request)
        );

        String expected = getFileContent("response/PostSupplierMappingsWithoutRequiredFields.expected.json");
        MbiAsserts.assertJsonEquals(expected, exception.getResponseBodyAsString());
        verifyAssortmentMarketQuickLogbrokerService();
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerFilterTest.BusinessMigration.update.error.csv")
    void postSupplierMappingsWithoutRequiredFieldsFailOnActiveBusinessMigration() {
        try {
            postMappingNullPositiveTestXml();
            Assertions.fail("Migration check failed: check ignored");
        } catch (HttpClientErrorException ex) {
            Assertions.assertEquals(HttpStatus.LOCKED, ex.getStatusCode());
            Assertions.assertEquals("Locked", ex.getStatusText());
            MbiAsserts.assertXmlEquals("<response>\n" +
                    "    <status>ERROR</status>\n" +
                    "    <errors>\n" +
                    "        <error code=\"LOCKED\" message=\"Partner is in business migration: 774\"/>\n" +
                    "    </errors>\n" +
                    "</response>", ex.getResponseBodyAsString());
        }
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerFilterTest.LockedBusiness.update.error.csv")
    void postSupplierMappingsWithoutRequiredFieldsFailOnLockedBusiness() {
        try {
            postMappingNullPositiveTestXml();
            Assertions.fail("Migration check failed: check ignored");
        } catch (HttpClientErrorException ex) {
            Assertions.assertEquals(HttpStatus.LOCKED, ex.getStatusCode());
            Assertions.assertEquals("Locked", ex.getStatusText());
            MbiAsserts.assertXmlEquals("<response>\n" +
                    "    <status>ERROR</status>\n" +
                    "    <errors>\n" +
                    "        <error code=\"LOCKED\" message=\"Partner is in business migration: 774\"/>\n" +
                    "    </errors>\n" +
                    "</response>", ex.getResponseBodyAsString());
        }
    }

    @Test
    void deleteAllMappingsXml() {
        prepareMocks(emptyList(), singletonList("1"));
        Mockito.when(patientMboMappingsService.addProductInfo(Mockito.any()))
                .thenReturn(MboMappings.ProviderProductInfoResponse.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                        .build());
        //language=xml
        String request = "<purge><all>true</all></purge>";

        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/purge.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.JSON.formatName());
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request);

        MbiAsserts.assertXmlEquals(XML_OK, response.getBody());
        verify(patientMboMappingsService).addProductInfo(MboMappings.ProviderProductInfoRequest.newBuilder()
                .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                        .setMarketSkuId(0L)
                        .setMarketModelId(0)
                        .setMarketCategoryId(0)
                        .setShopId(774)
                        .setShopSkuId("1")
                        .setMappingType(MboMappings.MappingType.PRICE_COMPARISION))
                .setRequestInfo(MboMappings.ProductUpdateRequestInfo.newBuilder()
                        .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER)
                        .build())
                .build());
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerFilterTest.BusinessMigration.deleteAllMappings.error.csv")
    void deleteAllMappingsFailOnActiveBusinessMigration() {
        try {
            deleteAllMappingsXml();
            Assertions.fail("Migration check failed: check ignored");
        } catch (HttpClientErrorException ex) {
            Assertions.assertEquals(HttpStatus.LOCKED, ex.getStatusCode());
            Assertions.assertEquals("Locked", ex.getStatusText());
            MbiAsserts.assertXmlEquals("<response>\n" +
                    "    <status>ERROR</status>\n" +
                    "    <errors>\n" +
                    "        <error code=\"LOCKED\" message=\"Partner is in business migration: 774\"/>\n" +
                    "    </errors>\n" +
                    "</response>\n", ex.getResponseBodyAsString());
        }
    }

    @Test
    void deleteSelectiveMappingsXml() {
        prepareMocks(emptyList(), singletonList("1"));
        Mockito.when(patientMboMappingsService.addProductInfo(Mockito.any()))
                .thenReturn(MboMappings.ProviderProductInfoResponse.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                        .build());
        //language=xml
        String request = "<purge><offers><offer><shop-sku>1</shop-sku></offer></offers></purge>";

        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/purge.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.JSON.formatName());
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request);

        MbiAsserts.assertXmlEquals(XML_OK, response.getBody());
        verify(patientMboMappingsService).addProductInfo(MboMappings.ProviderProductInfoRequest.newBuilder()
                .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                        .setMarketSkuId(0L)
                        .setMarketModelId(0)
                        .setMarketCategoryId(0)
                        .setShopId(774)
                        .setShopSkuId("1")
                        .setMasterDataInfo(MasterDataProto.MasterDataInfo.newBuilder()
                                .setProviderProductMasterData(MasterDataProto.ProviderProductMasterData.newBuilder()
                                        .build()).build())
                        .setMappingType(MboMappings.MappingType.PRICE_COMPARISION))
                .setRequestInfo(MboMappings.ProductUpdateRequestInfo.newBuilder()
                        .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER)
                        .build())
                .build());
    }

    @Test
    void deleteAllMappingsJson() {
        prepareMocks(emptyList(), singletonList("1"));
        Mockito.when(patientMboMappingsService.addProductInfo(Mockito.any()))
                .thenReturn(MboMappings.ProviderProductInfoResponse.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                        .build());
        //language=json
        String request = "{\"all\": true}";

        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/purge.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.JSON.formatName());
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request);

        MbiAsserts.assertJsonEquals(JSON_OK, response.getBody());
        verify(patientMboMappingsService).addProductInfo(MboMappings.ProviderProductInfoRequest.newBuilder()
                .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                        .setMarketSkuId(0L)
                        .setMarketModelId(0)
                        .setMarketCategoryId(0)
                        .setShopId(774)
                        .setShopSkuId("1")
                        .setMappingType(MboMappings.MappingType.PRICE_COMPARISION))
                .setRequestInfo(MboMappings.ProductUpdateRequestInfo.newBuilder()
                        .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER)
                        .build())
                .build());
    }

    @Test
    void deleteSelectiveMappingsJson() {
        prepareMocks(emptyList(), singletonList("1"));
        Mockito.when(patientMboMappingsService.addProductInfo(Mockito.any()))
                .thenReturn(MboMappings.ProviderProductInfoResponse.newBuilder()
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                        .build());
        //language=json
        String request = "{\"offers\": [{\"shopSku\": \"1\"}]}";

        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/purge.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.JSON.formatName());
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request);

        MbiAsserts.assertJsonEquals(JSON_OK, response.getBody());
        verify(patientMboMappingsService).addProductInfo(MboMappings.ProviderProductInfoRequest.newBuilder()
                .addProviderProductInfo(MboMappings.ProviderProductInfo.newBuilder()
                        .setMarketSkuId(0L)
                        .setMarketModelId(0)
                        .setMarketCategoryId(0)
                        .setShopId(774)
                        .setShopSkuId("1")
                        .setMasterDataInfo(MasterDataProto.MasterDataInfo.newBuilder()
                                .setProviderProductMasterData(MasterDataProto.ProviderProductMasterData.newBuilder()
                                        .build()).build())
                        .setMappingType(MboMappings.MappingType.PRICE_COMPARISION))
                .setRequestInfo(MboMappings.ProductUpdateRequestInfo.newBuilder()
                        .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.SUPPLIER)
                        .build())
                .build());
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerTest.categories.csv")
    void suggestJson() {
        prepareMocks(singletonList(1759344029L), emptyList());

        when(ultraControllerClient.enrich(UltraController.DataRequest.newBuilder()
                .addOffers(UltraController.Offer.newBuilder()
                        .setReturnMarketNames(true)
                        .setPrice(50000)
                        .setShopCategoryName("смартфоны")
                        .setOffer("Apple IPhone SE 128 GB rose gold")
                ).build()))
                .thenReturn(UltraController.DataResponse.newBuilder()
                        .addOffers(
                                UltraController.EnrichedOffer.newBuilder()
                                        .setCategoryId(91491)
                                        .setMarketSkuId(100131945878L)
                                        .setModelId(1759344029)
                                        .setMarketSkuPublishedOnBlueMarket(true)
                                        .setMarketSkuPublishedOnMarket(true)
                                        .setMarketSkuName("Смартфон Apple iPhone SE 128GB розовое золото")
                                        .setMarketModelName("Apple iPhone SE 128GB")
                                        .setMarketCategoryName("Мобильные телефоны")
                        )
                        .build());

        //language=json
        String request = "{" +
                "  \"offers\": [" +
                "    {" +
                "      \"name\": \"Apple IPhone SE 128 GB rose gold\"," +
                "      \"vendor\": \"Apple\"," +
                "      \"category\": \"смартфоны\"," +
                "      \"price\": 50000" +
                "    }" +
                "  ]" +
                "}";

        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/suggestions.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.JSON.formatName());

        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request);

        String expected = getFileContent("response/Suggest.expected.json");

        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerFilterTest.BusinessMigration.deleteAllMappings.error.csv")
    void suggestFailOnBusinessMigration() {
        try {
            suggestJson();
            Assertions.fail("Migration check failed: check ignored");
        } catch (HttpClientErrorException ex) {
            Assertions.assertEquals(HttpStatus.LOCKED, ex.getStatusCode());
            Assertions.assertEquals("Locked", ex.getStatusText());
            MbiAsserts.assertJsonEquals("{\n" +
                    "  \"status\": \"ERROR\",\n" +
                    "  \"errors\": [\n" +
                    "    {\n" +
                    "      \"code\": \"LOCKED\",\n" +
                    "      \"message\": \"Partner is in business migration: 774\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}", ex.getResponseBodyAsString());
        }
    }

    @DisplayName("Если УК возвращает ID неопубликованных категории/модели/sku, то показывать не надо")
    @Test
    void suggestNotPublishedJson() {
        prepareMocks(emptyList(), emptyList());

        when(ultraControllerClient.enrich(UltraController.DataRequest.newBuilder()
                .addOffers(UltraController.Offer.newBuilder()
                        .setReturnMarketNames(true)
                        .setPrice(50000)
                        .setShopCategoryName("смартфоны")
                        .setOffer("Apple IPhone SE 128 GB rose gold")
                ).build()))
                .thenReturn(UltraController.DataResponse.newBuilder()
                        .addOffers(
                                UltraController.EnrichedOffer.newBuilder()
                                        .setCategoryId(91491)
                                        .setMarketSkuId(100131945878L)
                                        .setModelId(1759344029)
                                        .setMarketSkuPublishedOnBlueMarket(false)
                                        .setMarketSkuPublishedOnMarket(false)
                                        .setMarketSkuName("Смартфон Apple iPhone SE 128GB розовое золото")
                                        .setMarketModelName("Apple iPhone SE 128GB")
                                        .setMarketCategoryName("Мобильные телефоны")
                        )
                        .build());

        //language=json
        String request = "{" +
                "  \"offers\": [" +
                "    {" +
                "      \"name\": \"Apple IPhone SE 128 GB rose gold\"," +
                "      \"vendor\": \"Apple\"," +
                "      \"category\": \"смартфоны\"," +
                "      \"price\": 50000" +
                "    }" +
                "  ]" +
                "}";

        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/suggestions.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.JSON.formatName());

        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.JSON, request);

        //language=json
        String expected = "{" +
                "    \"status\": \"OK\"," +
                "    \"result\": {" +
                "        \"offers\": [" +
                "            {" +
                "                \"name\": \"Apple IPhone SE 128 GB rose gold\"," +
                "                \"category\": \"смартфоны\"," +
                "                \"vendor\": \"Apple\"," +
                "                \"price\": 50000" +
                "            }" +
                "        ]" +
                "    }" +
                "}";

        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerTest.categories.csv")
    void suggestXml() {
        prepareMocks(singletonList(1759344029L), emptyList());

        when(ultraControllerClient.enrich(UltraController.DataRequest.newBuilder()
                .addOffers(UltraController.Offer.newBuilder()
                        .setReturnMarketNames(true)
                        .setPrice(50000)
                        .setShopCategoryName("смартфоны")
                        .setOffer("Apple IPhone SE 128 GB rose gold")
                ).build()))
                .thenReturn(UltraController.DataResponse.newBuilder()
                        .addOffers(
                                UltraController.EnrichedOffer.newBuilder()
                                        .setCategoryId(91491)
                                        .setMarketSkuId(100131945878L)
                                        .setModelId(1759344029)
                                        .setMarketSkuPublishedOnBlueMarket(true)
                                        .setMarketSkuPublishedOnMarket(true)
                                        .setMarketSkuName("Смартфон Apple iPhone SE 128GB розовое золото")
                                        .setMarketModelName("Apple iPhone SE 128GB")
                                        .setMarketCategoryName("Мобильные телефоны")
                        )
                        .build());

        String request = getFileContent("request/Suggest.request.xml");

        URI uri = new UriTemplate("{base}/campaigns/{campaignId}/offer-mapping-entries/suggestions.{format}")
                .expand(urlBasePrefix, CAMPAIGN_ID, Format.XML.formatName());

        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(uri, HttpMethod.POST, Format.XML, request);

        String expected = getFileContent("response/Suggest.expected.xml");

        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    private void compareAddProductInfoResponse() {
        Mockito.verify(patientMboMappingsService).addProductInfo(Mockito.argThat(addProductInfoResponse -> {
            List<MboMappings.ProviderProductInfo> productInfoList = addProductInfoResponse.getProviderProductInfoList();
            if (productInfoList.size() != 1) {
                return false;
            } else {
                MboMappings.ProviderProductInfo productInfo = productInfoList.get(0);
                return productInfo.hasMarketSkuId()
                        && productInfo.getMarketSkuId() == 123456789
                        && productInfo.hasShopSkuId()
                        && productInfo.getShopSkuId().equals("SKU.1ф")
                        && !productInfo.hasTitle()
                        && productInfo.hasShopId()
                        && productInfo.getShopId() == 774
                        && !productInfo.hasDescription();
            }
        }));
    }

    private void prepareAddProductInfoErrorMock() {
        when(patientMboMappingsService.addProductInfo(any()))
                .thenReturn(MboMappings.ProviderProductInfoResponse
                        .newBuilder()
                        .addResults(
                                MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                                        .addErrors(MboMappings.ProviderProductInfoResponse.Error
                                                .newBuilder()
                                                .setErrorKind(MboMappings.ProviderProductInfoResponse
                                                        .ErrorKind.WRONG_SHOP_SKU)
                                                .setMessage("Offer at position 1 should have " +
                                                        "sku containing ASCII " +
                                                        "graphical symbols no more than 80" +
                                                        " length. Current is SKU.1+")
                                                .build())
                                        .addErrors(MboMappings.ProviderProductInfoResponse.Error
                                                .newBuilder()
                                                .setErrorKind(MboMappings.ProviderProductInfoResponse
                                                        .ErrorKind.NO_REQUIRED_FIELDS)
                                                .setMessage("Отсутствует значение для колонки 'title'")
                                                .build())
                                        .addErrors(MboMappings.ProviderProductInfoResponse.Error
                                                .newBuilder()
                                                .setErrorKind(MboMappings.ProviderProductInfoResponse
                                                        .ErrorKind.NO_REQUIRED_FIELDS)
                                                .setMessage("Отсутствует значение для колонки 'category_name'")
                                                .build())
                                        .addErrors(MboMappings.ProviderProductInfoResponse.Error
                                                .newBuilder()
                                                .setErrorKind(MboMappings.ProviderProductInfoResponse
                                                        .ErrorKind.OK_NOT_SAVED_DUE_TO_OTHER_ERRORS)
                                                .setMessage("Offer is alright, not saved due to other errors")
                                                .build())
                                        .build())
                        .setStatus(MboMappings.ProviderProductInfoResponse.Status.ERROR)
                        .build());
    }
}
