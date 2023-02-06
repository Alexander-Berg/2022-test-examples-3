package steps.orderSteps.itemSteps;

import java.math.BigDecimal;
import java.util.ArrayList;

import ru.yandex.market.checkout.checkouter.order.ItemAvailability;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.common.report.model.QuantityLimits;

public class ItemsSteps {
    //OfferItem fields
    private static final long ORDER_ITEM_ID = 1;
    private static final long FEED_ID = 123L;
    private static final String WARE_MD5 = "md5";
    private static final String FEED_CATEGORY_ID = "123";
    private static final int CATEGORY_ID = 123;
    private static final String OFFER_NAME = "offer name";
    private static final BigDecimal PRICE = BigDecimal.valueOf(123.0);
    private static final int COUNT = 2;
    private static final boolean DELIVERY = true;
    private static final String OFFER_ID = "123";
    private static final ItemAvailability AVAILABILITY = ItemAvailability.ON_STOCK;

    //OrderItem fields
    private static final long MODEL_ID = 123L;
    private static final String DESCRIPTION = "description";
    private static final BigDecimal BUYER_PRICE = BigDecimal.valueOf(123.0);
    private static final BigDecimal FEE = BigDecimal.valueOf(123.0);
    private static final BigDecimal FEE_SUM = BigDecimal.valueOf(345);
    private static final String SHOW_INFO = "shop info";
    private static final int FEE_INT = 123;
    private static final String REAL_SHOW_UID = "123";
    private static final String SHOW_UID = "123";

    private static final boolean POST_AVAILABLE = true;
    private static final String SHOP_URL = "shop_url";
    private static final boolean GLOBAL = true;

    private ItemsSteps() {
    }

    public static ArrayList<OrderItem> getOrderItemsList() {
        ArrayList<OrderItem> orderItemsList = new ArrayList<>();

        orderItemsList.add(getOrderItem(ORDER_ITEM_ID));
        return orderItemsList;
    }

    public static OrderItem getOrderItem() {
        return getOrderItem(ORDER_ITEM_ID);
    }

    public static OrderItem getOrderItem(Long id) {
        OrderItem orderItem = new OrderItem();

        orderItem.setId(id);
        orderItem.setFeedId(FEED_ID);
        orderItem.setWareMd5(WARE_MD5);
        orderItem.setCategoryId(CATEGORY_ID);
        orderItem.setFeedCategoryId(FEED_CATEGORY_ID);
        orderItem.setOfferName(OFFER_NAME);
        orderItem.setPrice(PRICE);
        orderItem.setCount(COUNT);
        orderItem.setDelivery(DELIVERY);
        orderItem.setOfferId(OFFER_ID);

        orderItem.setModelId(MODEL_ID);
        orderItem.setDescription(DESCRIPTION);
        orderItem.setBuyerPrice(BUYER_PRICE);
        orderItem.setFee(FEE);
        orderItem.setFeeSum(FEE_SUM);
        orderItem.setShowInfo(SHOW_INFO);
        orderItem.setFeeInt(FEE_INT);
        orderItem.setChanges(ChangesSteps.getChangeSet());
        orderItem.setRealShowUid(REAL_SHOW_UID);
        orderItem.setShowUid(SHOW_UID);
        orderItem.setKind2Parameters(Kind2ParameterSteps.getKind2ParametersList());

        orderItem.setGlobal(GLOBAL);
        orderItem.setQuantityLimits(QuantityLimitsSteps.getQuantityLimits());
        orderItem.setPictureURL(PictureSteps.getUrl());
        orderItem.setPostAvailable(POST_AVAILABLE);
        orderItem.setPictures(PictureSteps.getPictures());
        orderItem.setShopUrl(SHOP_URL);

        orderItem.setShopSku("1234");
        orderItem.setSupplierId(1L);
        orderItem.setWarehouseId(1);
        orderItem.setFulfilmentWarehouseId(1L);

        return orderItem;
    }

    private static class QuantityLimitsSteps {
        private static final int STEP = 1;
        private static final int MINIMUM = 1;

        static QuantityLimits getQuantityLimits() {
            QuantityLimits quantityLimits = new QuantityLimits();

            quantityLimits.setStep(STEP);
            quantityLimits.setMinimum(MINIMUM);

            return quantityLimits;
        }
    }
}
