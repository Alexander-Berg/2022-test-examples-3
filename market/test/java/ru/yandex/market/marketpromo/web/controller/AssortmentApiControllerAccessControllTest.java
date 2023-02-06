package ru.yandex.market.marketpromo.web.controller;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.service.impl.CachedAssortmentService;
import ru.yandex.market.marketpromo.filter.AssortmentFilter;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.OfferDisabledSource;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.PublishActionState;
import ru.yandex.market.marketpromo.model.SupplierType;
import ru.yandex.market.marketpromo.core.application.security.MBOCAuthenticationRequest;
import ru.yandex.market.marketpromo.security.SecurityRoles;
import ru.yandex.market.marketpromo.test.MockedWebTestBase;
import ru.yandex.market.marketpromo.test.client.AssortmentRequests;
import ru.yandex.market.marketpromo.utils.IdentityUtils;
import ru.yandex.market.marketpromo.web.model.DirectDiscountMarkToParticipateItem;
import ru.yandex.market.marketpromo.web.model.DirectDiscountOfferProperties;
import ru.yandex.market.marketpromo.web.model.OfferItem;
import ru.yandex.market.marketpromo.web.model.request.MarkToParticipateRequest;
import ru.yandex.market.marketpromo.web.model.response.ImportResponse;
import ru.yandex.market.marketpromo.web.model.response.OfferItemsPagingResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.basePrice;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.categoryId;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.datacampOffer;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.disabled;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.disabledSource;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shop;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.stocks;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.supplierType;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.wareMd5;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.warehouse;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.DD_PROMO_KEY;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promoId;

public class AssortmentApiControllerAccessControllTest extends MockedWebTestBase {

    private static final long WAREHOUSE_ID = 123L;
    private static final long SHOP_ID = 12L;
    private static final String DD_PROMO_ID = "#21098";
    private static final String WARE_1 = "ware-1";
    private static final String WARE_2 = "ware-2";
    private static final String SSKU_1 = "ssku-1";
    private static final String SSKU_2 = "ssku-2";

    @Autowired
    private PromoDao promoDao;
    @Autowired
    private DatacampOfferDao datacampOfferDao;
    @Value("classpath:excel/assortment.xlsx")
    private Resource assortmentImportResource;
    @Autowired
    private CachedAssortmentService cachedAssortmentService;

    private Promo directDiscount;

