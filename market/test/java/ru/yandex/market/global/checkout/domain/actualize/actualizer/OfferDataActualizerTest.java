package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.ActualizationError;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.factory.TestElasticOfferFactory;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.common.elastic.dictionary.DictionaryQueryService;
import ru.yandex.market.global.db.jooq.enums.EOrderPaymentCurrency;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderItem;
import ru.yandex.mj.generated.server.model.OfferDto;
import ru.yandex.mj.generated.server.model.ShopExportDto;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OfferDataActualizerTest extends BaseFunctionalTest {

    public static final long MARKET_CATEGORY_ID = 44;
    private static final long SHOP_ID = 40;
    private static final long BUSINESS_ID = 41;
    private static final String OFFER_ID = "OFFER_ID";
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(OfferDataActualizerTest.class).build();

    private final OrderOfferDataActualizer orderOfferDataActualizer;
    private final CartOfferDataActualizer cartOfferDataActualizer;
    private final DictionaryQueryService<OfferDto> offerDictionary;

    @Autowired
    private TestElasticOfferFactory testElasticOfferFactory;

    @BeforeEach
    public void setup() {
        Mockito.when(offerDictionary.get(Mockito.anyList()))
                .thenReturn(getMockedResponse());
    }

    private List<OfferDto> getMockedResponse() {
        return getMockedResponse(Function.identity());
    }

    private List<OfferDto> getMockedResponse(Function<OfferDto, OfferDto> setup) {
        return testElasticOfferFactory.buildOne(
                offerDto -> {
                    OfferDto defaultSetupOffer = offerDto.shopId(SHOP_ID)
                            .businessId(BUSINESS_ID)
                            .offerId(OFFER_ID)
                            .marketCategoryId(MARKET_CATEGORY_ID);
                    return setup.apply(defaultSetupOffer);
                }
        );
    }

    private OrderActualization buildOrderActualizationFrom(OfferDto predefined) {
        Order order = new Order().setBusinessId(BUSINESS_ID).setShopId(SHOP_ID);
        List<OrderItem> item = List.of(RANDOM.nextObject(OrderItem.class).setOfferId(OFFER_ID)
                .setName(predefined.getTitle())
                .setPrice(predefined.getPrice()));
        ShopExportDto export = RANDOM.nextObject(ShopExportDto.class).id(SHOP_ID);
        List<ActualizationError> warns = new ArrayList<>();
        List<ActualizationError> errors = new ArrayList<>();
        return new OrderActualization()
                .setOrder(order)
                .setOrderItems(item)
                .setErrors(errors)
                .setWarnings(warns)
                .setShopExportDto(export);
    }

    @Test
    public void testValid() {
        List<OfferDto> offers = getMockedResponse();
        Mockito.when(offerDictionary.get(Mockito.anyList())).thenReturn(offers);

        OfferDto elasticOffer = offers.get(0);

        OrderActualization srcActualization = buildOrderActualizationFrom(elasticOffer);
        ShopExportDto shopExportDto = srcActualization.getShopExportDto();
        OrderActualization actualize = orderOfferDataActualizer.actualize(srcActualization);
        Assertions.assertThat(actualize.getOrderItems().size()).isEqualTo(1);
        OrderItem orderItem = actualize.getOrderItems().get(0);

        Assertions.assertThat(orderItem).usingRecursiveComparison().ignoringExpectedNullFields()
                .isEqualTo(new OrderItem()
                        .setCurrency(EOrderPaymentCurrency.valueOf(shopExportDto.getCurrency().getValue()))
                        .setOfferId(OFFER_ID)
                        .setBusinessId(BUSINESS_ID)
                        .setShopId(SHOP_ID)
                        .setPrice(elasticOffer.getPrice())
                        .setVat(BigDecimal.valueOf(shopExportDto.getVat()))
                        .setName(elasticOffer.getTitle())
                        .setDescription(elasticOffer.getDescription())
                        .setCategoryId(elasticOffer.getCategory().getId())
                        .setVendor(elasticOffer.getVendor())
                        .setMarketCategoryId(elasticOffer.getMarketCategoryId())
                        .setPictures(elasticOffer.getPictures().toArray(String[]::new)));
    }

    @Test
    public void testAdult() {

        Mockito.when(offerDictionary.get(Mockito.anyList())).thenReturn(
                getMockedResponse(offer -> offer.price(123L).adult(true)));


        OrderActualization before = TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupShop(o -> o
                                .id(SHOP_ID)
                                .businessId(BUSINESS_ID)
                        )
                        .setupItems(oi -> List.of(new OrderItem()
                                .setOfferId(OFFER_ID)
                                .setCount(1L)
                                .setAdult(null)
                        ))
                        .build()
        );

        OrderActualization after = orderOfferDataActualizer.actualize(before);
        Assertions.assertThat(after.getOrderItems())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .containsExactlyInAnyOrder(
                        new OrderItem()
                                .setOfferId(OFFER_ID)
                                .setAdult(true)
                );
    }

    @Test
    public void testOrderError() {
        testBasicErrors(orderOfferDataActualizer, 0, 1);
    }

    @Test
    public void testCartError() {
        testBasicErrors(cartOfferDataActualizer, 1, 0);
    }

    private void testBasicErrors(OfferDataActualizer actualizer, int warnCount, int errCounts) {
        Order order = new Order().setBusinessId(BUSINESS_ID).setShopId(SHOP_ID);
        List<OrderItem> item = List.of(RANDOM.nextObject(OrderItem.class).setOfferId(OFFER_ID));
        ShopExportDto export = RANDOM.nextObject(ShopExportDto.class).id(SHOP_ID);
        List<ActualizationError> warns = new ArrayList<>();
        List<ActualizationError> errors = new ArrayList<>();
        OrderActualization actualize =
                actualizer.actualize(new OrderActualization().setOrder(order).setOrderItems(item).setErrors(errors).setWarnings(warns).setShopExportDto(export));
        Assertions.assertThat(actualize.getWarnings().size()).isEqualTo(warnCount);
        Assertions.assertThat(actualize.getErrors().size()).isEqualTo(errCounts);
        Assertions.assertThat(actualize.getOrderItems().size()).isEqualTo(0);
    }

    @Test
    public void testInvalidName() {
        Order order = new Order().setBusinessId(BUSINESS_ID).setShopId(SHOP_ID);
        List<OrderItem> item = List.of(RANDOM.nextObject(OrderItem.class).setOfferId(OFFER_ID));
        ShopExportDto export = RANDOM.nextObject(ShopExportDto.class).id(SHOP_ID);
        List<ActualizationError> warns = new ArrayList<>();
        List<ActualizationError> errors = new ArrayList<>();
        OrderActualization actualize =
                orderOfferDataActualizer.actualize(new OrderActualization().setOrder(order).setOrderItems(item)
                        .setErrors(errors).setWarnings(warns).setShopExportDto(export));
        Assertions.assertThat(actualize.getErrors().size()).isEqualTo(1);
        ActualizationError actualizationError = actualize.getErrors().get(0);
        Assertions.assertThat(actualizationError.getMessage()).startsWith("Name of item is differ");
        Assertions.assertThat(actualizationError.getCode()).isEqualTo(ActualizationError.Code.ITEM_DIFFER);
    }

    @Test
    public void testInvalidPrice() {
        List<OfferDto> offers = getMockedResponse();
        Mockito.when(offerDictionary.get(Mockito.anyList())).thenReturn(offers);

        OrderActualization srcActualization = buildOrderActualizationFrom(offers.get(0));
        srcActualization.getOrderItems().get(0).setPrice(555555555L);
        OrderActualization actualize = orderOfferDataActualizer.actualize(srcActualization);
        Assertions.assertThat(actualize.getErrors().size()).isEqualTo(1);
        ActualizationError actualizationError = actualize.getErrors().get(0);
        Assertions.assertThat(actualizationError.getMessage()).startsWith("Price of item is differ");
        Assertions.assertThat(actualizationError.getCode()).isEqualTo(ActualizationError.Code.ITEM_DIFFER);
    }

    @Test
    public void testNoPrice() {
        List<OfferDto> offers = getMockedResponse(offerDto -> offerDto.price(null));
        Mockito.when(offerDictionary.get(Mockito.anyList())).thenReturn(offers);

        OrderActualization srcActualization = buildOrderActualizationFrom(offers.get(0));
        OrderActualization actualize = orderOfferDataActualizer.actualize(srcActualization);
        Assertions.assertThat(actualize.getErrors().size()).isEqualTo(1);
        ActualizationError actualizationError = actualize.getErrors().get(0);
        Assertions.assertThat(actualizationError.getMessage()).isEqualTo("Price for item is not set");
        Assertions.assertThat(actualizationError.getCode()).isEqualTo(ActualizationError.Code.ITEM_UNAVAILABLE);
    }

    @Test
    public void testNegativePrice() {
        List<OfferDto> offers = getMockedResponse(offerDto -> offerDto.price(-110_00L));
        Mockito.when(offerDictionary.get(Mockito.anyList())).thenReturn(offers);

        OrderActualization srcActualization = buildOrderActualizationFrom(offers.get(0));
        OrderActualization actualize = orderOfferDataActualizer.actualize(srcActualization);
        Assertions.assertThat(actualize.getErrors().size()).isEqualTo(1);
        ActualizationError actualizationError = actualize.getErrors().get(0);
        Assertions.assertThat(actualizationError.getMessage()).isEqualTo("Price for item is not set");
        Assertions.assertThat(actualizationError.getCode()).isEqualTo(ActualizationError.Code.ITEM_UNAVAILABLE);
    }

    @Test
    public void testZeroPrice() {
        List<OfferDto> offers = getMockedResponse(offerDto -> offerDto.price(0L));
        Mockito.when(offerDictionary.get(Mockito.anyList())).thenReturn(offers);

        OrderActualization srcActualization = buildOrderActualizationFrom(offers.get(0));
        OrderActualization actualize = orderOfferDataActualizer.actualize(srcActualization);
        Assertions.assertThat(actualize.getErrors().size()).isEqualTo(1);
        ActualizationError actualizationError = actualize.getErrors().get(0);
        Assertions.assertThat(actualizationError.getMessage()).isEqualTo("Price for item is not set");
        Assertions.assertThat(actualizationError.getCode()).isEqualTo(ActualizationError.Code.ITEM_UNAVAILABLE);
    }

    @Test
    public void testMissingMarketCategoryId() {
        List<OfferDto> offers = getMockedResponse(offerDto -> offerDto.marketCategoryId(null));
        Mockito.when(offerDictionary.get(Mockito.anyList())).thenReturn(offers);

        OrderActualization srcActualization = buildOrderActualizationFrom(offers.get(0));
        srcActualization.getOrderItems().get(0).setMarketCategoryId(12345678L);
        OrderActualization actualize = orderOfferDataActualizer.actualize(srcActualization);
        Assertions.assertThat(actualize.getOrderItems().size()).isEqualTo(1);
        OrderItem orderItem = actualize.getOrderItems().get(0);
        Assertions.assertThat(orderItem.getMarketCategoryId()).isEqualTo(12345678L);
    }

    @Test
    public void testNoCategoryName() {
        List<OfferDto> offers = getMockedResponse(offerDto -> offerDto.category(null));
        Mockito.when(offerDictionary.get(Mockito.anyList())).thenReturn(offers);

        OrderActualization srcActualization = buildOrderActualizationFrom(offers.get(0));
        OrderActualization actualize = orderOfferDataActualizer.actualize(srcActualization);
        Assertions.assertThat(actualize.getErrors().size()).isEqualTo(1);
        ActualizationError actualizationError = actualize.getErrors().get(0);
        Assertions.assertThat(actualizationError.getMessage()).isEqualTo("Offer does not have shop category");
        Assertions.assertThat(actualizationError.getCode()).isEqualTo(ActualizationError.Code.ITEM_UNAVAILABLE);
    }

    @Test
    public void testDifferShop() {
        List<OfferDto> offers = getMockedResponse(offerDto -> offerDto.shopId(SHOP_ID + 1));
        Mockito.when(offerDictionary.get(Mockito.anyList())).thenReturn(offers);

        OrderActualization srcActualization = buildOrderActualizationFrom(offers.get(0));
        OrderActualization actualize = orderOfferDataActualizer.actualize(srcActualization);
        Assertions.assertThat(actualize.getErrors().size()).isEqualTo(1);
        ActualizationError actualizationError = actualize.getErrors().get(0);
        Assertions.assertThat(actualizationError.getMessage()).startsWith("Item shop is invalid");
        Assertions.assertThat(actualizationError.getCode()).isEqualTo(ActualizationError.Code.ITEM_DIFFER);
    }


}
