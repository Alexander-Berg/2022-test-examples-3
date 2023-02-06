package ru.yandex.market.checkout.pushapi.client.xml;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.ItemParameter;
import ru.yandex.market.checkout.checkouter.order.TaxSystem;
import ru.yandex.market.checkout.checkouter.order.UnitValue;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartItem;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.client.xml.order.AddressXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.BuyerXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.CartItemXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.ItemParameterXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.ItemPromoXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.UnitValueXmlSerializer;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.EnhancedRandomHelper;

import static ru.yandex.market.checkout.pushapi.client.xml.serialize.CartXmlSerializeUtils.toXml;

//MARKETCHECKOUT-4009 run all randomized tests several times to prevent floating failures
public class CartXmlSerializerTest {

    private static final EnhancedRandom RANDOM = EnhancedRandomHelper.createEnhancedRandom();
    private static final CheckoutDateFormat CHECKOUT_DATE_FORMAT = new CheckoutDateFormat();

    private final Delivery delivery = RANDOM.nextObject(Delivery.class,
            "parcels.route", "shipments.route", "shipment.route", "liftPrice", "liftType", "marketBranded");
    private final CartItem item1 = RANDOM.nextObject(CartItem.class);
    private final CartItem item2 = RANDOM.nextObject(CartItem.class);
    private final CartItem digitalItem = RANDOM.nextObject(CartItem.class);
    private final Buyer buyer = BuyerProvider.getSberIdBuyer();

    private CartXmlSerializer serializer;

    @BeforeEach
    public void setUp() throws Exception {
        serializer = new CartXmlSerializer();

        PromoDefinition promoDefinition = RANDOM.nextObject(PromoDefinition.class);
        ItemParameter itemParameter = RANDOM.nextObject(ItemParameter.class);
        UnitValue unitValue = RANDOM.nextObject(UnitValue.class);
        unitValue.setValues(List.of(UUID.randomUUID().toString()));
        unitValue.setShopValues(List.of(UUID.randomUUID().toString()));
        itemParameter.setUnits(List.of(unitValue));

        item1.setPromos(Set.of(ItemPromo.createWithSubsidy(promoDefinition, BigDecimal.TEN)));
        item1.setKind2Parameters(List.of(itemParameter));
        item2.setPromos(Set.of(ItemPromo.createWithSubsidy(promoDefinition, BigDecimal.TEN)));
        item2.setKind2Parameters(List.of(itemParameter));
        digitalItem.setDigital(true);

        DeliveryXmlSerializer deliveryXmlSerializer = new DeliveryXmlSerializer();
        deliveryXmlSerializer.setCheckoutDateFormat(CHECKOUT_DATE_FORMAT);
        deliveryXmlSerializer.setAddressXmlSerializer(new AddressXmlSerializer());
        deliveryXmlSerializer.setParcelXmlSerializer(new ParcelXmlSerializer());

        serializer.setDeliveryXmlSerializer(deliveryXmlSerializer);

        CartItemXmlSerializer cartItemXmlSerializer = new CartItemXmlSerializer();
        ItemParameterXmlSerializer itemParameterXmlSerializer = new ItemParameterXmlSerializer();
        itemParameterXmlSerializer.setUnitValueXmlSerializer(new UnitValueXmlSerializer());

        cartItemXmlSerializer.setItemParameterXmlSerializer(itemParameterXmlSerializer);
        cartItemXmlSerializer.setPromoXmlSerializer(new ItemPromoXmlSerializer());

        serializer.setCartItemXmlSerializer(cartItemXmlSerializer);
        serializer.setBuyerXmlSerializer(new BuyerXmlSerializer());

        ShopOutlet outlet = RANDOM.nextObject(ShopOutlet.class);
        delivery.setOutletIds(Set.of(outlet.getId()));
        delivery.setOutletCodes(Set.of(delivery.getOutletCode()));
        delivery.setOutlet(outlet);
        delivery.setOutlets(List.of(outlet));
        Parcel parcel = RANDOM.nextObject(Parcel.class, "route");
        ParcelBox parcelBox = parcel.getBoxes().get(0);
        ParcelBoxItem parcelBoxItem = parcelBox.getItems().get(0);
        parcel.setBoxes(List.of(parcelBox));
        parcelBox.setItems(List.of(parcelBoxItem));

        delivery.setShipment(parcel);
        delivery.setParcels(List.of(parcel));
        delivery.setPaymentOptions(Set.of(RANDOM.nextObject(PaymentMethod.class)));
        delivery.setPromos(Set.of(RANDOM.nextObject(ItemPromo.class)));
        delivery.setTracks(List.of(RANDOM.nextObject(Track.class)));
    }

