package ru.yandex.market.loyalty.back.controller.discount;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams;
import ru.yandex.market.experiment.common.ExperimentContext;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.bundle.OrderExtraChargeDeliveryParams;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryPromoResponse;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryRequest;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackRegionSettingsTest;
import ru.yandex.market.loyalty.core.config.Blackbox;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.UserDataFactory;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.downloadable;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;

@TestFor(DiscountController.class)
@Log4j2
// Тесты для механизма из задачи https://st.yandex-team.ru/MARKETDISCOUNT-8311#626bd7a78d03c61488b56e11
public class DiscountControllerExtraChargeDeliveryTest extends MarketLoyaltyBackRegionSettingsTest {

    private static final long MOSCOW_REGION = 213L;

    @Autowired
    private ConfigurationService configurationService;
    @Value("classpath:data/extra-charge-cases.json")
    private Resource extraChargeTestCases;
    @Blackbox
    @Autowired
    public RestTemplate blackBoxTemplate;

    @Before
    public void setUp() throws Exception {
        configurationService.set(ConfigurationService.DELIVERY_EXTRA_CHARGE_CALCULATION_ENABLED, true);
        configurationService.set(ConfigurationService.CONSIDER_EXTRA_CHARGE_IN_DELIVERY_ZERO_PRICE_CALCULATION, true);
    }

    @Test
    public void shouldCalcExtraCharge() {
        OrderWithBundlesRequest order1 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        downloadable(true),
                        price(100)
                )
                .withDeliveries(DeliveryRequest.Builder.create()
                        .setId("0")
                        .setRegion(MOSCOW_REGION)
                        .setPrice(BigDecimal.valueOf(150))
                        .setSelected(true)
                        .setUnitEconomyValue(BigDecimal.valueOf(-300))
                        .setType(DeliveryType.COURIER)
                        .build()
                )
                .withExtraChargeDeliveryParams(OrderExtraChargeDeliveryParams.Builder.builder()
                        .setMaxCharge(BigDecimal.valueOf(1499.5))
                        .setChargeQuant(BigDecimal.valueOf(50))
                        .setVatMultiplier(BigDecimal.valueOf(12, 1))
                        .setMinChargeOfGmv(BigDecimal.valueOf(0.005))
                        .setMinCharge(BigDecimal.valueOf(199))
                        .build())
                .build();

