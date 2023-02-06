package ru.yandex.market.marketpromo.test.client;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.security.SecurityRoles;
import ru.yandex.market.marketpromo.core.config.ApplicationCoreInternalConfig;
import ru.yandex.market.marketpromo.filter.AssortmentFilter;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.filter.FilterValue;
import ru.yandex.market.marketpromo.filter.SortProperty;
import ru.yandex.market.marketpromo.utils.IdentityUtils;
import ru.yandex.market.marketpromo.web.controller.AssortmentApiController;
import ru.yandex.market.marketpromo.web.model.request.MarkToParticipateRequest;
import ru.yandex.market.marketpromo.web.model.response.ImportResponse;
import ru.yandex.market.marketpromo.web.model.response.OfferItemsCountResponse;
import ru.yandex.market.marketpromo.web.model.response.OfferItemsPagingResponse;
import ru.yandex.market.marketpromo.web.model.response.OfferItemsResponse;
import ru.yandex.market.marketpromo.web.model.response.PublishingResponse;
import ru.yandex.market.marketpromo.core.application.security.MBOCAuthenticationRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.marketpromo.test.client.AuthForRequests.addAuthHeaders;

public final class AssortmentRequests {

    private static final String API_ASSORTMENT = "/v1/promos/{promoId}/assortment";
    private static final String API_ASSORTMENT_COUNT = "/v1/promos/{promoId}/assortment/total";
    private static final String API_ASSORTMENT_PUBLISH = "/v1/promos/{promoId}/assortment/publish";
    private static final String API_ASSORTMENT_PUBLISHING_STATE = "/v1/promos/{promoId}/assortment/publish/{token}";
    private static final String API_ASSORTMENT_RESET = "/v1/promos/{promoId}/assortment/reset";
    private static final String API_ASSORTMENT_IMPORT = "/v1/promos/{promoId}/assortment.{type}";

    private static final ApplicationCoreInternalConfig.SerializationConfig SERIALIZATION_CONFIG =
            new ApplicationCoreInternalConfig.SerializationConfig();

    private AssortmentRequests() {
    }

    @Nonnull
    public static OfferItemsPagingResponse getAssortment(@Nonnull MockMvc mockMvc,
                                                         @Nonnull AssortmentRequest assortmentRequest)
            throws Exception {
        return getAssortment(mockMvc, assortmentRequest, MBOCAuthenticationRequest.builder()
                .roles(Set.of(SecurityRoles.VIEWER))
                .build());
    }

    @Nonnull
    public static OfferItemsPagingResponse getAssortment(@Nonnull MockMvc mockMvc,
                                                         @Nonnull AssortmentRequest assortmentRequest,
                                                         @Nonnull MBOCAuthenticationRequest authenticationRequest) throws Exception {
        return SERIALIZATION_CONFIG.objectMapper().readValue(
                getAssortmentAction(mockMvc, assortmentRequest, authenticationRequest)
                        .andExpect(status().is2xxSuccessful())
                        .andReturn().getResponse().getContentAsString(),
                OfferItemsPagingResponse.class);
    }

