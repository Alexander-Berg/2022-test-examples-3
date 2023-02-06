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
import OutletInformation from '@self/root/src/widgets/content/OrderMapPopup/components/SnippetOutlet/__pageObject';

import removedOrderItems from '@self/root/src/spec/hermione/test-suites/blocks/order/removedOrderItems';
import cashback from '@self/root/src/spec/hermione/test-suites/touch.blocks/order/cashback';
import unpaidOrderPaymentProcess from '@self/root/src/spec/hermione/test-suites/touch.blocks/unpaidOrder/paymentProcess';
import documents from '@self/root/src/spec/hermione/test-suites/touch.blocks/order/documents';
import returnInfo from '@self/root/src/spec/hermione/test-suites/touch.blocks/order/returnInfo';
import editOrder from '@self/root/src/spec/hermione/test-suites/touch.blocks/myorder/editOrder';
import marketBranded from '@self/root/src/spec/hermione/test-suites/touch.blocks/myorder/marketBranded';
import cancel from '@self/root/src/spec/hermione/test-suites/touch.blocks/orders/cancel';
import refund from '@self/root/src/spec/hermione/test-suites/touch.blocks/orders/refund';
import paymentButtonExisting from '@self/root/src/spec/hermione/test-suites/touch.blocks/myorder/paymentButtonExisting';
import orderStatus from '@self/root/src/spec/hermione/test-suites/touch.blocks/myorder/orderStatus';
import storagePeriod from '@self/root/src/spec/hermione/test-suites/touch.blocks/myorder/storagePeriod';
import details from '@self/root/src/spec/hermione/test-suites/touch.blocks/order/details';
import preorder from '@self/root/src/spec/hermione/test-suites/touch.blocks/order/preorder';
import dropship from '@self/root/src/spec/hermione/test-suites/touch.blocks/order/dropship';
import deliveryFeedbackOrderCard from '@self/root/src/spec/hermione/test-suites/blocks/deliveryFeedback/deliveryFeedbackOrderCard';
import genericBundle from '@self/root/src/spec/hermione/test-suites/touch.blocks/order/genericBundle';
import feedbackOnOrderCard from '@self/root/src/spec/hermione/test-suites/blocks/orderFeedback/feedbackOnOrderCard';
import orderConsultationsOrderCard
    from '@self/root/src/spec/hermione/test-suites/blocks/orderConsultations/orderConsultationsOrderCard';
import deliveryReschedulePopupSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/deliveryReschedulePopup';
import verifyDeliveryRescheduleSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/verifyDeliveryReschedule';
import verifyDeliveryLastMileRescheduleSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/verifyDeliveryLastMileReschedule';
import verifiedCancellationPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/verifiedCancellation';
import cancellationRejectionPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/cancellationRejection';
import removedItemsVerificationPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/removedItemsVerification';
import removedOrderItemsPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/removedOrderItems';
import orderOutletRouteInformation from '@self/root/src/spec/hermione/test-suites/blocks/order/orderOutletRouteInformation';
import orderDeliveryVerificationCode from '@self/root/src/spec/hermione/test-suites/touch.blocks/order/deliveryVerificationCode';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Заказ', {
    environment: 'testing',
    params: {
        ...commonParams.description,
    },
    defaultParams: {
        ...commonParams.value,
        pageId: PAGE_IDS_COMMON.ORDER,

    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    orderCard: () => this.createPageObject(OrderCard),
                    orderStatus: () => this.createPageObject(OrderStatus, {
                        parent: this.orderCard,
                    }),
                });
            },
        },

        {
            'Авторизованный пользователь.': mergeSuites(
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
                prepareSuite(dropship, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(marketBranded, {
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
                    suiteName: 'Арбитраж. Открытие чата из деталей заказа',
                    meta: {
                        id: 'bluemarket-4020',
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
                prepareSuite(cashback, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(returnInfo, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(editOrder, {
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
                prepareSuite(storagePeriod, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(genericBundle, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(feedbackOnOrderCard, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                }),
                prepareSuite(deliveryReschedulePopupSuite),
                prepareSuite(verifyDeliveryRescheduleSuite),
                prepareSuite(verifyDeliveryLastMileRescheduleSuite),
                prepareSuite(verifiedCancellationPopup),
                prepareSuite(cancellationRejectionPopup),
                prepareSuite(removedItemsVerificationPopup),
                prepareSuite(removedOrderItemsPopup),
                prepareSuite(orderOutletRouteInformation, {
                    params: {
                        isAuthWithPlugin: true,
                    },
                    pageObjects: {
                        outletInformation() {
                            return this.createPageObject(OutletInformation);
                        },
                    },
                }),
                prepareSuite(orderDeliveryVerificationCode, {
                    meta: {
                        id: 'bluemarket-4109',
                    },
                    params: {
                        pageId: PAGE_IDS_COMMON.ORDER,
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
                prepareSuite(refund, {
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
                prepareSuite(documents, {
                    params: {
                        needMuid: true,
                    },
                }),
                prepareSuite(editOrder, {
                    params: {
                        needMuid: true,
                    },
                }),
                prepareSuite(genericBundle, {
                    params: {
                        needMuid: true,
                    },
                })
            ),
        }
    ),
});