    @BeforeEach
    void configure() {
        directDiscount = promoDao.replace(promo(
                id(DD_PROMO_KEY.getId()),
                promoId(DD_PROMO_ID),
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));

        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        shop(SHOP_ID),
                        wareMd5(WARE_1),
                        warehouse(WAREHOUSE_ID),
                        supplierType(SupplierType._1P),
                        price(1000),
                        basePrice(1500),
                        disabledSource(OfferDisabledSource.MARKET_ABO),
                        disabledSource(OfferDisabledSource.MARKET_IDX),
                        disabled(true),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(DD_PROMO_KEY.getId(), BigDecimal.valueOf(150))
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        shop(SHOP_ID),
                        wareMd5(WARE_2),
                        warehouse(WAREHOUSE_ID),
                        supplierType(SupplierType._3P),
                        price(1000),
                        basePrice(1500),
                        categoryId(123L),
                        stocks(15L),
                        potentialPromo(DD_PROMO_KEY.getId(), BigDecimal.valueOf(150))
                )
        ));
    }

    @Test
    void shouldPermitMarkingIfPromoHas1PSupplierType() throws Exception {
        AssortmentRequests.markAssortmentAction(mockMvc, directDiscount,
                MarkToParticipateRequest.builder()
                        .item(DirectDiscountMarkToParticipateItem.builder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                                .participates(true)
                                .mechanicsProperties(DirectDiscountOfferProperties.builder()
                                        .fixedBasePrice(BigDecimal.TEN)
                                        .fixedPrice(BigDecimal.ONE)
                                        .minimalDiscountPercentSize(BigDecimal.TEN)
                                        .build())
                                .build())
                        .build()).andExpect(status().isOk());
    }

    @Test
    void shouldRejectMarkingIfPromoHas3PSupplierType() throws Exception {
        AssortmentRequests.markAssortmentAction(mockMvc, directDiscount,
                MarkToParticipateRequest.builder()
                        .item(DirectDiscountMarkToParticipateItem.builder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_2), SHOP_ID))
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                                .participates(true)
                                .mechanicsProperties(DirectDiscountOfferProperties.builder()
                                        .fixedBasePrice(BigDecimal.TEN)
                                        .fixedPrice(BigDecimal.ONE)
                                        .minimalDiscountPercentSize(BigDecimal.TEN)
                                        .build())
                                .build())
                        .build()).andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAssortmentGetIfNoRolePresented() throws Exception {
        AssortmentRequests.getAssortmentAction(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey()).build(),
                MBOCAuthenticationRequest.builder()
                        .build()).andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectMarkingOnReadOnlyAccess() throws Exception {
        AssortmentRequests.markAssortmentAction(mockMvc, directDiscount,
                MarkToParticipateRequest.builder()
                        .item(DirectDiscountMarkToParticipateItem.builder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_2), SHOP_ID))
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                                .participates(true)
                                .mechanicsProperties(DirectDiscountOfferProperties.builder()
                                        .fixedBasePrice(BigDecimal.TEN)
                                        .fixedPrice(BigDecimal.ONE)
                                        .minimalDiscountPercentSize(BigDecimal.TEN)
                                        .build())
                                .build())
                        .build(), MBOCAuthenticationRequest.builder()
                        .roles(Set.of(SecurityRoles.VIEWER))
                        .build()).andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnSavingActionStateIfHasEditAccess() throws Exception {
        AssortmentRequests.markAssortmentAction(mockMvc, directDiscount,
                MarkToParticipateRequest.builder()
                        .item(DirectDiscountMarkToParticipateItem.builder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                                .participates(true)
                                .mechanicsProperties(DirectDiscountOfferProperties.builder()
                                        .fixedBasePrice(BigDecimal.TEN)
                                        .fixedPrice(BigDecimal.ONE)
                                        .minimalDiscountPercentSize(BigDecimal.TEN)
                                        .build())
                                .build())
                        .build()).andExpect(status().isOk());

        OfferItemsPagingResponse response = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey()).build(),
                MBOCAuthenticationRequest.builder()
                        .roles(Set.of(SecurityRoles.VIEWER, SecurityRoles.MANAGE_PROMO_ASSORTMENT))
                        .build());

        assertThat(response.getMeta().isEnableSaveAction(), is(true));
    }

    @Test
    void shouldReturnPublishingActionStateIfHasEditAccess() throws Exception {
        AssortmentRequests.markAssortmentAction(mockMvc, directDiscount,
                MarkToParticipateRequest.builder()
                        .item(DirectDiscountMarkToParticipateItem.builder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                                .participates(true)
                                .mechanicsProperties(DirectDiscountOfferProperties.builder()
                                        .fixedBasePrice(BigDecimal.TEN)
                                        .fixedPrice(BigDecimal.ONE)
                                        .minimalDiscountPercentSize(BigDecimal.TEN)
                                        .build())
                                .build())
                        .build()).andExpect(status().isOk());

        OfferItemsPagingResponse response = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey()).build(),
                MBOCAuthenticationRequest.builder()
                        .roles(Set.of(SecurityRoles.VIEWER, SecurityRoles.MANAGE_PROMO_ASSORTMENT))
                        .build());

        assertThat(response.getMeta().getPublishStatus(), is(PublishActionState.ENABLED));
    }

    @Test
    void shouldNotReturnSavingActionStateIfNotEditAccess() throws Exception {
        AssortmentRequests.markAssortmentAction(mockMvc, directDiscount,
                MarkToParticipateRequest.builder()
                        .item(DirectDiscountMarkToParticipateItem.builder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                                .participates(true)
                                .mechanicsProperties(DirectDiscountOfferProperties.builder()
                                        .fixedBasePrice(BigDecimal.TEN)
                                        .fixedPrice(BigDecimal.ONE)
                                        .minimalDiscountPercentSize(BigDecimal.TEN)
                                        .build())
                                .build())
                        .build()).andExpect(status().isOk());

        OfferItemsPagingResponse response = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey()).build(),
                MBOCAuthenticationRequest.builder()
                        .roles(Set.of(SecurityRoles.VIEWER))
                        .build());

        assertThat(response.getMeta().isEnableSaveAction(), is(false));
    }

    @Test
    void shouldNotReturnPublishingActionStateIfNotEditAccess() throws Exception {
        AssortmentRequests.markAssortmentAction(mockMvc, directDiscount,
                MarkToParticipateRequest.builder()
                        .item(DirectDiscountMarkToParticipateItem.builder()
                                .id(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                                .participates(true)
                                .mechanicsProperties(DirectDiscountOfferProperties.builder()
                                        .fixedBasePrice(BigDecimal.TEN)
                                        .fixedPrice(BigDecimal.ONE)
                                        .minimalDiscountPercentSize(BigDecimal.TEN)
                                        .build())
                                .build())
                        .build()).andExpect(status().isOk());

        OfferItemsPagingResponse response = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey()).build(),
                MBOCAuthenticationRequest.builder()
                        .roles(Set.of(SecurityRoles.VIEWER))
                        .build());

        assertThat(response.getMeta().getPublishStatus(), is(PublishActionState.DISABLED_NO_ACCESS));
    }

    @Test
    void shouldSkipInaccessibleAssortmentOnImport() throws Exception {
        final byte[] bytes = Files.readAllBytes(assortmentImportResource.getFile().toPath());

        //данные не совсем корректные с точки зрения связки магазина и параметра типа поставщика
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        shopSku("qxr.215928"),
                        shop(SHOP_ID),
                        potentialPromo(directDiscount.getId()),
                        supplierType(SupplierType._3P)
                ),
                datacampOffer(
                        shopSku("etu.200327"),
                        shop(SHOP_ID),
                        potentialPromo(directDiscount.getId()),
                        supplierType(SupplierType._1P)
                )
        ));

        cachedAssortmentService.refreshAssortmentCache();

        MockMultipartFile importFile = new MockMultipartFile(
                "files",
                "assortment.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes);

        ImportResponse importResponse = AssortmentRequests.importXlsx(mockMvc, directDiscount, importFile);

        assertThat(importResponse.getErrors(), nullValue());

        OfferItemsPagingResponse response = AssortmentRequests.getAssortment(mockMvc,
                AssortmentRequest.builder(directDiscount.toPromoKey())
                        .filter(AssortmentFilter.SSKU, "qxr.215928")
                        .build(),
                MBOCAuthenticationRequest.builder()
                        .roles(Set.of(SecurityRoles.VIEWER))
                        .build());

        assertThat(response.getItems(), not(empty()));

        OfferItem offerItem = response.getItems().get(0);

        assertThat(offerItem.isParticipates(), is(false));
    }
}
