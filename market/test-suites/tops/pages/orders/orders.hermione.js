import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {commonParams} from '@self/root/src/spec/hermione/configs/params';

import OrderCard from '@self/root/src/widgets/content/orders/OrderCard/components/View/__pageObject';
import {ActionButton} from '@self/root/src/components/OrderActions/Actions/ActionButton/__pageObject';
import {OrderStatus} from '@self/root/src/components/OrderHeader/OrderStatus/__pageObject';
import {OrderItems} from '@self/root/src/containers/OrderDetails/OrderItems/__pageObject';
import OrderItem from '@self/root/src/components/OrderItem/__pageObject';

import removedOrderItems from '@self/root/src/spec/hermione/test-suites/blocks/order/removedOrderItems';
import orderStatus from '@self/root/src/spec/hermione/test-suites/touch.blocks/myorder/orderStatus';
import unpaidOrderPaymentProcess from '@self/root/src/spec/hermione/test-suites/touch.blocks/unpaidOrder/paymentProcess';
import unpaidOrderCard from '@self/root/src/spec/hermione/test-suites/touch.blocks/unpaidOrder/orderCard';
import cancel from '@self/root/src/spec/hermione/test-suites/touch.blocks/orders/cancel';
import refund from '@self/root/src/spec/hermione/test-suites/touch.blocks/orders/refund';
import paymentButtonExisting from '@self/root/src/spec/hermione/test-suites/touch.blocks/myorder/paymentButtonExisting';
import returnInfo from '@self/root/src/spec/hermione/test-suites/touch.blocks/order/returnInfo';
import checkUnitsSuite from '@self/root/src/spec/hermione/test-suites/touch.blocks/orders/checkUnitsSuite';
import orderLegalInfo from '@self/root/src/spec/hermione/test-suites/touch.blocks/orders/orderLegalInfo';
import alco from '@self/root/src/spec/hermione/test-suites/touch.blocks/alco/orders';
import list from '@self/root/src/spec/hermione/test-suites/touch.blocks/orders/list';
import details from '@self/root/src/spec/hermione/test-suites/touch.blocks/orders/details';
import preorder from '@self/root/src/spec/hermione/test-suites/touch.blocks/orders/preorder';
import deliveryFeedbackOrderCard from '@self/root/src/spec/hermione/test-suites/blocks/deliveryFeedback/deliveryFeedbackOrderCard';
import orderConsultationsOrderCard
    from '@self/root/src/spec/hermione/test-suites/blocks/orderConsultations/orderConsultationsOrderCard';
import dropship from '@self/root/src/spec/hermione/test-suites/touch.blocks/orders/dropship';
import tracking from '@self/root/src/spec/hermione/test-suites/touch.blocks/orders/tracking';
import bindByLink from '@self/project/src/spec/hermione/test-suites/blocks/order/bindByLink';
import feedbackOnOrderCard from '@self/root/src/spec/hermione/test-suites/blocks/orderFeedback/feedbackOnOrderCard';
import dsbs from '@self/root/src/spec/hermione/test-suites/blocks/order/dsbs';
import estimated from '@self/root/src/spec/hermione/test-suites/blocks/order/estimated';
import deliveryReschedulePopupSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/deliveryReschedulePopup';
import verifyDeliveryRescheduleSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/verifyDeliveryReschedule';
import verifyDeliveryLastMileRescheduleSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/verifyDeliveryLastMileReschedule';
import verifiedCancellationPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/verifiedCancellation';
import cancellationRejectionPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/cancellationRejection';
import removedItemsVerificationPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/removedItemsVerification';
import removedOrderItemsPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/removedOrderItems';
import ordersDeliveryVerificationCode
    from '@self/root/src/spec/hermione/test-suites/touch.blocks/order/deliveryVerificationCode';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Мои заказы', {
    environment: 'testing',
    params: {
        ...commonParams.description,
    },
    defaultParams: {
        ...commonParams.value,
        pageId: PAGE_IDS_COMMON.ORDERS,
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    orderCard: () => this.createPageObject(
                        OrderCard,
                        {
                            root: `${OrderCard.root}:nth-child(1)`,
                        }
                    ),
                    orderStatus: () => this.createPageObject(OrderStatus, {
                        parent: this.orderCard,
                    }),
                });
            },
        },
        {
            'Авторизованный пользователь.': mergeSuites(
                prepareSuite(list, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(cancel, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(details, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(preorder, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(deliveryFeedbackOrderCard, {
                    params: {
                        isAuthWithPlugin: true,
                        pageId: PAGE_IDS_COMMON.ORDERS,
                    },
                }),
                prepareSuite(orderConsultationsOrderCard, {
                    suiteName: 'Арбитраж. Открытие чата со списка заказов.',
                    meta: {
                        id: 'bluemarket-4019',
                        issue: 'MARKETFRONT-36441',
                        feature: 'Арбитраж',
                    },
                    params: {
                        isAuthWithPlugin: true,
                        pageId: PAGE_IDS_COMMON.ORDERS,
                    },
                }),
                prepareSuite(refund, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(paymentButtonExisting, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(orderStatus, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(removedOrderItems, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                    pageObjects: {
                        orderItems() {
                            return this.createPageObject(OrderItems, {
                                parent: this.orderCard,
                            });
                        },
                        changedOrderItem() {
                            return this.createPageObject(OrderItem, {
                                parent: this.orderItems.changedItemsBlock,
                            });
                        },
                    },
                }),
                prepareSuite(returnInfo, {
                    params: {
                        isAuthWithPlugin: true,
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
                prepareSuite(tracking, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(unpaidOrderPaymentProcess, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                    pageObjects: {
                        orderPaymentButton() {
                            return this.createPageObject(ActionButton, {
                                parent: this.orderCard,
                            });
                        },
                    },
                }),
                prepareSuite(unpaidOrderCard, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(bindByLink),
                prepareSuite(feedbackOnOrderCard, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(dsbs, {
                    params: {
                        isAuthWithPlugin: true,
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
                }),
                prepareSuite(estimated, {
                    params: {
                        isAuthWithPlugin: true,
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
                }),
                prepareSuite(deliveryReschedulePopupSuite),
                prepareSuite(verifyDeliveryRescheduleSuite),
                prepareSuite(verifyDeliveryLastMileRescheduleSuite),
                prepareSuite(verifiedCancellationPopup),
                prepareSuite(cancellationRejectionPopup),
                prepareSuite(removedItemsVerificationPopup),
                prepareSuite(removedOrderItemsPopup),
                prepareSuite(checkUnitsSuite, {
                    meta: {
                        id: 'marketfront-5573',
                        issue: 'MARKETFRONT-78854',
                    },
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(ordersDeliveryVerificationCode, {
                    meta: {
                        id: 'bluemarket-4108',
                    },
                    params: {
                        pageId: PAGE_IDS_COMMON.ORDERS,
                    },
                })
            ),
        },
        {
            'Неавторизованный пользователь.': mergeSuites(
                prepareSuite(cancel, {
                    params: {
                        needMuid: true,
                    },
                }),
                prepareSuite(dropship, {
                    params: {
                        needMuid: true,
                    },
                }),
                prepareSuite(details, {
                    params: {
                        needMuid: true,
                    },
                }),
                prepareSuite(preorder, {
                    params: {
                        needMuid: true,
                    },
                }),
                prepareSuite(paymentButtonExisting, {
                    params: {
                        needMuid: true,
                    },
                }),
                prepareSuite(orderStatus, {
                    params: {
                        needMuid: true,
                    },
                }),
                prepareSuite(dsbs, {
                    params: {
                        needMuid: true,
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
        }
    ),
});
