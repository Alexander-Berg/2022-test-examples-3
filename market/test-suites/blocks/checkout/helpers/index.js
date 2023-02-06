import {ORDER_STATUS} from '@self/root/src/entities/order';
import {region} from '@self/root/src/spec/hermione/configs/geo';
import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';
import {PAYMENT_METHOD, PAYMENT_TYPE} from '@self/root/src/entities/payment';

export function makeDeliveredOrder() {
    let orderIndex = 0;
    return async address => {
        await this.browser.setState(`persAddress.address.${address.id}`, address);
        await this.browser.setState(`Checkouter.collections.order.${orderIndex++}`, {
            id: orderIndex,
            status: ORDER_STATUS.DELIVERED,
            region: region['Москва'],
            delivery: {
                regionId: address.regionId,
                buyerAddress: address,
                type: DELIVERY_TYPES.DELIVERY,
            },
            paymentType: PAYMENT_TYPE.PREPAID,
            paymentMethod: PAYMENT_METHOD.YANDEX,
        });
    };
}