    @Nonnull
    public static ResultActions getAssortmentAction(@Nonnull MockMvc mockMvc,
                                                    @Nonnull AssortmentRequest assortmentRequest,
                                                    @Nonnull MBOCAuthenticationRequest authenticationRequest)
            throws Exception {
        var rb = get(API_ASSORTMENT, IdentityUtils.encodePromoId(
                assortmentRequest.getPromoKey().getMechanicsType(), assortmentRequest.getPromoKey().getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        appendFilters(rb, assortmentRequest);

        return mockMvc.perform(addAuthHeaders(rb, authenticationRequest));
    }

    @Nonnull
    public static OfferItemsCountResponse getAssortmentCount(@Nonnull MockMvc mockMvc,
                                                             @Nonnull AssortmentRequest assortmentRequest)
            throws Exception {
        return getAssortmentCount(mockMvc, assortmentRequest, MBOCAuthenticationRequest.builder()
                .roles(Set.of(SecurityRoles.VIEWER))
                .build());
    }

    @Nonnull
    public static OfferItemsCountResponse getAssortmentCount(@Nonnull MockMvc mockMvc,
                                                             @Nonnull AssortmentRequest assortmentRequest,
                                                             @Nonnull MBOCAuthenticationRequest authenticationRequest) throws Exception {
        return SERIALIZATION_CONFIG.objectMapper().readValue(
                getAssortmentCountAction(mockMvc, assortmentRequest, authenticationRequest)
                        .andExpect(status().is2xxSuccessful())
                        .andReturn().getResponse().getContentAsString(),
                OfferItemsCountResponse.class);
    }

    @Nonnull
    public static ResultActions getAssortmentCountAction(@Nonnull MockMvc mockMvc,
                                                         @Nonnull AssortmentRequest assortmentRequest,
                                                         @Nonnull MBOCAuthenticationRequest authenticationRequest)
            throws Exception {
        var rb = get(API_ASSORTMENT_COUNT, IdentityUtils.encodePromoId(
                assortmentRequest.getPromoKey().getMechanicsType(), assortmentRequest.getPromoKey().getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        appendFilters(rb, assortmentRequest);

        return mockMvc.perform(addAuthHeaders(rb, authenticationRequest));
    }

    @Nonnull
    public static OfferItemsResponse markAssortment(@Nonnull MockMvc mockMvc,
                                                    @Nonnull Promo promo,
                                                    @Nonnull MarkToParticipateRequest markToParticipateRequest)
            throws Exception {
        return markAssortment(mockMvc, promo, markToParticipateRequest, MBOCAuthenticationRequest.builder()
                .roles(Set.of(SecurityRoles.MANAGE_PROMO_ASSORTMENT))
                .build());
    }

    @Nonnull
    public static OfferItemsResponse markAssortment(@Nonnull MockMvc mockMvc,
                                                    @Nonnull Promo promo,
                                                    @Nonnull MarkToParticipateRequest markToParticipateRequest,
                                                    @Nonnull MBOCAuthenticationRequest authenticationRequest)
            throws Exception {
        return SERIALIZATION_CONFIG.objectMapper().readValue(markAssortmentAction(mockMvc, promo,
                markToParticipateRequest, authenticationRequest)
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), OfferItemsResponse.class);
    }

    @Nonnull
    public static ResultActions markAssortmentAction(@Nonnull MockMvc mockMvc,
                                                     @Nonnull Promo promo,
                                                     @Nonnull MarkToParticipateRequest markToParticipateRequest)
            throws Exception {
        return markAssortmentAction(mockMvc, promo, markToParticipateRequest, MBOCAuthenticationRequest.builder()
                .roles(Set.of(SecurityRoles.MANAGE_PROMO_ASSORTMENT))
                .build());
    }

    @Nonnull
    public static ResultActions markAssortmentAction(@Nonnull MockMvc mockMvc,
                                                     @Nonnull Promo promo,
                                                     @Nonnull MarkToParticipateRequest markToParticipateRequest,
                                                     @Nonnull MBOCAuthenticationRequest authenticationRequest)
            throws Exception {
        return mockMvc.perform(addAuthHeaders(patch(API_ASSORTMENT, IdentityUtils.encodePromoId(promo))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(SERIALIZATION_CONFIG.objectMapper()
                        .writeValueAsBytes(markToParticipateRequest)), authenticationRequest));
    }

    @Nonnull
    public static PublishingResponse publishAssortment(@Nonnull MockMvc mockMvc,
                                                       @Nonnull Promo promo) throws Exception {
        return SERIALIZATION_CONFIG.objectMapper().readValue(publishAssortmentAction(mockMvc, promo)
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), PublishingResponse.class);
    }

    @Nonnull
    public static PublishingResponse publishAssortment(@Nonnull MockMvc mockMvc,
                                                       @Nonnull Promo promo,
                                                       @Nonnull MBOCAuthenticationRequest authenticationRequest)
            throws Exception {
        return SERIALIZATION_CONFIG.objectMapper().readValue(publishAssortmentAction(mockMvc, promo,
                authenticationRequest)
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), PublishingResponse.class);
    }


    @Nonnull
    public static ResultActions publishAssortmentAction(@Nonnull MockMvc mockMvc,
                                                        @Nonnull Promo promo) throws Exception {
        return publishAssortmentAction(mockMvc, promo, MBOCAuthenticationRequest.builder()
                .roles(Set.of(SecurityRoles.MANAGE_PROMO_ASSORTMENT))
                .build());
    }

    @Nonnull
    public static ResultActions publishAssortmentAction(@Nonnull MockMvc mockMvc,
                                                        @Nonnull Promo promo,
                                                        @Nonnull MBOCAuthenticationRequest authenticationRequest)
            throws Exception {
        return mockMvc.perform(addAuthHeaders(
                patch(API_ASSORTMENT_PUBLISH, IdentityUtils.encodePromoId(promo)), authenticationRequest));
    }

    @Nonnull
    public static PublishingResponse assortmentPublishingState(@Nonnull MockMvc mockMvc,
                                                               @Nonnull Promo promo,
                                                               @Nonnull String token) throws Exception {
        return assortmentPublishingState(mockMvc, promo, token, MBOCAuthenticationRequest.builder()
                .roles(Set.of(SecurityRoles.VIEWER))
                .build());
    }

    @Nonnull
    public static PublishingResponse assortmentPublishingState(@Nonnull MockMvc mockMvc,
                                                               @Nonnull Promo promo,
                                                               @Nonnull String token,
                                                               @Nonnull MBOCAuthenticationRequest authenticationRequest)
            throws Exception {
        return SERIALIZATION_CONFIG.objectMapper().readValue(mockMvc.perform(
                addAuthHeaders(get(API_ASSORTMENT_PUBLISHING_STATE,
                        IdentityUtils.encodePromoId(promo), token), authenticationRequest)
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), PublishingResponse.class);
    }

    public static ImportResponse importXlsx(@Nonnull MockMvc mockMvc,
                                  @Nonnull Promo promo,
                                  @Nonnull MockMultipartFile importFile) throws Exception {
        return importXlsx(mockMvc, promo, importFile, MBOCAuthenticationRequest.builder()
                .roles(Set.of(SecurityRoles.MANAGE_PROMO_ASSORTMENT))
                .build());
    }

    public static ImportResponse importXlsx(@Nonnull MockMvc mockMvc,
                                            @Nonnull Promo promo,
                                            @Nonnull MockMultipartFile importFile,
                                            @Nonnull MBOCAuthenticationRequest authenticationRequest) throws Exception {
        return SERIALIZATION_CONFIG.objectMapper().readValue(mockMvc.perform(addAuthHeaders(multipart(API_ASSORTMENT_IMPORT,
                IdentityUtils.encodePromoId(promo),
                AssortmentApiController.ALLOWABLE_IMPORT_TYPES.get(0))
                .file(importFile), authenticationRequest))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), ImportResponse.class);

    }

