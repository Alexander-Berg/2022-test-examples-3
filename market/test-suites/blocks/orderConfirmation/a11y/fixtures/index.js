import {outlet1 as outletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/outlets';
import {PAYMENT_STATUS} from '@self/root/src/entities/payment/statuses';

const CASHBACK_AMOUNT = 300;

const FIRST_ORDER_ID = 11111;
const SECOND_ORDER_ID = 222222;

const ORDER = {
    items: [{
        skuId: 11,
        count: 1,
        buyerPrice: 5,
    }],
    recipient: 111,
    deliveryType: 'POST',
    outletId: outletMock.id,
    currency: 'RUR',
    buyerCurrency: 'RUR',
    delivery: {
        buyerPrice: 100,
        dates: {
            fromDate: '10-10-2000',
            toDate: '15-10-2000',
            fromTime: '13:00',
            toTime: '19:00',
        },
    },
    payment: {
        status: PAYMENT_STATUS.HOLD,
    },
    cashbackEmitInfo: {
        totalAmount: CASHBACK_AMOUNT,
        status: 'INIT',
    },
};

const UNPAID_ORDER = {
    items: [{
        skuId: 11,
        count: 1,
        buyerPrice: 5,
    }],
    recipient: 111,
    deliveryType: 'POST',
    outletId: outletMock.id,
    currency: 'RUR',
    buyerCurrency: 'RUR',
    delivery: {
        buyerPrice: 100,
        dates: {
            fromDate: '10-10-2000',
            toDate: '15-10-2000',
            fromTime: '13:00',
            toTime: '19:00',
        },
    },
    cashbackEmitInfo: {
        totalAmount: CASHBACK_AMOUNT,
        status: 'INIT',
    },
};

const FIRST_ORDER = {
    ...ORDER,
    orderId: FIRST_ORDER_ID,
};

const SECOND_ORDER = {
    ...ORDER,
    orderId: SECOND_ORDER_ID,
};

const UNPAID_FIRST_ORDER = {
    ...UNPAID_ORDER,
    orderId: FIRST_ORDER_ID,
};

const UNPAID_SECOND_ORDER = {
    ...UNPAID_ORDER,
    orderId: SECOND_ORDER_ID,
};

export const SOLO_ORDER = [FIRST_ORDER];
export const MULTI_ORDER = [FIRST_ORDER, SECOND_ORDER];
export const UNPAID_SOLO_ORDER = [UNPAID_FIRST_ORDER];
export const UNPAID_MULTI_ORDER = [UNPAID_FIRST_ORDER, UNPAID_SECOND_ORDER];
