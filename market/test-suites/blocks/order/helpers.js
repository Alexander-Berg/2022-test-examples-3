// eslint-disable-next-line no-restricted-imports
import _ from 'lodash';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';

export async function createOrder({status, substatus, cancellationRequest, paymentType, paymentMethod} = {}) {
    const {browser} = this;

    const orderParams = {
        status,
        cancellationRequest,
        paymentType: paymentType || 'POSTPAID',
        paymentMethod: paymentMethod || 'CASH_ON_DELIVERY',
        region: this.params.region,
        orders: [{
            items: [{skuId: checkoutItemIds.asus.skuId}],
            deliveryType: status === 'PICKUP' ? status : 'DELIVERY',
            delivery: {
                deliveryPartnerType: 'YANDEX_MARKET',
            },
            outletId: '66843529',
        }],
    };

    if (substatus) {
        orderParams.substatus = substatus;
    }

    const order = await browser.yaScenario(
        this,
        'checkoutResource.prepareOrder',
        orderParams
    );
    const orderId = _.get(order, ['orders', 0, 'id']);
    if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
        await this.browser.yaOpenPage(this.params.pageId, {orderId});
    } else {
        await this.browser.yaOpenPage(this.params.pageId);
    }
    return orderId;
}
