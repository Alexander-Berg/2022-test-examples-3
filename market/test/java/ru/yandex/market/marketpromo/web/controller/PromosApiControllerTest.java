package ru.yandex.market.marketpromo.web.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.marketpromo.core.application.security.MBOCAuthenticationRequest;
import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.data.source.logbroker.OfferLogbrokerEvent;
import ru.yandex.market.marketpromo.filter.PromoFilter;
import ru.yandex.market.marketpromo.filter.PromoSort;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferParticipation;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoParticipation;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.PromoStatus;
import ru.yandex.market.marketpromo.model.PromoWarningCode;
import ru.yandex.market.marketpromo.model.SupplierType;
import ru.yandex.market.marketpromo.model.processing.ProcessingRequestType;
import ru.yandex.market.marketpromo.model.processing.PublishingInfo;
import ru.yandex.market.marketpromo.model.processing.PublishingStatus;
import ru.yandex.market.marketpromo.processing.ProcessId;
import ru.yandex.market.marketpromo.security.SecurityRoles;
import ru.yandex.market.marketpromo.service.AssortmentService;
import ru.yandex.market.marketpromo.test.MockedWebTestBase;
import ru.yandex.market.marketpromo.utils.IdentityUtils;
import ru.yandex.market.marketpromo.web.model.response.PromoDefinitionResponse;
import ru.yandex.market.marketpromo.web.model.response.PromoOperationsResponse;
import ru.yandex.market.marketpromo.web.model.response.PromosPagingResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.basePrice;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.categoryId;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.datacampOffer;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shop;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.warehouse;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.DD_PROMO_KEY;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;
import static ru.yandex.market.marketpromo.test.client.AuthForRequests.addAuthHeaders;

public class PromosApiControllerTest extends MockedWebTestBase {

    private static final String PROMO_ID = "cheapest-as-gift$83972ad2-da80-11ea-87d0-0242ac130003";
    private static final String PROMO_ID_DD = "direct-discount$79303ddf-7031-40c5-aad9-d5a8bf1df16f";
    private static final int WAREHOUSE_ID = 123;
    private static final long SHOP_ID = 12L;
    private static final String SSKU_1 = "ssku-1";

    @Autowired
    private AssortmentService assortmentService;
    @Autowired
    private Queue<OfferLogbrokerEvent> mockedLogbrokerQueue;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private DatacampOfferDao datacampOfferDao;

    private Promo directDiscount;