    public static void resetAssortment(@Nonnull MockMvc mockMvc,
                                       @Nonnull Promo promo) throws Exception {
        resetAssortment(mockMvc, promo, MBOCAuthenticationRequest.builder()
                .roles(Set.of(SecurityRoles.MANAGE_PROMO_ASSORTMENT))
                .build());
    }

    public static void resetAssortment(@Nonnull MockMvc mockMvc,
                                       @Nonnull Promo promo,
                                       @Nonnull MBOCAuthenticationRequest authenticationRequest) throws Exception {
        resetAssortmentAction(mockMvc, promo, authenticationRequest);
    }

    @Nonnull
    public static ResultActions resetAssortmentAction(@Nonnull MockMvc mockMvc,
                                                      @Nonnull Promo promo) throws Exception {
        return resetAssortmentAction(mockMvc, promo, MBOCAuthenticationRequest.builder()
                .roles(Set.of(SecurityRoles.MANAGE_PROMO_ASSORTMENT))
                .build());
    }

    @Nonnull
    public static ResultActions resetAssortmentAction(@Nonnull MockMvc mockMvc,
                                                      @Nonnull Promo promo,
                                                      @Nonnull MBOCAuthenticationRequest authenticationRequest)
            throws Exception {
        return mockMvc.perform(
                addAuthHeaders(delete(API_ASSORTMENT_RESET, IdentityUtils.encodePromoId(promo))
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE), authenticationRequest)
        );
    }

    @Nonnull
    private static String mapToString(@Nonnull FilterValue<AssortmentFilter, ?> filterValue) {
        if (filterValue.isCollection()) {
            Collection<?> collection = (Collection<?>) filterValue.getValue();
            return collection.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        } else {
            return String.valueOf(filterValue.getValue());
        }
    }

    @Nonnull
    private static MockHttpServletRequestBuilder appendFilters(
            @Nonnull MockHttpServletRequestBuilder requestBuilder,
            @Nonnull AssortmentRequest assortmentRequest
    ) {
        for (FilterValue<AssortmentFilter, ?> filterValue : assortmentRequest.getFilterValues()) {
            switch (filterValue.getFilter()) {
                case PROMO_ID:
                    break;
                case NAME:
                    requestBuilder.param("name", mapToString(filterValue));
                    break;
                case CATEGORY_ID:
                    requestBuilder.param("categories", mapToString(filterValue));
                    break;
                case SUPPLIER_TYPE:
                    requestBuilder.param("supplierTypes", mapToString(filterValue));
                    break;
                case SUPPLIER_ID:
                    requestBuilder.param("supplierIds", mapToString(filterValue));
                    break;
                case MSKU:
                    requestBuilder.param("mskuIds", mapToString(filterValue));
                    break;
                case SSKU:
                    requestBuilder.param("sskuIds", mapToString(filterValue));
                    break;
                case DISOUNT_FROM:
                    requestBuilder.param("discountFrom", mapToString(filterValue));
                    break;
                case DISOUNT_TO:
                    requestBuilder.param("discountTo", mapToString(filterValue));
                    break;
                case PARTICIPATE:
                    requestBuilder.param("participates", mapToString(filterValue));
                    break;
                case HAS_ERRORS:
                    requestBuilder.param("hasErrors", mapToString(filterValue));
                    break;
                case HIDE_DISABLED:
                    requestBuilder.param("hideDisabled", mapToString(filterValue));
                    break;
                case HIDE_OTHER_ACTIVE_PROMOS:
                    requestBuilder.param("hideOtherActivePromos", mapToString(filterValue));
                    break;
                case HIDE_EMPTY_STOCKS:
                    requestBuilder.param("hideEmptyStocks", mapToString(filterValue));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
        if (!assortmentRequest.getSorters().isEmpty()) {
            requestBuilder.param("sortBy", assortmentRequest.getSorters().stream()
                    .map(SortProperty::toString)
                    .collect(Collectors.joining(",")));
        }
        if (assortmentRequest.getLimit() > 0) {
            requestBuilder.param("limit", String.valueOf(assortmentRequest.getLimit()));
            if (assortmentRequest.getOffset() > 0) {
                requestBuilder.param("offset", String.valueOf(assortmentRequest.getOffset()));
            }
        }
        return requestBuilder;
    }
}
