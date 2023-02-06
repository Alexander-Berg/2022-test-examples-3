package ru.yandex.market.checkout.checkouter.trace;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.RandomUtils;
import org.apache.curator.shaded.com.google.common.collect.Lists;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.AddressSource;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.YaLavkaHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.DeliveryOffer;
import ru.yandex.market.common.report.model.DeliveryTypeDistribution;
import ru.yandex.market.common.report.model.PickupOption;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

/**
 * @author mmetlov
 */
public class CheckoutContextHolderTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    YaLavkaHelper yaLavkaHelper;

    @Test
    @Disabled // по необъяснимым причинам падает в TeamCity, но проходит локально, позволяет потестить хотя бы локально
    public void testDeliverabilityMetrics() {

        ActualDelivery actualDelivery = new ActualDeliveryProvider.ActualDeliveryBuilder()
                .addDelivery(deliveryOption(1))
                .addDelivery(deliveryOption(2))
                .addPickup(pickupOption(1, Lists.newArrayList(1L, 2L, 3L)))
                .addPickup(pickupOption(2, Lists.newArrayList(4L)))
                .addPost(pickupOption(2, Lists.newArrayList(1L, 2L, 3L)))
                .addPost(pickupOption(3, Lists.newArrayList(4L)))
                .addOffers(generateOneDeliveryOffer())
                .addBucketActive(generateOneOrMorePickupDistribution(1L))
                .addBucketAll(generateOneOrMorePickupDistribution(1L))
                .addCarriersActive(generateOneOrMorePickupDistribution(1L, 2L))
                .addCarriersAll(generateOneOrMorePickupDistribution(1L, 2L))
                .addDimensions(Arrays.asList(BigDecimal.valueOf(11), BigDecimal.valueOf(10), BigDecimal.valueOf(10)))
                .build();

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .withActualDelivery(actualDelivery)
                .buildParameters();

        orderCreateHelper.cart(parameters);
        Map<String, Object> attributes = new CheckoutContextHolder.CheckoutAttributesHolder().getAttributes();
        assertThat(attributes.get("deliveryRegionId"), is(DeliveryProvider.REGION_ID));
        assertThat(attributes.get("warehouseId"), is(DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED.toString()));
        assertThat(attributes.get("minDeliveryDate"), is(Integer.toString(1)));
        assertThat(attributes.get("avgMinPickupDate"), is("1.25"));
        assertThat(attributes.get("avgMinPostDate"), is("2.25"));
        assertThat(attributes.get("parcelWeight"), is("10000"));
        assertThat(attributes.get("minDeliveryPriceForShop"), is("10"));
        assertThat(attributes.get("indexSpecified"), is("true"));
        assertThat(attributes.get("parcelVolume"), is("1100"));
        assertThat(attributes.get("itemsCount"), is("1"));
        assertThat(attributes.get("allPickupBucketsCount"), is("1"));
        assertThat(attributes.get("activePickupBucketsCount"), is("1"));
        assertThat(attributes.get("allPickupCarriers"), is("1,2"));
        assertThat(attributes.get("activePickupCarriers"), is("1,2"));
        assertThat(attributes.get("emptyPostOptions"), is("false"));
        assertThat(attributes.get("sellerPrice"), is("100600"));
        assertThat(attributes.get("cartSize"), is(1));
        assertThat(attributes.get("gpsPrecisionWeight"), is("0"));

        orderCreateHelper.createOrder(parameters);
        attributes = new CheckoutContextHolder.CheckoutAttributesHolder().getAttributes();
        assertThat(attributes.get("deliveryRegionId"), is(DeliveryProvider.REGION_ID));
        assertThat(attributes.get("warehouseId"), is(DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED.toString()));
        assertThat(attributes.get("minDeliveryDate"), is(Integer.toString(1)));
        assertThat(attributes.get("avgMinPickupDate"), is("1.25"));
        assertThat(attributes.get("avgMinPostDate"), is("2.25"));
        assertThat(attributes.get("parcelWeight"), is("10000"));
        assertThat(attributes.get("minDeliveryPriceForShop"), is("10"));
        assertThat(attributes.get("indexSpecified"), is("true"));
        assertThat(attributes.get("parcelVolume"), is("1100"));
        assertThat(attributes.get("itemsCount"), is("1"));
        assertThat(attributes.get("allPickupBucketsCount"), is("1"));
        assertThat(attributes.get("activePickupBucketsCount"), is("1"));
        assertThat(attributes.get("allPickupCarriers"), is("1,2"));
        assertThat(attributes.get("activePickupCarriers"), is("1,2"));
        assertThat(attributes.get("emptyPostOptions"), is("false"));
        assertThat(attributes.get("sellerPrice"), is("100600"));
        assertThat(attributes.get("cartSize"), is(1));
        assertThat(attributes.get("gpsPrecisionWeight"), is("0,0,0"));
        assertThat(attributes.get("addressSource"), is(AddressSource.NEW));
        assertThat(attributes.get("addressSources"), is("NEW"));

        parameters.getBuiltMultiCart().getCarts().forEach(c -> c.getDelivery().setBuyerAddress(
                AddressProvider.getAddressWithoutPostcode()));
        orderCreateHelper.cart(parameters);
        attributes = new CheckoutContextHolder.CheckoutAttributesHolder().getAttributes();
        assertThat(attributes.get("indexSpecified"), is("false"));
    }

    @Test
    @Disabled // TODO локально проходит, с аркануме не проходит, разобраться
    @DisplayName("В логи добавляется флаг deliverySlotMinutes и checkoutedDeliveryFeatures")
    public void shouldAddWideSlotGoAttribute() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                Collections.<String>emptySet()));
        Parameters parameters = yaLavkaHelper.buildParametersForDeferredCourier(1);
        orderCreateHelper.createOrder(parameters);

        Map<String, Object> attributes = new CheckoutContextHolder.CheckoutAttributesHolder().getAttributes();
        assertThat(attributes.get("checkoutedDeliveryFeatures"),
                is(DeliveryFeature.DEFERRED_COURIER.name()));
        assertThat(attributes.get("deliverySlotMinutes"), is("60"));
        assertThat(attributes.get("wideSlotGoAny"), is("false"));
        assertThat(attributes.get("wideSlotGoCount"), is("0"));
    }

    private ActualDeliveryOption deliveryOption(int dayFrom) {
        ActualDeliveryOption deliveryOption = new ActualDeliveryOption();
        deliveryOption.setDayFrom(dayFrom);
        deliveryOption.setDayTo(10);
        deliveryOption.setShipmentDay(0);
        deliveryOption.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        deliveryOption.setPaymentMethods(Sets.newHashSet("YANDEX"));
        deliveryOption.setPrice(BigDecimal.ZERO);
        deliveryOption.setPriceForShop(BigDecimal.valueOf(10));
        return deliveryOption;
    }

    private PickupOption pickupOption(int dayFrom, List<Long> outletIds) {
        PickupOption pickupOption = new PickupOption();
        pickupOption.setDayFrom(dayFrom);
        pickupOption.setDayTo(10);
        pickupOption.setShipmentDay(0);
        pickupOption.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        pickupOption.setPaymentMethods(Sets.newHashSet("YANDEX"));
        pickupOption.setPrice(BigDecimal.ZERO);
        pickupOption.setOutletIds(outletIds);
        return pickupOption;
    }

    private List<DeliveryOffer> generateOneDeliveryOffer() {
        DeliveryOffer deliveryOffer = new DeliveryOffer();
        deliveryOffer.setMarketSku(RandomUtils.nextLong());
        deliveryOffer.setSellerPrice(100600L);
        deliveryOffer.setCurrency(Currency.RUR);
        return Collections.singletonList(deliveryOffer);
    }

    private DeliveryTypeDistribution generateOneOrMorePickupDistribution(Long... pickupIds) {
        DeliveryTypeDistribution deliveryTypeDistribution = new DeliveryTypeDistribution();
        if (pickupIds.length > 0) {
            deliveryTypeDistribution.setPickup(Arrays.asList(pickupIds));
        } else {
            deliveryTypeDistribution.setPickup(Collections.singletonList(RandomUtils.nextLong()));
        }
        deliveryTypeDistribution.setCourier(Collections.emptyList());
        deliveryTypeDistribution.setPost(Collections.emptyList());
        return deliveryTypeDistribution;
    }
}