    @BeforeEach
    void setUp() {
        mockedLogbrokerQueue.clear();

        directDiscount = promoDao.replace(promo(
                id(DD_PROMO_KEY.getId()),
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));

        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        categoryId(123L),
                        potentialPromo(directDiscount.getId(), BigDecimal.valueOf(150))
                )
        ));
    }

    @AfterEach
    void clean() {
        mockedLogbrokerQueue.clear();
    }

    @Test
    void shouldRespondOnStubbedGetMethod() throws Exception {
        PromoDefinitionResponse promoResponse = objectMapper.readValue(
                mockMvc.perform(get("/v1/promos/" + PROMO_ID)
                        .cookie(new Cookie("stub", "1"))
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn().getResponse().getContentAsString(), PromoDefinitionResponse.class);

        assertThat(promoResponse, notNullValue());
        assertThat(promoResponse.getPromo(), notNullValue());
        assertThat(promoResponse.getPromo(), allOf(
                hasProperty("id"),
                hasProperty("promoId"),
                hasProperty("name"),
                hasProperty("description"),
                hasProperty("createdAt"),
                hasProperty("updatedAt"),
                hasProperty("deadlineAt"),
                hasProperty("period", allOf(
                        hasProperty("from"),
                        hasProperty("to")
                )),
                hasProperty("mechanicsType", is(MechanicsType.CHEAPEST_AS_GIFT)),
                hasProperty("supplierTypes", hasItems(SupplierType._1P, SupplierType._3P)),
                hasProperty("warnings",
                        hasItems(PromoWarningCode.PARTICIPATION_WARNINGS, PromoWarningCode.CHANGES_NOT_PUBLISHED)),
                hasProperty("categoryIds", not(empty())),
                hasProperty("status", is(PromoStatus.APPROVING_PROMO)),
                hasProperty("mechanicsProperties", allOf(
                        hasProperty("quantityInBundle", comparesEqualTo(3)),
                        hasProperty("warehouseId", comparesEqualTo(123L))
                ))

        ));
    }

    @Test
    void shouldRespondOnStubbedGetMethodDirectDiscount() throws Exception {
        PromoDefinitionResponse promoResponse = objectMapper.readValue(mockMvc.perform(get("/v1/promos/" + PROMO_ID_DD)
                .cookie(new Cookie("stub", "1"))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), PromoDefinitionResponse.class);

        assertThat(promoResponse, notNullValue());
        assertThat(promoResponse.getPromo(), notNullValue());
        assertThat(promoResponse.getPromo(), allOf(
                hasProperty("id"),
                hasProperty("promoId"),
                hasProperty("name"),
                hasProperty("description"),
                hasProperty("createdAt"),
                hasProperty("updatedAt"),
                hasProperty("deadlineAt"),
                hasProperty("period", allOf(
                        hasProperty("from"),
                        hasProperty("to")
                )),
                hasProperty("mechanicsType", is(MechanicsType.DIRECT_DISCOUNT)),
                hasProperty("supplierTypes", hasItems(SupplierType._1P, SupplierType._3P)),
                hasProperty("categoryIds", not(empty())),
                hasProperty("status", is(PromoStatus.APPROVING_PROMO)),
                hasProperty("mechanicsProperties", allOf(
                        hasProperty("minimalDiscountPercentSize", comparesEqualTo(BigDecimal.valueOf(30.0))),
                        hasProperty("categoriesWithDiscounts", hasSize(2))
                ))

        ));
    }

    @Test
    void shouldRespondOnStubbedGetListMethod() throws Exception {
        PromosPagingResponse pagingResponse = objectMapper.readValue(mockMvc.perform(get("/v1/promos")
                .cookie(new Cookie("stub", "1"))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), PromosPagingResponse.class);

        assertThat(pagingResponse, notNullValue());
        assertThat(pagingResponse.getPromos(), notNullValue());
        assertThat(pagingResponse.getPromos(), hasItem(allOf(
                hasProperty("id"),
                hasProperty("promoId"),
                hasProperty("name"),
                hasProperty("description"),
                hasProperty("createdAt"),
                hasProperty("updatedAt"),
                hasProperty("deadlineAt"),
                hasProperty("period", allOf(
                        hasProperty("from"),
                        hasProperty("to")
                )),
                hasProperty("mechanicsType", is(MechanicsType.CHEAPEST_AS_GIFT)),
                hasProperty("supplierTypes", hasItems(SupplierType._1P, SupplierType._3P)),
                hasProperty("categoryIds", not(empty())),
                hasProperty("status", is(PromoStatus.APPROVING_PROMO)),
                hasProperty("mechanicsProperties", allOf(
                        hasProperty("quantityInBundle", comparesEqualTo(3)),
                        hasProperty("warehouseId", comparesEqualTo(123L))
                ))
        )));
        assertThat(pagingResponse.getPages(), notNullValue());
        assertThat(pagingResponse.getPages().getCurrent(), comparesEqualTo(1));
        assertThat(pagingResponse.getPages().getTotal(), comparesEqualTo(1));
        assertThat(pagingResponse.getPages().getTotalItems(), comparesEqualTo(1L));

    }

    @Test
    void shouldRespondOnNotFoundPromos() throws Exception {
        PromosPagingResponse pagingResponse = objectMapper.readValue(
                mockMvc.perform(get("/v1/promos")
                        .params(filterParams())
                        .params(pagingParams())
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn().getResponse().getContentAsString(), PromosPagingResponse.class);

        assertTrue(pagingResponse.getPromos().isEmpty());
        assertThat(pagingResponse.getAppliedSort(), empty());
        assertThat(pagingResponse.getAppliedFilters(), is(List.of(PromoFilter.NAME)));
    }

    @Test
    void shouldHandleEmptyFiltersSearch() throws Exception {
        PromosPagingResponse pagingResponse = objectMapper.readValue(
                mockMvc.perform(get("/v1/promos")
                        .params(pagingParams())
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn().getResponse().getContentAsString(), PromosPagingResponse.class);

        assertThat(pagingResponse.getAppliedSort(), empty());
        assertThat(pagingResponse.getAppliedFilters(), empty());
    }

    @Test
    void shouldRespondWithEmptySort() throws Exception {
        PromosPagingResponse emptySortResponse = objectMapper.readValue(
                mockMvc.perform(get("/v1/promos")
                        .param("sortBy", "")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn().getResponse().getContentAsString(), PromosPagingResponse.class);

        assertThat(emptySortResponse.getAppliedSort(), empty());
        assertThat(emptySortResponse.getAppliedFilters(), empty());
    }

    @Test
    void shouldRespondWithAppliedSort() throws Exception {
        PromosPagingResponse sortedResponse = objectMapper.readValue(
                mockMvc.perform(get("/v1/promos")
                        .param("sortBy", "MECHANICS_TYPE", "!NAME")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn().getResponse().getContentAsString(), PromosPagingResponse.class);

        assertThat(sortedResponse.getAppliedSort(), containsInRelativeOrder(
                allOf(
                        hasProperty("sortBy", is(PromoSort.MECHANICS_TYPE)),
                        hasProperty("ascending", is(true))),
                allOf(
                        hasProperty("sortBy", is(PromoSort.NAME)),
                        hasProperty("ascending", is(false)))));
        assertThat(sortedResponse.getAppliedFilters(), empty());
    }

    @Test
    void shouldReturnPublishingProcessDataOnPromoGet() throws Exception {
        assortmentService.markDirectDiscountToParticipate(DD_PROMO_KEY, List.of(
                DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(OfferPromoParticipation.builder()
                                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .participate(true)
                                .promoId(directDiscount.getId())
                                .build())
                        .fixedBasePrice(BigDecimal.valueOf(100))
                        .fixedPrice(BigDecimal.TEN)
                        .build()
        ));
        PublishingInfo publishingInfo = assortmentService.publishPromoAssortment(DD_PROMO_KEY);

        assertThat(publishingInfo.getPublishingStatus(), is(PublishingStatus.PUBLISHED));

        PromoDefinitionResponse promoResponse = objectMapper.readValue(
                mockMvc.perform(get("/v1/promos/{promoId}", IdentityUtils.encodePromoId(directDiscount))
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn().getResponse().getContentAsString(), PromoDefinitionResponse.class);

        assertThat(promoResponse.getProcesses(), notNullValue());
        assertThat(promoResponse.getProcesses().getPublishing(), notNullValue());
        assertThat(promoResponse.getProcesses().getPublishing().getToken(),
                is(IdentityUtils.encodeProcessId(
                        ProcessId.of(ProcessingRequestType.PUBLISH_ASSORTMENT, directDiscount.getId()))
                ));
    }

    @Test
    void shouldFailByAccessWhenOnTogglePromoIsSystemWithoutDevRole() throws Exception {
        final String id = IdentityUtils.encodePromoId(directDiscount);
        final String url = "/v1/promos/{id}/toggleIsSystem";
        final ResultActions res = mockMvc.perform(post(url, id).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        res.andExpect(status().isForbidden());
    }

    @Test
    void shouldFailByAccessWhenOnTryToTogglePromoIsSystemSystemByViewerRole() throws Exception {
        final ResultActions res = performMakePromoSystemWithRole(SecurityRoles.VIEWER);
        res.andExpect(status().isForbidden());
    }

    @Test
    void shouldFailByAccessWhenOnTryToTogglePromoIsSystemByAssortmentRole() throws Exception {
        final ResultActions res = performMakePromoSystemWithRole(SecurityRoles.MANAGE_PROMO_ASSORTMENT);
        res.andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnProcessedResultOnTogglePromoIsSystem() throws Exception {
        final String promoId = IdentityUtils.encodePromoId(directDiscount);
        final String url = "/v1/promos/{promoId}/toggleIsSystem";

        assertThat(directDiscount.getSystem(), equalTo(false));

        final MockHttpServletRequestBuilder rb =
                addAuthHeaders(
                        post(url, promoId).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE),
                        MBOCAuthenticationRequest.builder().roles(Set.of(SecurityRoles.DEVELOPER)).build()
                );
        PromoOperationsResponse res = objectMapper.readValue(
                mockMvc.perform(rb)
                        .andExpect(status().is2xxSuccessful())
                        .andReturn().getResponse().getContentAsString(), PromoOperationsResponse.class);

        assertThat(res.getIsSystem(), equalTo(true));
    }

    @Test
    void shouldReturnNotFoundResultOnToggleMissingPromoIsSystem() throws Exception {
        final String id = IdentityUtils.encodePromoId(MechanicsType.DIRECT_DISCOUNT, "wrong direct discount promo");
        final String url = "/v1/promos/{id}/toggleIsSystem";
        final MockHttpServletRequestBuilder requestBuilder =
                addAuthHeaders(
                        post(url, id).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE),
                        MBOCAuthenticationRequest.builder().roles(Set.of(SecurityRoles.DEVELOPER)).build()
                );

        final String content = mockMvc.perform(requestBuilder)
                .andExpect(status().is(404))
                .andReturn().getResponse().getContentAsString();

        assertThat(content, containsString("Promo '" + id + "' not found."));
    }

    private MultiValueMap<String, String> filterParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Скидосик");
        return params;
    }

    private MultiValueMap<String, String> pagingParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("offset", "0");
        params.add("count", "500");
        return params;
    }

    private ResultActions performMakePromoSystemWithRole(String role) throws Exception {
        final String id = IdentityUtils.encodePromoId(directDiscount);
        final String url = "/v1/promos/{id}/toggleIsSystem";
        final MockHttpServletRequestBuilder rb = post(url, id).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        return mockMvc.perform(addAuthHeaders(rb, MBOCAuthenticationRequest.builder().roles(Set.of(role)).build()));
    }
}
