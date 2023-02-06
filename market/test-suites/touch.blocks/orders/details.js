// eslint-disable-next-line no-restricted-imports
import {
    flow,
    get,
    head,
} from 'lodash/fp';
import {prepareSuite, makeSuite} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import {regular as regularOrder} from '@self/root/src/spec/hermione/configs/orders';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import details from '@self/root/src/spec/hermione/test-suites/touch.blocks/order/details';

const createOrder = async ctx => {
    const collections = await ctx.browser.yaScenario(ctx, prepareOrder, regularOrder);
    const order = flow([
        get('orders'),
        head,
    ])(collections);
    return {
        orderId: order.id,
        collections: {
            orderItem: collections.orderItem,
            order: {
                [order.id]: order,
            },
        },
    };
};

/**
 * Тесты на обыкновенный заказ.
 * @param {PageObject.OrderCard} orderCard - карточка заказа (для списка и для отдельного заказа они разные),
 */
module.exports = makeSuite('Ссылка на подробности о заказе', {
    feature: 'Состав заказа',
    story: {
        async beforeEach() {
            const {orderId, collections} = await createOrder(this);
            this.params.expectedOrderId = orderId;
            this.params.expectedOrderCollections = collections;
            await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS);
        },
        'при клике': {
            async beforeEach() {
                await this.orderCard.clickDetailsLink();
            },
            'открывает экран заказа.': prepareSuite(details, {}),
        },
    },
});
