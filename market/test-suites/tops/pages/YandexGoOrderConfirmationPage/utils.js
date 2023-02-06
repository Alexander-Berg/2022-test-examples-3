import {isNil} from 'ambar';
import {prepareThankPage} from '@self/root/src/spec/hermione/scenarios/thank';
import {PAGE_IDS_YANDEX_GO} from '@self/root/src/constants/pageIds';
import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';
import {PAYMENT_TYPE} from '@self/root/src/entities/payment';
import {PAYMENT_METHODS} from '@self/root/src/entities/payment/constants';
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {PROMO_TYPES} from '@self/root/src/entities/promo';

export function prepareUnpaidOrderConfirmationPage({
    orderId,
    items,
    buyerTotal,
    cashbackAmount,
    buyerItemsDiscount,
}) {
    return this.browser.yaScenario(this, prepareThankPage, {
        pageId: PAGE_IDS_YANDEX_GO.ORDER_CONFIRMATION,
        orders: [{
            orderId,
            items,
            deliveryType: DELIVERY_TYPES.DELIVERY,
            buyerTotal,
            cashbackEmitInfo: !isNil(cashbackAmount) ? {
                totalAmount: cashbackAmount,
                status: 'INIT',
            } : undefined,
            promos: !isNil(buyerItemsDiscount) ? [
                {
                    key: '1111',
                    type: PROMO_TYPES.MARKET_BLUE,
                    // Хотя по типам здесь должен быть объект Price,
                    // фактически по коду ожидается число
                    buyerItemsDiscount: buyerItemsDiscount,
                },
            ] : undefined,
        }],
        paymentOptions: {
            paymentType: PAYMENT_TYPE.PREPAID,
            paymentMethod: PAYMENT_METHODS.YANDEX,
            status: ORDER_STATUS.UNPAID,
        },
    });
}
