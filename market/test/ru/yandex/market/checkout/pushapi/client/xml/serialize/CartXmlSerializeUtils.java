package ru.yandex.market.checkout.pushapi.client.xml.serialize;

import javax.annotation.Nonnull;

import ru.yandex.common.util.StringUtils;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.order.ItemParameter;
import ru.yandex.market.checkout.checkouter.order.UnitValue;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartItem;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;

import static ru.yandex.market.checkout.common.util.ChainCalls.safeNull;
import static ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat.HOUR_MINUTE_FORMATTER;
import static ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat.LONG_DATE_FORMATTER;
import static ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat.SHORT_DATE_FORMATTER;

public final class CartXmlSerializeUtils {

    private static final CheckoutDateFormat CHECKOUT_DATE_FORMAT = new CheckoutDateFormat();

    private CartXmlSerializeUtils() {
    }

    public static String toXml(@Nonnull Cart cart) {
        final StringBuilder sb = new StringBuilder("<cart ");

        if (cart.getCurrency() != null) {
            sb.append("currency='" + cart.getCurrency().name() + "' ");
        }

        if (cart.getDeliveryCurrency() != null) {
            sb.append("delivery-currency='" + cart.getDeliveryCurrency().name() + "' ");
        }

        if (Boolean.TRUE.equals(cart.isFulfilment())) {
            sb.append("fulfilment='" + cart.isFulfilment() + "' ");
        }

        if (Boolean.TRUE.equals(cart.isCrossborder())) {
            sb.append("crossborder='" + cart.isCrossborder() + "' ");
        }

        if (cart.getTaxSystem() != null) {
            sb.append("tax-system='" + cart.getTaxSystem() + "' ");
        }

        if (cart.getRgb() != null) {
            sb.append("rgb='" + cart.getRgb().name() + "' ");
        }

        if (cart.isPreorder()) {
            sb.append("preorder='" + cart.isPreorder() + "' ");
        }

        if (cart.hasCertificate()) {
            sb.append("hasCertificate='" + cart.hasCertificate() + "' ");
        }

        if (cart.getExperiments() != null) {
            sb.append("experiments='" + cart.getExperiments() + "' ");
        }

        sb.append("> ");

        if (cart.getDelivery() != null) {
            sb.append(toXml(cart.getDelivery()));
        }

        if (cart.getBuyer() != null) {
            sb.append("<buyer id='' " +
                    "   first-name='" + toString(cart.getBuyer().getFirstName()) + "' " +
                    "   last-name='" + toString(cart.getBuyer().getLastName()) + "' " +
                    "   middle-name='" + toString(cart.getBuyer().getMiddleName()) + "' " +
                    "   phone='" + toString(cart.getBuyer().getPhone()) + "' " +
                    "   email='" + toString(cart.getBuyer().getEmail()) + "' " +
                    "   personal-email-id='" + toString(cart.getBuyer().getPersonalEmailId()) + "' " +
                    "   personal-full-name-id='" + toString(cart.getBuyer().getPersonalFullNameId()) + "' " +
                    "   personal-phone-id='" + toString(cart.getBuyer().getPersonalPhoneId()) + "' " +
                    "   uid='" + toString(cart.getBuyer().getUid()) + "' />");
        }

        if (cart.getItems() != null) {
            sb.append("<items>");
            for (CartItem item : cart.getItems()) {
                sb.append(toXml(item));
            }
            sb.append("</items>");
        }

        return sb.append("</cart>").toString();
    }

