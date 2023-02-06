import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import bindByLink from '@self/project/src/spec/hermione/test-suites/blocks/order/bindByLink';
import {commonParams} from '@self/project/src/spec/hermione/configs/params';
import MyOrders from '@self/root/src/widgets/content/orders/OrderList/components/View/__pageObject';
import orderLegalInfo from '@self/platform/spec/hermione/test-suites/blocks/orderLegalInfo';
import alco from '@self/platform/spec/hermione/test-suites/blocks/alco/orders';
import OrderCard from '@self/root/src/widgets/content/orders/OrderCard/components/View/__pageObject';
import feedbackOnOrderCard from '@self/root/src/spec/hermione/test-suites/blocks/orderFeedback/feedbackOnOrderCard';
import orderConsultationsOrderCard
    from '@self/root/src/spec/hermione/test-suites/blocks/orderConsultations/orderConsultationsOrderCard';
import deliveryFeedbackOrderCard from '@self/root/src/spec/hermione/test-suites/blocks/deliveryFeedback/deliveryFeedbackOrderCard';
import checkUnitsSuite from '@self/root/src/spec/hermione/test-suites/desktop.blocks/myorder/checkUnitsSuite';
import orderReceiptCode from '@self/root/src/spec/hermione/test-suites/desktop.blocks/orderReceiptCode';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Мои заказы', {
    environment: 'testing',
    params: {
        ...commonParams.description,
    },
    defaultParams: {
        ...commonParams.value,
    },
    story: {
        async beforeEach() {
            await this.setPageObjects({
                ordersLayout: () => this.createPageObject(MyOrders),
            });
        },
        'Авторизованный пользователь.': mergeSuites(
            prepareSuite(require('@self/platform/spec/hermione/test-suites/blocks/myOrders'), {
                params: {
                    isAuthWithPlugin: true,
                    pageId: PAGE_IDS_COMMON.ORDERS,
                },
            }),
            prepareSuite(require('@self/root/src/spec/hermione/test-suites/desktop.blocks/myorder'), {
                params: {
                    isAuthWithPlugin: true,
                    pageId: PAGE_IDS_COMMON.ORDERS,
                },
            }),
            prepareSuite(orderLegalInfo, {
                params: {
                    isAuthWithPlugin: true,
                },
            }),
            prepareSuite(alco, {
                params: {
                    isAuthWithPlugin: true,
                },
            }),
            prepareSuite(bindByLink),
            prepareSuite(deliveryFeedbackOrderCard, {
                params: {
                    pageId: PAGE_IDS_COMMON.ORDERS,
                    isAuthWithPlugin: true,
                },
            }),
            prepareSuite(feedbackOnOrderCard, {
                hooks: {
                    async beforeEach() {
                        await this.setPageObjects({
                            orderCard: () => this.createPageObject(OrderCard, {
                                parent: this.ordersLayout,
                            }),
                        });
                    },
                },
                params: {
                    pageId: PAGE_IDS_COMMON.ORDERS,
                    isAuthWithPlugin: true,
                },
            }),
            prepareSuite(orderConsultationsOrderCard, {
                suiteName: 'Арбитраж. Открытие чата со списка заказов.',
                meta: {
                    id: 'bluemarket-4019',
                    issue: 'MARKETFRONT-36441',
                    feature: 'Арбитраж',
                },
                hooks: {
                    async beforeEach() {
                        await this.setPageObjects({
                            orderCard: () => this.createPageObject(OrderCard, {
                                parent: this.ordersLayout,
                            }),
                        });
                    },
                },
                params: {
                    pageId: PAGE_IDS_COMMON.ORDERS,
                    isAuthWithPlugin: true,
                },
            }),
            prepareSuite(checkUnitsSuite, {
                meta: {
                    id: 'marketfront-5573',
                    issue: 'MARKETFRONT-78854',
                },
                params: {
                    pageId: PAGE_IDS_COMMON.ORDERS,
                    isAuthWithPlugin: true,
                },
            }),
            prepareSuite(orderReceiptCode, {
                params: {
                    pageId: PAGE_IDS_COMMON.ORDERS,
                },
            })
        ),
    },
});
