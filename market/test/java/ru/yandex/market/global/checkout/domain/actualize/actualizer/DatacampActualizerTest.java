package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.ActualizationError;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.factory.TestGetUnitedOfferFactory;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.common.datacamp.DataCampClient;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderItem;
import ru.yandex.mj.generated.server.model.ShopExportDto;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Deprecated
@Disabled
public class DatacampActualizerTest extends BaseFunctionalTest {

    public static final String MARKET_CATEGORY_ID = "marketCategoryId";
    private static final long SHOP_ID = 40;
    private static final long BUSINESS_ID = 41;
    private static final String OFFER_ID = "OFFER_ID";
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(DatacampActualizerTest.class).build();

    private final DataCampClient dataCampClient;
    private final OrderDatacampActualizer orderDatacampActualizer;
    private final CartDatacampActualizer cartDatacampActualizer;
    private final TestOrderFactory testOrderFactory;
    private final TestClock clock;

    @Autowired
    private TestGetUnitedOfferFactory getUnitedOfferFactory;

    @BeforeEach
    public void setup() {
        Mockito.when(dataCampClient.getAvailableOffers(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList()))
                .thenReturn(getUnitedOfferFactory.build());
    }

    private OrderActualization buildOrderActualizationFrom(SyncGetOffer.GetUnitedOffersResponse predefined) {
        DataCampOffer.Offer basic = predefined.getOffers(0).getBasic();
        DataCampOfferContent.OriginalSpecification basicOrig = basic.getContent().getPartner().getOriginal();
        DataCampOffer.Offer service = predefined.getOffers(0).getServiceOrThrow((int) SHOP_ID);

        Order order = new Order().setBusinessId(BUSINESS_ID).setShopId(SHOP_ID);
        List<OrderItem> item = List.of(RANDOM.nextObject(OrderItem.class).setOfferId(OFFER_ID)
                .setName(basicOrig.getName().getValue())
                .setPrice(service.getPrice().getBasic().getBinaryPrice().getPrice() / 100_000));
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
    public void testValidDataCamp() {
        SyncGetOffer.GetUnitedOffersResponse predefined = getUnitedOfferFactory.build();

        Mockito.when(dataCampClient.getAvailableOffers(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList())).thenReturn(predefined);
        DataCampOffer.Offer basic = predefined.getOffers(0).getBasic();
        DataCampOfferContent.OriginalSpecification basicOrig = basic.getContent().getPartner().getOriginal();

        OrderActualization srcActualization = buildOrderActualizationFrom(predefined);
        ShopExportDto shopExportDto = srcActualization.getShopExportDto();
        List<OrderItem> orderItems = srcActualization.getOrderItems();
        OrderActualization actualize = orderDatacampActualizer.actualize(srcActualization);
        Assertions.assertThat(actualize.getOrderItems().size()).isEqualTo(1);
        OrderItem orderItem = actualize.getOrderItems().get(0);
        Assertions.assertThat(orderItem.getCurrency().getLiteral()).isEqualTo(shopExportDto.getCurrency().getValue());
        Assertions.assertThat(orderItem.getOfferId()).isEqualTo(OFFER_ID);
        Assertions.assertThat(orderItem.getBusinessId()).isEqualTo(BUSINESS_ID);
        Assertions.assertThat(orderItem.getShopId()).isEqualTo(SHOP_ID);
        Assertions.assertThat(orderItem.getPrice()).isEqualTo(orderItems.get(0).getPrice());
        Assertions.assertThat(orderItem.getVat()).isEqualTo(BigDecimal.valueOf(shopExportDto.getVat()));

        Assertions.assertThat(orderItem.getName()).isEqualTo(basicOrig.getName().getValue());
        Assertions.assertThat(orderItem.getDescription()).isEqualTo(basicOrig.getDescription().getValue());
        Assertions.assertThat(orderItem.getCategoryId()).isEqualTo(basicOrig.getCategory().getId());
        Assertions.assertThat(orderItem.getVendor()).isEqualTo(basicOrig.getVendor().getValue());
        Assertions.assertThat(orderItem.getPictures().length).isEqualTo(basic.getPictures().getPartner().getActualCount());
    }

    @Test
    public void testAdult() {
        Mockito.when(dataCampClient.getAvailableOffers(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList()))
                .thenReturn(getUnitedOfferFactory.build(
                        123, true, BUSINESS_ID, SHOP_ID, OFFER_ID
                ));

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

        OrderActualization after = orderDatacampActualizer.actualize(before);
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
        testBasicErrors(orderDatacampActualizer, 0, 1);
    }

    @Test
    public void testCartError() {
        testBasicErrors(cartDatacampActualizer, 1, 0);
    }

    private void testBasicErrors(DatacampActualizer actualizer, int warnCount, int errCounts) {
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
                orderDatacampActualizer.actualize(new OrderActualization().setOrder(order).setOrderItems(item).setErrors(errors).setWarnings(warns).setShopExportDto(export));
        Assertions.assertThat(actualize.getErrors().size()).isEqualTo(1);
        ActualizationError actualizationError = actualize.getErrors().get(0);
        Assertions.assertThat(actualizationError.getMessage()).startsWith("Name of item is differ");
        Assertions.assertThat(actualizationError.getCode()).isEqualTo(ActualizationError.Code.ITEM_DIFFER);
    }


    @Test
    public void testInvalidPrice() {
        SyncGetOffer.GetUnitedOffersResponse predefined = getUnitedOfferFactory.build();

        Mockito.when(dataCampClient.getAvailableOffers(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList())).thenReturn(predefined);

        OrderActualization srcActualization = buildOrderActualizationFrom(predefined);
        srcActualization.getOrderItems().get(0).setPrice(555555555L);
        OrderActualization actualize = orderDatacampActualizer.actualize(srcActualization);
        Assertions.assertThat(actualize.getErrors().size()).isEqualTo(1);
        ActualizationError actualizationError = actualize.getErrors().get(0);
        Assertions.assertThat(actualizationError.getMessage()).startsWith("Price of item is differ");
        Assertions.assertThat(actualizationError.getCode()).isEqualTo(ActualizationError.Code.ITEM_DIFFER);
    }

    @Test
    public void testDatacampNoPrice() {

        SyncGetOffer.GetUnitedOffersResponse predefined = getUnitedOfferFactory.build(0);

        Mockito.when(dataCampClient.getAvailableOffers(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList())).thenReturn(predefined);

        OrderActualization srcActualization = buildOrderActualizationFrom(predefined);
        OrderActualization actualize = orderDatacampActualizer.actualize(srcActualization);
        Assertions.assertThat(actualize.getErrors().size()).isEqualTo(1);
        ActualizationError actualizationError = actualize.getErrors().get(0);
        Assertions.assertThat(actualizationError.getMessage()).isEqualTo("DataCamp price for item is not set");
        Assertions.assertThat(actualizationError.getCode()).isEqualTo(ActualizationError.Code.ITEM_UNAVAILABLE);
    }

    @Test
    public void testValidMarketCategoryId() {

        SyncGetOffer.GetUnitedOffersResponse predefined = getUnitedOfferFactory.buildWithOfferParams(
                () -> Map.of(MARKET_CATEGORY_ID, "44")
        );

        Mockito.when(dataCampClient.getAvailableOffers(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList())).thenReturn(predefined);

        OrderActualization srcActualization = buildOrderActualizationFrom(predefined);
        OrderActualization actualize = orderDatacampActualizer.actualize(srcActualization);
        Assertions.assertThat(actualize.getOrderItems().size()).isEqualTo(1);
        OrderItem orderItem = actualize.getOrderItems().get(0);
        Assertions.assertThat(orderItem.getMarketCategoryId()).isEqualTo(44L);
    }

    @Test
    public void testInValidMarketCategoryId() {

        SyncGetOffer.GetUnitedOffersResponse predefined = getUnitedOfferFactory.buildWithOfferParams(
                () -> Map.of(MARKET_CATEGORY_ID, "someText")
        );

        Mockito.when(dataCampClient.getAvailableOffers(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList())).thenReturn(predefined);

        OrderActualization srcActualization = buildOrderActualizationFrom(predefined);
        Assertions.assertThatThrownBy(() -> orderDatacampActualizer.actualize(srcActualization));
    }

    @Test
    public void testMissingMarketCategoryId() {

        SyncGetOffer.GetUnitedOffersResponse predefined = getUnitedOfferFactory.buildWithOfferParams(Map::of);

        Mockito.when(dataCampClient.getAvailableOffers(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyList())).thenReturn(predefined);

        OrderActualization srcActualization = buildOrderActualizationFrom(predefined);
        srcActualization.getOrderItems().get(0).setMarketCategoryId(12345678L);
        OrderActualization actualize = orderDatacampActualizer.actualize(srcActualization);
        Assertions.assertThat(actualize.getOrderItems().size()).isEqualTo(1);
        OrderItem orderItem = actualize.getOrderItems().get(0);
        Assertions.assertThat(orderItem.getMarketCategoryId()).isEqualTo(12345678L);
    }


}