    public static String toXml(@Nonnull Delivery delivery) {
        final PaymentMethod paymentMethod = delivery.getPaymentOptions().iterator().next();
        final StringBuilder sb = new StringBuilder("<delivery type='" + delivery.getType() + "' " +
                "price='" + delivery.getPrice() + "' " +
                "vat='" + delivery.getVat().name() + "' " +
                "region-id='" + delivery.getRegionId() + "' " +
                "service-name='" + delivery.getServiceName() + "' " +
                "id='" + delivery.getId() + "' " +
                "shop-delivery-id='" + delivery.getShopDeliveryId() + "' " +
                "hash='" + delivery.getHash() + "' " +
                "delivery-option-id='" + delivery.getDeliveryOptionId() + "' " +
                "delivery-service-id='" + delivery.getDeliveryServiceId() + "' " +
                "delivery-partner-type='" + delivery.getDeliveryPartnerType() + "' >" +
                "<dates from-date='" +
                CHECKOUT_DATE_FORMAT.formatShort(delivery.getDeliveryDates().getFromDate()) +
                "' to-date='" +
                CHECKOUT_DATE_FORMAT.formatShort(delivery.getDeliveryDates().getToDate()) +
                "' from-time='" +
                delivery.getDeliveryDates().getFromTimeString() +
                "' to-time='" +
                delivery.getDeliveryDates().getToTimeString() + "' />" +
                "<validated-dates to-date='" +
                CHECKOUT_DATE_FORMAT.formatShort(delivery.getValidatedDeliveryDates().getToDate()) +
                "' from-date='" +
                CHECKOUT_DATE_FORMAT.formatShort(delivery.getValidatedDeliveryDates().getFromDate()) +
                "' />");

        if (delivery.getShopAddress() != null) {
            sb.append(toXml(delivery.getShopAddress()));
        }

        if (delivery.getOutletIdsSet().isEmpty() && delivery.getOutletCodes().isEmpty()) {
            sb.append("<outlets>" +
                    "    <outlet id='" + delivery.getOutletId() + "' />" +
                    "    <outlet code='" + delivery.getOutletCode() + "' />" +
                    "</outlets>");
        } else {
            sb.append("<outlets>");
            for (Long outletId : delivery.getOutletIdsSet()) {
                sb.append("    <outlet id='" + outletId + "' />");
            }
            for (String code : delivery.getOutletCodes()) {
                sb.append("    <outlet code='" + code + "' />");
            }
            sb.append("</outlets>");
        }

        sb.append("<payment-methods>" +
                "    <payment-method>" + paymentMethod + "</payment-method>" +
                "</payment-methods>");

        sb.append(toXml(delivery.getShipment()));

        if (!delivery.getParcels().isEmpty()) {
            sb.append("<shipments>");
            for (Parcel parcel : delivery.getParcels()) {
                sb.append(toXml(parcel));
            }
            sb.append("</shipments>");
        }

        return sb.append("</delivery>").toString();
    }

    public static String toXml(@Nonnull Parcel parcel) {
        final StringBuilder sb = new StringBuilder("<shipment id='" + parcel.getId() + "' " +
                "shipmentDate='" + parcel.getShipmentDate().format(SHORT_DATE_FORMATTER) + "' " +
                "shipmentTime='" + parcel.getShipmentTime().format(HOUR_MINUTE_FORMATTER) + "' " +
                "weight='" + parcel.getWeight() + "' " +
                "width='" + parcel.getWidth() + "' " +
                "height='" + parcel.getHeight() + "' " +
                "depth='" + parcel.getDepth() + "' " +
                "status='" + parcel.getStatus().ordinal() + "' >" +
                "<boxes>");
        for (ParcelBox parcelBox : parcel.getBoxes()) {
            sb.append("<box id='" + parcelBox.getId() + "' " +
                    "weight='" + parcelBox.getWeight() + "' " +
                    "width='" + parcelBox.getWidth() + "' " +
                    "height='" + parcelBox.getHeight() + "' " +
                    "depth='" + parcelBox.getDepth() + "' >" +
                    "<items>");

            for (ParcelBoxItem parcelBoxItem : parcelBox.getItems()) {
                sb.append("<item id='" + parcelBoxItem.getItemId() +
                        "' count='" + parcelBoxItem.getCount() + "' />");
            }
            sb.append("</items></box>");
        }
        sb.append("</boxes><items>");
        for (ParcelItem parcelItem : parcel.getParcelItems()) {
            String shipmentDateTimeBySupplier = safeNull(
                    parcelItem.getShipmentDateTimeBySupplier(), value -> value.format(LONG_DATE_FORMATTER));
            sb.append("<item itemId='" + parcelItem.getItemId() + "' " +
                    "shipmentDateTimeBySupplier='" + shipmentDateTimeBySupplier + "' />");
        }
        sb.append("</items>");
        return sb.append("</shipment>").toString();
    }

    public static String toXml(@Nonnull Address address) {
        return "<" + address.getType().getXmlFieldName() +
                "   street='" + toString(address.getStreet()) + "' " +
                "   country='" + toString(address.getCountry()) + "' " +
                "   postcode='" + toString(address.getPostcode()) + "' " +
                "   city='" + toString(address.getCity()) + "' " +
                "   district='" + toString(address.getDistrict()) + "' " +
                "   subway='" + toString(address.getSubway()) + "' " +
                "   house='" + toString(address.getHouse()) + "' " +
                "   block='" + toString(address.getBlock()) + "' " +
                "   entrance='" + toString(address.getEntrance()) + "' " +
                "   entryphone='" + toString(address.getEntryPhone()) + "' " +
                "   floor='" + toString(address.getFloor()) + "' " +
                "   apartment='" + toString(address.getApartment()) + "' " +
                "   recipient='" + toString(address.getRecipient()) + "' " +
                "   recipientFirstName='" + toString(address
                .getRecipientPerson().getFirstName()) + "' " +
                "   recipientMiddleName='" + toString(address
                .getRecipientPerson().getMiddleName()) + "' " +
                "   recipientLastName='" + toString(address
                .getRecipientPerson().getLastName()) + "' " +
                "   phone='" + toString(address.getPhone()) + "' " +
                "   recipientEmail='" + toString(address.getRecipientEmail()) + "' " +
                "   gps='" + address.getGps() + "' " +
                "   personalAddressId='" + address.getPersonalAddressId() + "' " +
                "   personalEmailId='" + address.getPersonalEmailId() + "' " +
                "   personalFullNameId='" + address.getPersonalFullNameId() + "' " +
                "   personalGpsId='" + address.getPersonalGpsId() + "' " +
                "   personalPhoneId='" + address.getPersonalPhoneId() + "' " +
                "/>";
    }

