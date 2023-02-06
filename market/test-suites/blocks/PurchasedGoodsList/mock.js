import {ORDER_STATUS} from '@self/root/src/entities/order';
import {PAYMENT_TYPE} from '@self/root/src/entities/payment';
import {DELIVERY_PARTNERS} from '@self/root/src/constants/delivery';
import offerKettle from '@self/root/src/spec/hermione/kadavr-mock/report/offer/kettle';
import productKettle from '@self/root/src/spec/hermione/kadavr-mock/report/product/kettle';

export const ORDER_ID = 10;
export const ORDER = {
    id: ORDER_ID,
    items: [
        {
            id: 100,
            modelId: productKettle.id,
        },
        {
            id: 200,
            wareMd5: offerKettle.wareId,
        },
    ],
    status: ORDER_STATUS.DELIVERED,
    paymentType: PAYMENT_TYPE.PREPAID,
    delivery: {deliveryPartnerType: DELIVERY_PARTNERS.YANDEX_MARKET},
};

