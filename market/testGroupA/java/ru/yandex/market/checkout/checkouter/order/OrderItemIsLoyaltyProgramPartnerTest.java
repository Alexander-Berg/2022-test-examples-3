package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckoutParametersBuilder;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderViewModel;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.loyalty.api.model.CashbackOptions;
import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.CashbackPromoResponse;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.CashbackType;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.util.OrderUtils.firstOrder;
import static ru.yandex.market.checkout.util.fulfillment.FulfillmentConfigurer.applyFulfilmentParams;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;

public class OrderItemIsLoyaltyProgramPartnerTest extends AbstractWebTestBase {

    @Autowired
    private TestSerializationService serializationService;


    @Test
    @DisplayName("Проверка что новые поля partner_id, market_cashback_percent, partner_cashback_percent, " +
            " cms_description_semantic_id, details_group_name_id, ui_promo_flags, anaplanId," +
            "priority, promoBucketName, thresholds, marketTariffsVersionId, nominal" +
            "успешно сохраняются в БД после чекаута по новой логике парсинга ответа от лоялти")
    public void shouldSaveOrderToDatabaseByNewLogic() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_PARSE_PROMOS_ARRAY_FROM_LOYALTY, Boolean.TRUE);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SAVE_NEW_FIELDS_IN_ITEM_PROMO, Boolean.TRUE);
        Map<OrderItem, FoundOfferBuilder> foundOfferByItem = new HashMap<>();

        Parameters parameters = defaultBlueOrderParameters();
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));
        OrderItem firstItem = order.getItems().iterator().next();
        foundOfferByItem.computeIfAbsent(firstItem, i -> FoundOfferBuilder.createFrom(firstItem));
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        String cmsSemanticId = "default-partner-cashback";
        String detailsGroupName = "default-extra";
        List<String> uiPromoFlags = List.of("uiPromoFlag1", "uiPromoFlag2");
        long partnerId = 1L;
        BigDecimal marketTariff = BigDecimal.TEN;
        BigDecimal partnerTariff = BigDecimal.ZERO;
        String anaplanId = "anaplanId";
        String shopPromoId = "shopPromoId";
        String sourceType = "sourceType";
        List<CashbackPromoResponse> cashbackPromos =
                Collections.singletonList(CashbackPromoResponse.builder()
                        .setAmount(BigDecimal.ONE)
                        .setPromoKey("1")
                        .setPartnerId(partnerId)
                        .setNominal(BigDecimal.ONE)
                        .setMarketTariff(marketTariff)
                        .setPartnerTariff(partnerTariff)
                        .setUiPromoFlags(uiPromoFlags)
                        .setCmsSemanticId(cmsSemanticId)
                        .setDetailsGroupName(detailsGroupName)
                        .setAnaplanId(anaplanId)
                        .setShopPromoId(shopPromoId)
                        .setSourceType(sourceType)
                        .setPriority(1)
                        .setPromoBucketName("promoBucketName")
                        .setThresholds(List.of("1", "2"))
                        .setMarketTariffsVersionId(1L)
                        .setNominal(BigDecimal.TEN)
                        .build());

        parameters.getLoyaltyParameters()
                .expectResponseItems(
                        itemResponseFor(firstItem)
                                .quantity(2)
                                .cashback(new CashbackResponse(
                                        new CashbackOptions("1", 1,
                                                null,
                                                cashbackPromos,
                                                BigDecimal.ONE, CashbackPermision.ALLOWED, null,
                                                null, null, null), null))
                );
        loyaltyConfigurer.mockCalcsWithDynamicResponse(parameters);
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);

        Order orderFromDatabase = orderService.getOrder(firstOrder(checkout).getId());
        OrderItem orderItemFromDatabase = orderFromDatabase.getItems().iterator().next();
        ItemPromo itemPromoFromDatabase = orderItemFromDatabase.getPromos().iterator().next();

        assertThat(itemPromoFromDatabase.getPartnerId(), equalTo(partnerId));
        assertThat(itemPromoFromDatabase.getMarketCashbackPercent(), comparesEqualTo(marketTariff));
        assertThat(itemPromoFromDatabase.getPartnerCashbackPercent(), comparesEqualTo(partnerTariff));
        assertThat(itemPromoFromDatabase.getCmsDescriptionSemanticId(), equalTo(cmsSemanticId));
        assertThat(itemPromoFromDatabase.getUiPromoFlags(), equalTo(uiPromoFlags));
        assertThat(itemPromoFromDatabase.getDetailsGroupNameId(), equalTo(detailsGroupName));
        assertThat(itemPromoFromDatabase.getPriority(), comparesEqualTo(1));
        assertThat(itemPromoFromDatabase.getPromoBucketName(), equalTo("promoBucketName"));
        assertThat(itemPromoFromDatabase.getThresholds(), equalTo(List.of("1", "2")));
        assertThat(itemPromoFromDatabase.getMarketTariffsVersionId(), comparesEqualTo(1L));
        assertThat(itemPromoFromDatabase.getNominal(), comparesEqualTo(BigDecimal.TEN));
        List<OrderPromo> promos = orderFromDatabase.getPromos();
        assertThat(promos, hasSize(2));

        List<String> anaplanIds = promos.stream()
                .map(orderPromo -> orderPromo.getPromoDefinition().getAnaplanId())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertThat(anaplanIds, hasSize(1));
        assertThat(anaplanIds.get(0), equalTo(anaplanId));

        List<String> shopPromoIds = promos.stream()
                .map(orderPromo -> orderPromo.getPromoDefinition().getShopPromoId())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertThat(shopPromoIds, hasSize(1));
        assertThat(shopPromoIds.get(0), equalTo(shopPromoId));

        List<String> sourceTypes = promos.stream()
                .map(orderPromo -> orderPromo.getPromoDefinition().getSourceType())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertThat(sourceTypes, hasSize(1));
        assertThat(sourceTypes.get(0), equalTo(sourceType.toUpperCase()));
    }

    @Test
    @DisplayName("Проверка что новые поля partner_id, market_cashback_percent, partner_cashback_percent " +
            "успешно сохраняются в БД после чекаута по старой логике парсинга ответа от лоялти")
    public void shouldSaveOrderToDatabaseByOldLogic() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_PARSE_PROMOS_ARRAY_FROM_LOYALTY, Boolean.FALSE);
        Map<OrderItem, FoundOfferBuilder> foundOfferByItem = new HashMap<>();

        Parameters parameters = defaultBlueOrderParameters();
        Order order = parameters.getOrder();
        order.getItems().forEach(item -> applyFulfilmentParams(parameters.getReportParameters(), item));
        OrderItem firstItem = order.getItems().iterator().next();
        foundOfferByItem.computeIfAbsent(firstItem, i -> FoundOfferBuilder.createFrom(firstItem));
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);


        String marketPromoId = "marketPromoId";
        BigDecimal cashbackAccuralAmount = BigDecimal.valueOf(100);
        Map<String, BigDecimal> amountByPromoKey = Map.of(marketPromoId, cashbackAccuralAmount);
        parameters.getLoyaltyParameters()
                .expectResponseItems(
                        itemResponseFor(firstItem)
                                .quantity(2)
                                .cashback(new CashbackResponse(
                                        new CashbackOptions("1", 1,
                                                amountByPromoKey,
                                                null,
                                                BigDecimal.ONE, CashbackPermision.ALLOWED, null,
                                                null, null, null), null))
                );
        loyaltyConfigurer.mockCalcsWithDynamicResponse(parameters);
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);

        Order orderFromDatabase = orderService.getOrder(firstOrder(checkout).getId());
        OrderItem orderItemFromDatabase = orderFromDatabase.getItems().iterator().next();
        Set<ItemPromo> promos = orderItemFromDatabase.getPromos();
        ItemPromo promo = promos.iterator().next();

        assertThat(promos, hasSize(amountByPromoKey.size()));
        assertThat(promo.getPromoDefinition().getMarketPromoId(), equalTo(marketPromoId));
        assertThat(promo.getCashbackAccrualAmount(), comparesEqualTo(cashbackAccuralAmount));
        assertThat(promo.getUiPromoFlags(), nullValue());
    }

    @Test
    public void shouldReturnIsLoyaltyProgramPartnerAfterCheckout() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.configuration()
                .cart()
                .mockConfigurations()
                .values()
                .forEach(cfg -> cfg.getReportParameters().setLoyaltyProgramPartner(true));

        var cart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.mapCartToOrder(cart, parameters);
        pushApiConfigurer.mockAccept(multiOrder.getCarts().get(0), true);
        var multiOrderAfterCheckout = client.checkout(multiOrder,
                CheckoutParametersBuilder.aCheckoutParameters()
                        .withUid(parameters.getBuyer().getUid())
                        .withContext(Context.MARKET)
                        .withHitRateGroup(HitRateGroup.LIMIT)
                        .withApiSettings(ApiSettings.PRODUCTION)
                        .withRgb(Color.BLUE)
                        .build());
        var actualOrderFromDatabase = orderService.getOrder(firstOrder(multiOrderAfterCheckout).getId());
        var actualItemFromDatabase = actualOrderFromDatabase.getItems().iterator().next();
        assertThat(actualItemFromDatabase.isLoyaltyProgramPartner(), is(true));
    }

    @Test
    public void shouldReturnIsLoyaltyProgramPartnerInOrderViewModel() throws Exception {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.configuration()
                .cart()
                .mockConfigurations()
                .values()
                .forEach(cfg -> cfg.getReportParameters().setLoyaltyProgramPartner(true));
        var order = orderCreateHelper.createOrder(parameters);
        var result = mockMvc.perform(get("/orders/{orderId}", order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(CheckouterClientParams.CLIENT_ID, "0")
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var orderViewModel = serializationService.deserializeCheckouterObject(result, OrderViewModel.class);
        assertThat(orderViewModel.getItems().iterator().next().isLoyaltyProgramPartner(), is(true));
    }

    @Test
    public void shouldReturnIsLoyaltyProgramPartnerInOrderHistory() {
        var parameters = BlueParametersProvider.defaultBlueParametersWithDelivery(DELIVERY_SERVICE_ID);
        parameters.configuration()
                .cart()
                .mockConfigurations()
                .values()
                .forEach(cfg -> cfg.getReportParameters().setLoyaltyProgramPartner(true));
        var order = orderCreateHelper.createOrder(parameters);
        // самый простой способ сходить в базу и проверить, что в order_item_history проставился loyalty_program_partner
        var loyaltyProgramPartnerFlags = masterJdbcTemplate.queryForList(
                "select loyalty_program_partner from order_item_history where order_id=?", Boolean.class,
                order.getId());
        assertThat(Iterables.getOnlyElement(loyaltyProgramPartnerFlags), is(true));
    }
}