    public static String toXml(@Nonnull ItemParameter itemParameter) {
        final StringBuilder sb = new StringBuilder("<item-parameter type='" + itemParameter.getType() + "' " +
                "sub-type='" + itemParameter.getSubType() + "' " +
                "name='" + itemParameter.getName() + "' " +
                "value='" + itemParameter.getValue() + "' " +
                "unit='" + itemParameter.getUnit() + "' " +
                "code='" + itemParameter.getCode() + "' >" +
                "<units>");
        for (UnitValue unitValue : itemParameter.getUnits()) {
            sb.append("<unit-value values='" +
                    StringUtils.join(unitValue.getValues(), ",") + "' " +
                    "shop-values='" +
                    StringUtils.join(unitValue.getShopValues(), ",") + "' " +
                    "unit-id='" + unitValue.getUnitId() + "' " +
                    "default-unit='" + unitValue.isDefaultUnit() + "' />");
        }
        return sb.append("</units></item-parameter>").toString();
    }

    public static String toXml(@Nonnull ItemPromo itemPromo) {
        final StringBuilder sb = new StringBuilder("<promo type='" + itemPromo.getType() + "' " +
                " subsidy='" + itemPromo.getSubsidy() + "' ");
        if (itemPromo.getBuyerDiscount() != null) {
            sb.append(" discount='" + itemPromo.getBuyerDiscount() + "' ");
        }
        if (itemPromo.getPromoDefinition().getShopPromoId() != null) {
            sb.append(" shop-promo-id='" + itemPromo.getPromoDefinition().getShopPromoId() + "' ");
        }
        if (itemPromo.getPromoDefinition().getMarketPromoId() != null) {
            sb.append(" market-promo-id='" + itemPromo.getPromoDefinition().getMarketPromoId() + "' ");
        }
        return sb.append("/>").toString();
    }

    public static String toXml(@Nonnull CartItem cartItem) {
        final StringBuilder sb = new StringBuilder("<item id='" + cartItem.getId() + "' " +
                "feed-id='" + cartItem.getFeedId() + "' " +
                "offer-id='" + cartItem.getOfferId() + "' " +
                "bundle-id='" + cartItem.getBundleId() + "' " +
                "category-id='" + cartItem.getCategoryId() + "' " +
                "feed-category-id='" + cartItem.getFeedCategoryId() + "' " +
                "offer-name='" + cartItem.getOfferName() + "' " +
                "price='" + cartItem.getPrice() + "' ");

        if (cartItem.getSupplierCurrency() != null) {
            sb.append("currency='" + cartItem.getSupplierCurrency().name() + "' ");
        }

        if (cartItem.getBuyerPrice() != null) {
            sb.append("buyer-price='" + cartItem.getBuyerPrice() + "' ");
        }

        sb.append("subsidy='" + cartItem.getSubsidy() + "' " +
                "vat='" + cartItem.getVat().ordinal() + "' " +
                "delivery='" + cartItem.getDelivery() + "' " +
                "count='" + cartItem.getCount() + "' " +
                "digital='" + cartItem.isDigital() + "' "
        );

        if (cartItem.getSupplierId() != null && cartItem.getSupplierId() > 0) {
            sb.append("fulfilment-shop-id='" + cartItem.getSupplierId() + "' " +
                    "sku='" + cartItem.getSku() + "' " +
                    "shop-sku='" + cartItem.getShopSku() + "' " +
                    "warehouse-id='" + cartItem.getWarehouseId() + "' ");
        }

        if (cartItem.getExternalFeedId() != null) {
            sb.append("external-feed-id='" + cartItem.getExternalFeedId() + "' ");
        }
        sb.append(">");

        if (!cartItem.getPromos().isEmpty()) {
            sb.append("<promos>");
            for (ItemPromo promo : cartItem.getPromos()) {
                sb.append(toXml(promo));
            }
            sb.append("</promos>");
        }

        if (cartItem.getQuantityLimits() != null) {
            sb.append("<quantity-limits min='" + cartItem.getQuantityLimits().getMinimum() +
                    "' step='" + cartItem.getQuantityLimits().getStep() + "' />");
        }

        if (!cartItem.getKind2Parameters().isEmpty()) {
            sb.append("<kind2Parameters>");
            for (ItemParameter parameter : cartItem.getKind2Parameters()) {
                sb.append(toXml(parameter));
            }
            sb.append("</kind2Parameters>");
        }
        return sb.append("</item>").toString();
    }

    private static String toString(Object value) {
        return value == null ? "" : value.toString();
    }
}
