package dto.responses.lgw;

import dto.responses.lgw.message.cancel_order.CancelOrderResponse;
import dto.responses.lgw.message.create_order.CreateOrderResponse;
import dto.responses.lgw.message.create_order.CreateOrderSuccess;
import dto.responses.lgw.message.create_order.FFCreateOrderSuccess;
import dto.responses.lgw.message.get_courier.GetCourierResponse;
import dto.responses.lgw.message.put_outbound.PutOutboundResponse;
import dto.responses.lgw.message.transfer_code.TransferCodeResponse;
import dto.responses.lgw.message.transfer_code.UpdateTransferCodesSuccess;
import dto.responses.lgw.message.update_courier.UpdateCourierResponse;
import dto.responses.lgw.message.update_items_instances.UpdateItemsInstancesResponse;
import dto.responses.lgw.message.update_order.UpdateOrderResponse;

public enum LgwTaskFlow {
    FF_PUT_OUTBOUND("ff-put-outbound", PutOutboundResponse.class),
    FF_CREATE_ORDER("ff-create-order", CreateOrderResponse.class),
    DS_CREATE_ORDER("ds-create-order", CreateOrderResponse.class),
    DS_UPDATE_TRANSFER_CODES("ds-update-order-transfer-codes", TransferCodeResponse.class),
    DS_CREATE_ORDER_SUCCESS("ds-create-order-success", CreateOrderSuccess.class),
    FF_CREATE_ORDER_SUCCESS("ff-create-order-success", FFCreateOrderSuccess.class),
    FF_CANCEL_ORDER_SUCCESS("ff-cancel-order-success", Object.class),
    DS_CANCEL_ORDER_SUCCESS("ds-cancel-order-success", Object.class),
    DS_UPDATE_TRANSFER_CODES_SUCCESS("ds-update-order-transfer-codes-success", UpdateTransferCodesSuccess.class),
    DS_UPDATE_ITEMS_INSTANCES("ds-update-items-instances", UpdateItemsInstancesResponse.class),
    DS_GET_COURIER_SUCCESS("ds-get-courier-success", GetCourierResponse.class),
    FF_UPDATE_COURIER("ff-update-courier", UpdateCourierResponse.class),
    DS_CANCEL_ORDER("ds-cancel-order", CancelOrderResponse.class),
    DS_UPDATE_ORDER("ds-update-order", UpdateOrderResponse.class),
    DS_UPDATE_ORDER_DELIVERY_DATE_SUCCESS("ds-update-order-delivery-date-success", Object.class),
    DS_UPDATE_ORDER_DELIVERY_DATE_ERROR("ds-update-order-delivery-date-error", Object.class),
    DS_GET_ORDERS_DELIVERY_DATE("ds-get-orders-delivery-date", Object.class);

    private final String flow;
    private final Class<?> clazz;

    LgwTaskFlow(String flow, Class<?> clazz) {
        this.flow = flow;
        this.clazz = clazz;
    }

    public String getFlow() {
        return flow;
    }

    public Class<?> getClazz() {
        return clazz;
    }
}
