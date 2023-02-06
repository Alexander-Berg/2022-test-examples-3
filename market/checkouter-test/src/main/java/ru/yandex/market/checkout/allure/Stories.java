package ru.yandex.market.checkout.allure;

/**
 * "Ручка" в testpalm.
 */
public abstract class Stories {
    public static final String CART = "/cart";
    public static final String CHECKOUT = "/checkout";
    public static final String MOVE_ORDERS = "/move-orders";
    public static final String DELIVERY = "/delivery";
    public static final String STATUS = "/status";
    public static final String ORDERS_ORDER_ID = "/orders/{orderId}";
    public static final String ORDERS = "/orders";
    public static final String POST_GET_ORDERS = "POST /get-orders";
    public static final String EVENTS = "/events";
    public static final String PAYMENT = "/payment";
    public static final String REFUND = "/refund";
    public static final String REFUND_RETRY = "/refund/{id}/retry";
    public static final String RECEIPTS = "/receipts";
    public static final String PAYMENTS = "/payments";
    public static final String PAYMENTS_NOTIFY_REFUNDS = "/refunds/notify";
    public static final String REFUNDS = "/refunds";
    public static final String GET_SHOPS = "GET /shops";
    public static final String GET_ORDERS_BY_SHOPSHIPMENT = "/shops/{shopId}/shipments/{shipmentId}/orders";
    public static final String POST_SHOPS = "POST /shops";
    public static final String EVENTS_READ_BY_USER = "/orders/{id}/events/{id}/read-by-user";
    public static final String SHOP_SHIPMENTS = "POST /shop-shipments";
    public static final String ORDERS_ITEMS = "PUT /orders/{id}/items";
    public static final String PUT_SHOP_SHIPMENTS = "PUT /shop-shipments";
    public static final String PUT_SHOP_SHIPMENTS_ID = "PUT /shop-shipments/{id}";
    public static final String POST_GET_SHOP_SHIPMENTS = "POST /get-shop-shipments";
    public static final String POST_ORDERS_DELIVERY_PARCEL_SHIPMENT = "POST /orders/{id}/delivery/parcel/shipment";
    public static final String ORDERS_BUYER_BEEN_CALLED = "/orders/{id}/buyer/been-called";
    public static final String CHECKPOINTS_COUNT = "/checkpoints/count";
    public static final String ORDERS_EVENTS = "/orders/events";
    public static final String ORDERS_STATUS = "/orders/{id}/status";
    public static final String ACTUALIZE = "/actualize";
    public static final String ORDERS_DELIVERY = "/orders/{id}/delivery";
    public static final String ORDERS_DELIVERY_POSSIBLE_UPDATES = "/orders/{id}/delivery/possible-updates";
    public static final String ORDERS_PAYMENT = "/orders/{id}/payment";
    public static final String ORDERS_PAYMENTS = "/orders/{id}/payments";
    public static final String NOTIFY_TRACKS = "/notify-tracks";
    public static final String ORDER_BY_BINDKEY = "/orders/by-bind-key";
    public static final String BUSINESS_RULES = "Бизнес-правила";
    public static final String POST_RETURN = "POST /orders/{order-id}/return";
    public static final String GET_RETURN = "GET /return/{return-id}";
    public static final String RETURN_BY_ORDER_AND_ID = "GET /orders/{order-id}/returns/{return-id}";
    public static final String RETURNS_BY_ORDER = "GET /orders/{order-id}/returns";
    public static final String RETURN_OPTIONS = "POST /orders/{order-id}/returns/options";
    public static final String RETURN_CREATE = "POST /orders/{order-id}/returns/create";
    public static final String RETURN_RESUME = "POST /orders/{order-id}/returns/{return-id}/resume";
    public static final String RETURN_ADD_DELIVERY = "POST /orders/{order-id}/returns/{return-id}/delivery";
    public static final String GET_RETURNS_ITEMS = "POST /orders/{order-id}/returns/items";
    public static final String PUT_TRACK_CODE_TO_RETURN = "PUT /orders/{orderId}/returns/{returnId}/track-code";
    public static final String UPDATE_BANK_DETAILS = "POST /orders/{orderId}/returns/{returnId}/bank-details";
    public static final String GET_BANK_DETAILS_BY_BIK = "GET /bank-details";
    public static final String GET_RETURNS_HISTORY = "GET /returns/history";
    public static final String SET_OW_TICKET = "PUT /orders/{orderId}/returns/{returnId}/owTicket/{owTicketId}";
    // Таски
    public static final String RECEIPT_INSPECTOR = "ReceiptInspector";
    public static final String MULTICART_ACTUALIZE = "/v2/multicart/actualize";
}

