import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import OrderStatusNotifierPaymentButton
    from '@self/root/src/components/Orders/OrderStatusNotifier/OrderStatusNotifierPaymentButton/__pageObject';
import {OrderStatusNotifier} from '@self/root/src/components/Orders/OrderStatusNotifier/__pageObject';
import OrderItems from '@self/root/src/components/Orders/OrderItems/__pageObject';
import OrderItem from '@self/root/src/components/Orders/OrderItems/OrderItem/__pageObject';
import OrderCard from '@self/root/src/widgets/content/orders/OrderCard/components/View/__pageObject';
import OutletInformation from '@self/root/src/widgets/content/orders/OutletInformation/__pageObject';

import removedOrderItems from '@self/root/src/spec/hermione/test-suites/blocks/order/removedOrderItems';
import dsbs from '@self/root/src/spec/hermione/test-suites/blocks/order/dsbs';
import estimated from '@self/root/src/spec/hermione/test-suites/blocks/order/estimated';
import orderOutletRouteInformation
    from '@self/root/src/spec/hermione/test-suites/blocks/order/orderOutletRouteInformation';
import unpaidOrderPaymentProcess
    from '@self/root/src/spec/hermione/test-suites/desktop.blocks/unpaidOrder/paymentProcess';
import unpaidOrderCard from '@self/root/src/spec/hermione/test-suites/desktop.blocks/unpaidOrder/orderCard';
import verifiedCancellationPopup
    from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/verifiedCancellation';
import cancellationRejectionPopup
    from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/cancellationRejection';
import removedItemsVerificationPopup
    from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/removedItemsVerification';
import removedOrderItemsPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/removedOrderItems';
import deliveryReschedulePopupSuite
    from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/deliveryReschedulePopup';
import verifyDeliveryRescheduleSuite
    from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/verifyDeliveryReschedule';
import verifyDeliveryLastMileRescheduleSuite
    from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/verifyDeliveryLastMileReschedule';

import orderItems from './orderItems';
import cancel from './cancel';
import paymentMethod from './paymentMethod';
import preorder from './preorder';
import dropship from './dropship';
import marketBranded from './marketBranded';
import checkOrderReturnPopup from './return';
import paymentButtonExisting from './paymentButtonExisting';
import orderStatus from './orderStatus';
import documents from './documents';
import editOrder from './editOrder';
import storagePeriod from './storagePeriod';
import tracking from './tracking';
import cashback from './cashback';
import minimumQuantityReorder from './minimumQuantityReorder';


/**
 * Тесты на блок отдельного заказа.
 * Тесты разложены на сьюты по фичам и заточены на работу на странице отдельного заказа и списка заказов.
 *
 * @param {PageObject.Orders} ordersLayout - список заказов
 */
module.exports = makeSuite('Информация о заказе.', {
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    myOrder() {
                        return this.createPageObject(
                            OrderCard,
                            {
                                parent: this.ordersLayout,
                                root: `${OrderCard.root}:nth-child(1)`,
                            }
                        );
                    },
                });
            },
        },
        prepareSuite(orderItems, {}),
        prepareSuite(removedOrderItems, {
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
        prepareSuite(cancel, {}),
        prepareSuite(paymentMethod, {}),
        prepareSuite(preorder, {}),
        prepareSuite(dropship, {}),
        prepareSuite(marketBranded, {}),
        prepareSuite(tracking),
        prepareSuite(checkOrderReturnPopup, {}),
        prepareSuite(paymentButtonExisting, {}),
        prepareSuite(orderStatus, {
            pageObjects: {
                orderStatusNotifier() {
                    return this.createPageObject(OrderStatusNotifier, {
                        parent: this.myOrder,
                    });
                },
            },
        }),
        prepareSuite(documents, {}),
        prepareSuite(editOrder, {}),
        prepareSuite(unpaidOrderPaymentProcess, {
            pageObjects: {
                orderPaymentButton() {
                    return this.createPageObject(OrderStatusNotifierPaymentButton, {
                        parent: this.myOrder,
                    });
                },
            },
        }),
        prepareSuite(unpaidOrderCard),
        prepareSuite(storagePeriod),
        prepareSuite(cashback),
        prepareSuite(dsbs),
        prepareSuite(estimated),
        prepareSuite(deliveryReschedulePopupSuite),
        prepareSuite(verifyDeliveryRescheduleSuite),
        prepareSuite(verifyDeliveryLastMileRescheduleSuite),
        prepareSuite(verifiedCancellationPopup),
        prepareSuite(cancellationRejectionPopup),
        prepareSuite(removedItemsVerificationPopup),
        prepareSuite(removedOrderItemsPopup),
        prepareSuite(orderOutletRouteInformation, {
            pageObjects: {
                outletInformation() {
                    return this.createPageObject(OutletInformation);
                },
                orderCard() {
                    return this.myOrder;
                },
            },
        }),
        prepareSuite(minimumQuantityReorder)
    ),
});
