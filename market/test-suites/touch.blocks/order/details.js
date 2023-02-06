// eslint-disable-next-line no-restricted-imports
import {
    flow, get, head,
} from 'lodash/fp';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {regular as regularOrder} from '@self/root/src/spec/hermione/configs/orders';
import {OrderItems} from '@self/root/src/containers/OrderDetails/OrderItems/__pageObject';
import OrderCard from '@self/root/src/widgets/content/orders/OrderCard/components/View/__pageObject';

import estimated from '@self/root/src/spec/hermione/test-suites/blocks/order/estimated';

const createOrder = async ctx => {
    const collections = await ctx.browser.yaScenario(ctx, 'checkoutResource.prepareOrder', regularOrder);
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
 * @param {Order} expectedOrderId - Заказ, состав которого должен быть отображён.
 * @param {Order} expectedOrderCollections - Заказ, состав которого должен быть отображён.
 */
module.exports = makeSuite('Подробности о заказе.', {
    params: {
        expectedOrderId: 'ID заказа, состав которого должен быть отображён',
        expectedOrderCollections: 'Коллекции заказа, состав которого должен быть отображён',
        isAuth: 'Авторизован ли пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                if (!this.params.expectedOrderId) {
                    const {orderId, collections} = await createOrder(this);
                    this.params.expectedOrderId = orderId;
                    this.params.expectedOrderCollections = collections;
                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDER, {orderId});
                }
            },
        },
        // eslint-disable-next-line global-require
        prepareSuite(require('@self/root/src/spec/hermione/test-suites/touch.blocks/order/items'), {
            pageObjects: {
                orderItems() {
                    return this.createPageObject(OrderItems);
                },
            },
        }),
        prepareSuite(estimated, {
            params: {
                isAuth: true,
            },
            pageObjects: {
                myOrder() {
                    return this.createPageObject(
                        OrderCard,
                        {
                            root: `${OrderCard.root}:nth-child(1)`,
                        }
                    );
                },
            },
        })
    ),
});
