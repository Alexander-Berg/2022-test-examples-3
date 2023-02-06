package ru.yandex.market.mbi.datacamp.saas.impl;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOfferMeta;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbi.datacamp.saas.impl.attributes.DisabledBySourceAttribute;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.PartnerStatus;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.SaasDocType;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.SupplyStatus;
import ru.yandex.market.mbi.datacamp.saas.impl.mapper.SaasDatacampMapperImpl;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferFilter;
import ru.yandex.market.mbi.datacamp.saas.impl.util.SaasConverter;
import ru.yandex.market.mbi.datacamp.saas.impl.util.SaasSearchUtil;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;
import ru.yandex.market.saas.search.SaasSearchService;

import static Market.DataCamp.DataCampOfferContent.SupplyPlan.Variation.ARCHIVE;
import static Market.DataCamp.DataCampOfferContent.SupplyPlan.Variation.WILL_SUPPLY;
import static Market.DataCamp.DataCampOfferContent.SupplyPlan.Variation.WONT_SUPPLY;
import static Market.DataCamp.DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_CHECKING;
import static Market.DataCamp.DataCampOfferStatus.OfferStatus.ResultStatus.PUBLISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.datacamp.saas.impl.attributes.PartnerStatus.PartnerStatusType.AVAILABLE;
import static ru.yandex.market.mbi.datacamp.saas.impl.attributes.PartnerStatus.PartnerStatusType.HIDDEN;

public class SaasClientRequestConversionTest {

    private static final String TEXT_PARAM_PREFIX = "text=";

    private final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
    private final SaasDatacampService saasDatacampService = new SaasDatacampService(
            new SaasSearchService("mock", 0, "", httpClientMock),
            new SaasDatacampMapperImpl(),
            new SaasConverter()
    );

    @Test
    void testAllOfferFilters() {
        String expectedResponse = "" +
                " (  ( Рюкзак && Adidas* )  | s_offer_id:\"Рюкзак Adidas*\" ) " +
                " && (i_content_cpa_status:4 | i_content_cpa_status:7)" +
                " && (i_creation_hour_ts:1000..2000)" +
                " && (i_disabled_by_10462382:10 | i_disabled_by_10462382:3)" +
                " && (i_disabled_by_10462383:3)" +
                " && (i_group_id:20112002)" +
                " && (i_integral_content_status:1 | i_integral_content_status:5)" +
                " && (i_partner_status_10462382:3)" +
                " && (i_partner_status_10462383:1)" +
                " && (i_partner_status_10462384:1)" +
                " && (i_result_status_10462383:1 | i_result_status_10462383:3)" +
                " && (i_supply_plan_10462382:2)" +
                " && (i_supply_plan_10462383:3)" +
                " && (i_supply_plan_10462384:4)" +
                " && (i_united_catalog:1)" +
                " && (i_verdict:1111 | i_verdict:2222)" +
                " && (s_doc_type:offer)" +
                " && (s_leaf_category_id:1414 | s_leaf_category_id:1515)" +
                " && (s_shop_id:10462382 | s_shop_id:10462383 | s_shop_id:10462384)" +
                " && (s_variant_id:301428000)" +
                " && (s_vendor:Ikea | s_vendor:Ашан)" +
                " ~~ (s_shop_id:123456 | s_shop_id:234567 | s_shop_id:345678)";

        String result = doRequestAndCaptureRequestText(getSaasOfferFilter());
        assertEquals(expectedResponse, result);
    }

    @Test
    @DisplayName("Строка поиска с несколькими словами")
    void testTextWithWhiteSpaces() {
        String result = SaasSearchUtil.buildSkuOrNameTextFilter("Женские тапочки*");
        assertEquals(" (  ( Женские && тапочки* )  | s_offer_id:\"Женские тапочки*\" ) ", result);
    }

    @Test
    @DisplayName("Строка поиска с одним словом")
    void testTextWithoutWhiteSpaces() {
        String result = SaasSearchUtil.buildSkuOrNameTextFilter("тапочки*");
        assertEquals(" (  ( тапочки* )  | s_offer_id:тапочки* ) ", result);
    }

    @Nonnull
    private String doRequestAndCaptureRequestText(SaasOfferFilter filter) {
        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);

        try {
            StatusLine statusLineMock = new BasicStatusLine(new HttpVersion(1, 1), 200, "OK");
            HttpResponse responseMock = new BasicHttpResponse(statusLineMock);
            String responseBody = "{\"TotalDocCount\": [0]}";
            responseMock.setEntity(new StringEntity(responseBody));

            when(httpClientMock.execute(requestCaptor.capture()))
                    .thenReturn(responseMock);
            saasDatacampService.searchBusinessOffers(filter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        HttpUriRequest request = Objects.requireNonNull(requestCaptor.getValue());
        return Arrays.stream(
                        request.getURI()
                                .toString()
                                .split("&"))
                .filter(fragment -> fragment.startsWith(TEXT_PARAM_PREFIX))
                .map(fragment -> fragment.replaceFirst(TEXT_PARAM_PREFIX, ""))
                .map(textEncoded -> URLDecoder.decode(textEncoded, StandardCharsets.UTF_8))
                .findFirst()
                .orElseThrow();
    }

    private SaasOfferFilter getSaasOfferFilter() {
        return new SaasOfferFilter.Builder()
                .setDocType(SaasDocType.OFFER)
                .setTextQuery(SaasSearchUtil.buildSkuOrNameTextFilter("Рюкзак Adidas*"))
                .addShopIds(List.of(10462382L, 10462383L, 10462384L))
                .addExcludeShopIds(List.of(123456L, 234567L, 345678L))
                .addVendors(List.of("Ikea", "Ашан"))
                .addCategoryIds(List.of(1414L, 1515L))
                .setVariantId("301428000")
                .setGroupId(20112002L)
                .addResultOfferStatuses(10462383L, List.of(PUBLISHED, NOT_PUBLISHED_CHECKING))
                .addPartnerStatuses(List.of(
                        new PartnerStatus(10462382L, HIDDEN),
                        new PartnerStatus(10462383L, AVAILABLE),
                        new PartnerStatus(10462384L, AVAILABLE)
                ))
                .addContentStatusesCPA(List.of(
                        DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_IN_WORK,
                        DataCampContentStatus.OfferContentCpaState.CONTENT_STATE_SUSPENDED
                ))
                .addResultContentStatuses(List.of(
                        DataCampContentStatus.ResultContentStatus.CardStatus.HAS_CARD_MARKET,
                        DataCampContentStatus.ResultContentStatus.CardStatus.NO_CARD_NEED_CONTENT
                ))
                .addSupplyStatuses(List.of(
                        new SupplyStatus(10462382L, WILL_SUPPLY),
                        new SupplyStatus(10462383L, WONT_SUPPLY),
                        new SupplyStatus(10462384L, ARCHIVE)
                ))
                .addDisabledBySource(List.of(
                        new DisabledBySourceAttribute(10462382L, DataCampOfferMeta.DataSource.PUSH_PARTNER_API),
                        new DisabledBySourceAttribute(10462382L, DataCampOfferMeta.DataSource.MARKET_PRICELABS),
                        new DisabledBySourceAttribute(10462383L, DataCampOfferMeta.DataSource.PUSH_PARTNER_API)
                ))
                .setBusinessId(10462389L)
                .setPrefix(10462389L)
                .setUnitedCatalog(true)
                .setPageRequest(SeekSliceRequest.firstN(1))
                .addCreationTs(1000L, 2000L)
                .addVerdicts(List.of(1111L, 2222L))
                .build();
    }
}