        OrderWithBundlesRequest order2 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        downloadable(true),
                        price(100)
                )
                .withDeliveries(DeliveryRequest.Builder.create()
                        .setId("1")
                        .setRegion(MOSCOW_REGION)
                        .setPrice(BigDecimal.valueOf(150))
                        .setSelected(true)
                        .setUnitEconomyValue(BigDecimal.valueOf(-200))
                        .setType(DeliveryType.COURIER)
                        .build()
                )
                .withExtraChargeDeliveryParams(OrderExtraChargeDeliveryParams.Builder.builder()
                        .setMaxCharge(BigDecimal.valueOf(1499.5))
                        .setChargeQuant(BigDecimal.valueOf(50))
                        .setVatMultiplier(BigDecimal.valueOf(12, 1))
                        .setMinChargeOfGmv(BigDecimal.valueOf(0.005))
                        .setMinCharge(BigDecimal.valueOf(199))
                        .build())
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(CheckoutCommonParams.X_EXPERIMENTS, ExperimentContext.NEW_SPREAD_ALGORITHM_EXP);
        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order1, order2)
                                .build(),
                        httpHeaders
                );

        List<DeliveryPromoResponse> promoResponses = discountResponse.getOrders().stream()
                .flatMap(o -> o.getDeliveries().stream())
                .flatMap(o -> o.getPromos().stream())
                .collect(Collectors.toList());
        assertThat(promoResponses, containsInAnyOrder(
                allOf(
                        hasProperty("promoType", equalTo(PromoType.MULTICART_DISCOUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(-105))),
                        hasProperty("extraCharge", comparesEqualTo(BigDecimal.valueOf(180)))
                ),
                allOf(
                        hasProperty("promoType", equalTo(PromoType.MULTICART_DISCOUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(-195))),
                        hasProperty("extraCharge", comparesEqualTo(BigDecimal.valueOf(270)))
                )
        ));
    }

    @Test
    public void shouldNotCalcExtraChargeOnNullUnitEconomy() {
        OrderWithBundlesRequest order1 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        downloadable(true),
                        price(100)
                )
                .withDeliveries(DeliveryRequest.Builder.create()
                        .setId("0")
                        .setRegion(MOSCOW_REGION)
                        .setPrice(BigDecimal.valueOf(150))
                        .setSelected(true)
                        .setType(DeliveryType.COURIER)
                        .build()
                )
                .withExtraChargeDeliveryParams(OrderExtraChargeDeliveryParams.Builder.builder()
                        .setMaxCharge(BigDecimal.valueOf(1499.5))
                        .setChargeQuant(BigDecimal.valueOf(50))
                        .setVatMultiplier(BigDecimal.valueOf(12, 1))
                        .setMinChargeOfGmv(BigDecimal.valueOf(0.005))
                        .setMinCharge(BigDecimal.valueOf(199))
                        .build())
                .build();

        OrderWithBundlesRequest order2 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        downloadable(true),
                        price(100)
                )
                .withDeliveries(DeliveryRequest.Builder.create()
                        .setId("1")
                        .setRegion(MOSCOW_REGION)
                        .setPrice(BigDecimal.valueOf(150))
                        .setSelected(true)
                        .setType(DeliveryType.COURIER)
                        .build()
                )
                .withExtraChargeDeliveryParams(OrderExtraChargeDeliveryParams.Builder.builder()
                        .setMaxCharge(BigDecimal.valueOf(1499.5))
                        .setChargeQuant(BigDecimal.valueOf(50))
                        .setVatMultiplier(BigDecimal.valueOf(12, 1))
                        .setMinChargeOfGmv(BigDecimal.valueOf(0.005))
                        .setMinCharge(BigDecimal.valueOf(199))
                        .build())
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(CheckoutCommonParams.X_EXPERIMENTS, ExperimentContext.NEW_SPREAD_ALGORITHM_EXP);
        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order1, order2)
                                .build(),
                        httpHeaders
                );
        List<DeliveryPromoResponse> promoResponses = discountResponse.getOrders().stream()
                .flatMap(o -> o.getDeliveries().stream())
                .flatMap(o -> o.getPromos().stream())
                .collect(Collectors.toList());
        assertThat(promoResponses, containsInAnyOrder(
                allOf(
                        hasProperty("promoType", equalTo(PromoType.MULTICART_DISCOUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(75))),
                        hasProperty("extraCharge", comparesEqualTo(BigDecimal.valueOf(0)))
                ),
                allOf(
                        hasProperty("promoType", equalTo(PromoType.MULTICART_DISCOUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(75))),
                        hasProperty("extraCharge", comparesEqualTo(BigDecimal.valueOf(0)))
                )
        ));
    }

    @Test
    public void shouldCalcZeroExtraChargeOnPositiveUnitEconomy() {
        OrderWithBundlesRequest order1 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        downloadable(true),
                        price(100)
                )
                .withDeliveries(DeliveryRequest.Builder.create()
                        .setId("0")
                        .setRegion(MOSCOW_REGION)
                        .setPrice(BigDecimal.valueOf(150))
                        .setSelected(true)
                        .setType(DeliveryType.COURIER)
                        .setUnitEconomyValue(BigDecimal.TEN)
                        .build()
                )
                .withExtraChargeDeliveryParams(OrderExtraChargeDeliveryParams.Builder.builder()
                        .setMaxCharge(BigDecimal.valueOf(1499.5))
                        .setChargeQuant(BigDecimal.valueOf(50))
                        .setVatMultiplier(BigDecimal.valueOf(12, 1))
                        .setMinChargeOfGmv(BigDecimal.valueOf(0.005))
                        .setMinCharge(BigDecimal.valueOf(199))
                        .build())
                .build();

        OrderWithBundlesRequest order2 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        downloadable(true),
                        price(100)
                )
                .withDeliveries(DeliveryRequest.Builder.create()
                        .setId("1")
                        .setRegion(MOSCOW_REGION)
                        .setPrice(BigDecimal.valueOf(150))
                        .setSelected(true)
                        .setType(DeliveryType.COURIER)
                        .setUnitEconomyValue(BigDecimal.TEN)
                        .build()
                )
                .withExtraChargeDeliveryParams(OrderExtraChargeDeliveryParams.Builder.builder()
                        .setMaxCharge(BigDecimal.valueOf(1499.5))
                        .setChargeQuant(BigDecimal.valueOf(50))
                        .setVatMultiplier(BigDecimal.valueOf(12, 1))
                        .setMinChargeOfGmv(BigDecimal.valueOf(0.005))
                        .setMinCharge(BigDecimal.valueOf(199))
                        .build())
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(CheckoutCommonParams.X_EXPERIMENTS, ExperimentContext.NEW_SPREAD_ALGORITHM_EXP);
        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order1, order2)
                                .build(),
                        httpHeaders
                );

        List<DeliveryPromoResponse> promoResponses = discountResponse.getOrders().stream()
                .flatMap(o -> o.getDeliveries().stream())
                .flatMap(o -> o.getPromos().stream())
                .collect(Collectors.toList());
        assertThat(promoResponses, containsInAnyOrder(
                allOf(
                        hasProperty("promoType", equalTo(PromoType.MULTICART_DISCOUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(75))),
                        hasProperty("extraCharge", comparesEqualTo(BigDecimal.valueOf(0)))
                ),
                allOf(
                        hasProperty("promoType", equalTo(PromoType.MULTICART_DISCOUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(75))),
                        hasProperty("extraCharge", comparesEqualTo(BigDecimal.valueOf(0)))
                ))
        );
    }

    @Test
    public void shouldSpreadExtraChargeOnOrdersWithNegativeUnitEconomy() {
        OrderWithBundlesRequest order1 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        downloadable(true),
                        price(100)
                )
                .withDeliveries(DeliveryRequest.Builder.create()
                        .setId("0")
                        .setRegion(MOSCOW_REGION)
                        .setPrice(BigDecimal.valueOf(150))
                        .setSelected(true)
                        .setType(DeliveryType.COURIER)
                        .setUnitEconomyValue(BigDecimal.valueOf(-500))
                        .build()
                )
                .withExtraChargeDeliveryParams(OrderExtraChargeDeliveryParams.Builder.builder()
                        .setMaxCharge(BigDecimal.valueOf(1499.5))
                        .setChargeQuant(BigDecimal.valueOf(50))
                        .setVatMultiplier(BigDecimal.valueOf(12, 1))
                        .setMinChargeOfGmv(BigDecimal.valueOf(0.005))
                        .setMinCharge(BigDecimal.valueOf(199))
                        .build())
                .build();

        OrderWithBundlesRequest order2 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        downloadable(true),
                        price(100)
                )
                .withDeliveries(DeliveryRequest.Builder.create()
                        .setId("1")
                        .setRegion(MOSCOW_REGION)
                        .setPrice(BigDecimal.valueOf(150))
                        .setSelected(true)
                        .setType(DeliveryType.COURIER)
                        .setUnitEconomyValue(BigDecimal.valueOf(200))
                        .build()
                )
                .withExtraChargeDeliveryParams(OrderExtraChargeDeliveryParams.Builder.builder()
                        .setMaxCharge(BigDecimal.valueOf(1499.5))
                        .setChargeQuant(BigDecimal.valueOf(50))
                        .setVatMultiplier(BigDecimal.valueOf(12, 1))
                        .setMinChargeOfGmv(BigDecimal.valueOf(0.005))
                        .setMinCharge(BigDecimal.valueOf(199))
                        .build())
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(CheckoutCommonParams.X_EXPERIMENTS, ExperimentContext.NEW_SPREAD_ALGORITHM_EXP);
        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order1, order2)
                                .build(),
                        httpHeaders
                );

        List<DeliveryPromoResponse> promoResponses = discountResponse.getOrders().stream()
                .flatMap(o -> o.getDeliveries().stream())
                .flatMap(o -> o.getPromos().stream())
                .collect(Collectors.toList());
        assertThat(promoResponses, containsInAnyOrder(
                allOf(
                        hasProperty("promoType", equalTo(PromoType.MULTICART_DISCOUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(-125))),
                        hasProperty("extraCharge", comparesEqualTo(BigDecimal.valueOf(200)))
                ),
                allOf(
                        hasProperty("promoType", equalTo(PromoType.MULTICART_DISCOUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(75))),
                        hasProperty("extraCharge", comparesEqualTo(BigDecimal.ZERO))
                ))
        );
    }

    @Test
    public void shouldCorrectlyCalcOnSingleOrderWithSeveralDeliveryOptions() throws JsonProcessingException {
        OrderWithBundlesRequest order1 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        downloadable(true),
                        price(223),
                        quantity(19)
                )
                .withDeliveries(DeliveryRequest.Builder.create()
                                .setId("0")
                                .setRegion(MOSCOW_REGION)
                                .setPrice(BigDecimal.valueOf(49))
                                .setSelected(false)
                                .setUnitEconomyValue(null)
                                .setType(DeliveryType.COURIER)
                                .build(),
                        DeliveryRequest.Builder.create()
                                .setId("1")
                                .setRegion(MOSCOW_REGION)
                                .setPrice(BigDecimal.valueOf(49))
                                .setSelected(true)
                                .setUnitEconomyValue(BigDecimal.valueOf(-500))
                                .setType(DeliveryType.COURIER)
                                .build()
                )
                .withExtraChargeDeliveryParams(OrderExtraChargeDeliveryParams.Builder.builder()
                        .setMaxCharge(BigDecimal.valueOf(799))
                        .setChargeQuant(BigDecimal.valueOf(50))
                        .setVatMultiplier(BigDecimal.valueOf(12, 1))
                        .setMinChargeOfGmv(BigDecimal.valueOf(0.005))
                        .setMinCharge(BigDecimal.valueOf(199))
                        .build())
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(CheckoutCommonParams.X_EXPERIMENTS, ExperimentContext.NEW_SPREAD_ALGORITHM_EXP);
        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(DiscountRequestWithBundlesBuilder.builder(order1)
                                .build(),
                        httpHeaders
                );

        List<DeliveryPromoResponse> promoResponses = discountResponse.getOrders().stream()
                .flatMap(o -> o.getDeliveries().stream())
                .flatMap(o -> o.getPromos().stream())
                .collect(Collectors.toList());
        assertThat(promoResponses, containsInAnyOrder(
                allOf(
                        hasProperty("promoType", equalTo(PromoType.MULTICART_DISCOUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(-550))),
                        hasProperty("extraCharge", comparesEqualTo(BigDecimal.valueOf(550)))
                ),
                allOf(
                        hasProperty("promoType", equalTo(PromoType.MULTICART_DISCOUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(-550))),
                        hasProperty("extraCharge", comparesEqualTo(BigDecimal.valueOf(550)))
                )
        ));
    }

    @Test
    public void shouldCalcExtraChargeWithFreeTariff() {
        OrderWithBundlesRequest order1 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        downloadable(true),
                        price(100)
                )
                .withDeliveries(DeliveryRequest.Builder.create()
                        .setId("0")
                        .setRegion(MOSCOW_REGION)
                        .setPrice(BigDecimal.ZERO)
                        .setSelected(true)
                        .setUnitEconomyValue(BigDecimal.valueOf(-300))
                        .setType(DeliveryType.COURIER)
                        .build()
                )
                .withExtraChargeDeliveryParams(OrderExtraChargeDeliveryParams.Builder.builder()
                        .setMaxCharge(BigDecimal.valueOf(1499.5))
                        .setChargeQuant(BigDecimal.valueOf(50))
                        .setVatMultiplier(BigDecimal.valueOf(12, 1))
                        .setMinChargeOfGmv(BigDecimal.valueOf(0.005))
                        .setMinCharge(BigDecimal.valueOf(199))
                        .build())
                .build();

        OrderWithBundlesRequest order2 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        downloadable(true),
                        price(100)
                )
                .withDeliveries(DeliveryRequest.Builder.create()
                        .setId("1")
                        .setRegion(MOSCOW_REGION)
                        .setPrice(BigDecimal.ZERO)
                        .setSelected(true)
                        .setUnitEconomyValue(BigDecimal.valueOf(-200))
                        .setType(DeliveryType.COURIER)
                        .build()
                )
                .withExtraChargeDeliveryParams(OrderExtraChargeDeliveryParams.Builder.builder()
                        .setMaxCharge(BigDecimal.valueOf(1499.5))
                        .setChargeQuant(BigDecimal.valueOf(50))
                        .setVatMultiplier(BigDecimal.valueOf(12, 1))
                        .setMinChargeOfGmv(BigDecimal.valueOf(0.005))
                        .setMinCharge(BigDecimal.valueOf(199))
                        .build())
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(CheckoutCommonParams.X_EXPERIMENTS, ExperimentContext.NEW_SPREAD_ALGORITHM_EXP);
        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order1, order2)
                                .build(),
                        httpHeaders
                );

        List<DeliveryPromoResponse> promoResponses = discountResponse.getOrders().stream()
                .flatMap(o -> o.getDeliveries().stream())
                .flatMap(o -> o.getPromos().stream())
                .collect(Collectors.toList());
        assertThat(promoResponses, containsInAnyOrder(
                allOf(
                        hasProperty("promoType", equalTo(PromoType.MULTICART_DISCOUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(-359))),
                        hasProperty("extraCharge", comparesEqualTo(BigDecimal.valueOf(359)))
                ),
                allOf(
                        hasProperty("promoType", equalTo(PromoType.MULTICART_DISCOUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(-240))),
                        hasProperty("extraCharge", comparesEqualTo(BigDecimal.valueOf(240)))
                )
        ));
    }

    @Test
    public void shouldNotCalcExtraChargeWithYaPlus() {
        BlackboxUtils.mockBlackbox(UserDataFactory.DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        BlackboxUtils.mockBlackboxResponse(true, PerkType.YANDEX_PLUS);
        OrderWithBundlesRequest order1 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        downloadable(true),
                        price(100)
                )
                .withDeliveries(DeliveryRequest.Builder.create()
                        .setId("0")
                        .setRegion(MOSCOW_REGION)
                        .setPrice(BigDecimal.ZERO)
                        .setSelected(true)
                        .setUnitEconomyValue(BigDecimal.ZERO)
                        .setType(DeliveryType.COURIER)
                        .build()
                )
                .withExtraChargeDeliveryParams(OrderExtraChargeDeliveryParams.Builder.builder()
                        .setMaxCharge(BigDecimal.valueOf(1499.5))
                        .setChargeQuant(BigDecimal.valueOf(50))
                        .setVatMultiplier(BigDecimal.valueOf(12, 1))
                        .setMinChargeOfGmv(BigDecimal.valueOf(0.005))
                        .setMinCharge(BigDecimal.valueOf(199))
                        .build())
                .build();

        OrderWithBundlesRequest order2 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        downloadable(true),
                        price(100)
                )
                .withDeliveries(DeliveryRequest.Builder.create()
                        .setId("1")
                        .setRegion(MOSCOW_REGION)
                        .setPrice(BigDecimal.ZERO)
                        .setSelected(true)
                        .setUnitEconomyValue(BigDecimal.ZERO)
                        .setType(DeliveryType.COURIER)
                        .build()
                )
                .withExtraChargeDeliveryParams(OrderExtraChargeDeliveryParams.Builder.builder()
                        .setMaxCharge(BigDecimal.valueOf(1499.5))
                        .setChargeQuant(BigDecimal.valueOf(50))
                        .setVatMultiplier(BigDecimal.valueOf(12, 1))
                        .setMinChargeOfGmv(BigDecimal.valueOf(0.005))
                        .setMinCharge(BigDecimal.valueOf(199))
                        .build())
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(CheckoutCommonParams.X_EXPERIMENTS, ExperimentContext.NEW_SPREAD_ALGORITHM_EXP);
        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order1, order2)
                                .build(),
                        httpHeaders
                );

        List<DeliveryPromoResponse> promoResponses = discountResponse.getOrders().stream()
                .flatMap(o -> o.getDeliveries().stream())
                .flatMap(o -> o.getPromos().stream())
                .collect(Collectors.toList());
        assertThat(promoResponses, hasSize(0));
        BlackboxUtils.mockBlackbox(UserDataFactory.DEFAULT_UID, PerkType.YANDEX_PLUS, false, blackboxRestTemplate);
    }

    @Test
    @Ignore
    public void testIncomingRequest() throws JsonProcessingException {
        final String requestJson = "";
        MultiCartWithBundlesDiscountRequest request = objectMapper.readValue(requestJson,
                MultiCartWithBundlesDiscountRequest.class);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(CheckoutCommonParams.X_EXPERIMENTS, ExperimentContext.NEW_SPREAD_ALGORITHM_EXP);
        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(request,
                        httpHeaders
                );

        List<DeliveryPromoResponse> promoResponses = discountResponse.getOrders().stream()
                .flatMap(o -> o.getDeliveries().stream())
                .flatMap(o -> o.getPromos().stream())
                .collect(Collectors.toList());
        assertThat(promoResponses, containsInAnyOrder(
                allOf(
                        hasProperty("promoType", equalTo(PromoType.MULTICART_DISCOUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(-450))),
                        hasProperty("extraCharge", comparesEqualTo(BigDecimal.valueOf(450)))
                ),
                allOf(
                        hasProperty("promoType", equalTo(PromoType.MULTICART_DISCOUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(-450))),
                        hasProperty("extraCharge", comparesEqualTo(BigDecimal.valueOf(450)))
                )
        ));
    }

    @Test
    public void checkTestCasesFromFile() throws IOException {
        File file = extraChargeTestCases.getFile();
        String content = Files.readString(file.toPath());
        JsonNode jsonNode = objectMapper.readTree(content);
        if (!jsonNode.isArray()) {
            throw new IllegalArgumentException("Wrong file format");
        }
        ArrayNode jsonArray = (ArrayNode) jsonNode;
        for (JsonNode node : jsonArray) {
            MultiCartWithBundlesDiscountRequest request = objectMapper.readValue(node.get("request").toString(),
                    MultiCartWithBundlesDiscountRequest.class);
            MultiCartWithBundlesDiscountResponse response = objectMapper.readValue(node.get("response").toString(),
                    MultiCartWithBundlesDiscountResponse.class);
            testCase(request, response);
        }
    }

    private void testCase(MultiCartWithBundlesDiscountRequest request, MultiCartWithBundlesDiscountResponse response) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(CheckoutCommonParams.X_EXPERIMENTS, ExperimentContext.NEW_SPREAD_ALGORITHM_EXP);
        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(request,
                        httpHeaders
                );

        List<DeliveryPromoResponse> actualResponse = discountResponse.getOrders().stream()
                .flatMap(o -> o.getDeliveries().stream())
                .flatMap(o -> o.getPromos().stream())
                .collect(Collectors.toList());
        List<DeliveryPromoResponse> expectedResponse = response.getOrders().stream()
                .flatMap(o -> o.getDeliveries().stream())
                .flatMap(o -> o.getPromos().stream())
                .collect(Collectors.toList());

        assertThat(expectedResponse, containsInAnyOrder(actualResponse.toArray(new DeliveryPromoResponse[0])));
    }

}