    @RepeatedTest(10)
    public void testSerialize() throws Exception {
        Cart cart = RANDOM.nextObject(Cart.class, "isFulfilment",
                "delivery.parcels.route", "delivery.shipments.route", "delivery.shipment.route");
        cart.setDelivery(delivery);
        cart.setCurrency(Currency.RUR);
        cart.setDeliveryCurrency(null);
        cart.setItems(Arrays.asList(item1, item2));
        cart.setTaxSystem(TaxSystem.USN);
        cart.setRgb(Color.BLUE);
        cart.setPreorder(false);
        cart.setHasCertificate(false);
        cart.setCrossborder(false);
        cart.setBuyer(buyer);
        cart.setExperiments("experiments");

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                cart,
                toXml(cart)
        );
    }

    @RepeatedTest(10)
    public void testSerializeFulfilment() throws Exception {
        Cart cart = prepareCart();
        cart.setDelivery(delivery);
        cart.setCurrency(Currency.RUR);
        cart.setDeliveryCurrency(null);
        cart.setItems(Arrays.asList(item1, item2));
        cart.setTaxSystem(TaxSystem.ECHN);
        cart.setFulfilment(true);
        cart.setCrossborder(false);
        cart.setRgb(Color.BLUE);
        cart.setPreorder(false);
        cart.setHasCertificate(false);
        cart.setBuyer(buyer);
        cart.setExperiments("experiments");

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                cart,
                toXml(cart)
        );
    }

    @RepeatedTest(10)
    public void testSerializePreorder() throws Exception {
        Cart cart = prepareCart();
        cart.setDelivery(delivery);
        cart.setCurrency(Currency.RUR);
        cart.setDeliveryCurrency(null);
        cart.setItems(Arrays.asList(item1, item2));
        cart.setTaxSystem(TaxSystem.ECHN);
        cart.setRgb(Color.BLUE);
        cart.setPreorder(true);
        cart.setFulfilment(false);
        cart.setCrossborder(false);
        cart.setHasCertificate(false);
        cart.setBuyer(buyer);
        cart.setExperiments("experiments");

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                cart,
                toXml(cart)
        );
    }

    @RepeatedTest(10)
    public void testSerializeDigitalOrder() throws Exception {
        Cart cart = prepareCart();
        cart.setDelivery(delivery);
        cart.setCurrency(Currency.RUR);
        cart.setDeliveryCurrency(null);
        cart.setItems(List.of(digitalItem));
        cart.setTaxSystem(TaxSystem.OSN);
        cart.setRgb(Color.WHITE);
        cart.setFulfilment(false);
        cart.setCrossborder(false);
        cart.setHasCertificate(false);
        cart.setBuyer(buyer);
        cart.setExperiments("experiments");

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                cart,
                toXml(cart)
        );
    }

    @RepeatedTest(10)
    public void testSerializeCrossborder() throws Exception {
        Cart cart = prepareCart();
        cart.setDelivery(delivery);
        cart.setCurrency(Currency.RUR);
        cart.setDeliveryCurrency(null);
        cart.setItems(Arrays.asList(item1, item2));
        cart.setTaxSystem(TaxSystem.ECHN);
        cart.setRgb(Color.BLUE);
        cart.setPreorder(false);
        cart.setFulfilment(false);
        cart.setCrossborder(true);
        cart.setHasCertificate(false);
        cart.setBuyer(buyer);
        cart.setExperiments("experiments");

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                cart,
                toXml(cart)
        );
    }

    @RepeatedTest(10)
    public void testSerializeCertificate() throws Exception {
        Cart cart = prepareCart();
        cart.setDelivery(delivery);
        cart.setCurrency(Currency.RUR);
        cart.setDeliveryCurrency(null);
        cart.setItems(Arrays.asList(item1, item2));
        cart.setTaxSystem(TaxSystem.ECHN);
        cart.setRgb(Color.BLUE);
        cart.setPreorder(false);
        cart.setFulfilment(true);
        cart.setCrossborder(false);
        cart.setHasCertificate(true);
        cart.setBuyer(buyer);
        cart.setExperiments("experiments");

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                cart,
                toXml(cart)
        );
    }

    @RepeatedTest(10)
    public void testSerializeDeliveryCurrency() throws Exception {
        Cart cart = RANDOM.nextObject(Cart.class, "items", "delivery", "isFulfilment", "buyer", "isCrossborder",
                "delivery.parcels.route", "delivery.shipments.route", "delivery.shipment.route");
        cart.setCurrency(Currency.USD);
        cart.setDeliveryCurrency(Currency.EUR);
        cart.setTaxSystem(TaxSystem.PSN);
        cart.setRgb(Color.BLUE);
        cart.setPreorder(false);
        cart.setHasCertificate(false);
        cart.setExperiments("experiments");

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                cart,
                toXml(cart)
        );
    }

    @RepeatedTest(10)
    public void testSerializeEmpty() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new Cart(),
                "<cart />"
        );
    }

    private Cart prepareCart() {
        return RANDOM.nextObject(Cart.class,
                "delivery.parcels.route", "delivery.shipments.route", "delivery.shipment.route",
                "delivery.liftType", "delivery.liftPrice");
    }
}
