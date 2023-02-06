import {
    makeCase,
    makeSuite,
    mergeSuites,
} from 'ginny';
import CancelSubstatusRadioControl
    from '@self/root/src/widgets/content/orders/MyOrderCancel/components/CancelSubstatusRadioControl/__pageObject';
import MyOrderCancelButton from '@self/root/src/components/Orders/OrderCancelButton/__pageObject';
import MyOrderOrderStatus from '@self/root/src/components/OrderHeader/OrderStatus/__pageObject';
import CancelPopupBody
    from '@self/root/src/widgets/content/orders/MyOrderCancel/components/CancelPopupBody/__pageObject';
import CancellationRequestForm
    from '@self/root/src/widgets/content/orders/MyOrderCancel/components/CancellationRequestForm/__pageObject';
import CancellationRequestSuccess
    from '@self/root/src/widgets/content/orders/MyOrderCancel/components/CancellationRequestSuccess/__pageObject';
import InfoOpener from '@self/root/src/components/InfoOpener/__pageObject';
import Tooltip from '@self/root/src/uikit/components/AbstractTooltip/__pageObject';
import {
    ORDER_STATUS,
    ORDER_SUBSTATUS,
} from '@self/root/src/entities/order';

import {createOrder} from '@self/root/market/src/spec/hermione/test-suites/blocks/order/helpers';

/**
 * Тесты на отмену заказа
 * @param {PageObject.MyOrder} myOrder - заказ
 */
module.exports = makeSuite('Отмена заказа.', {
    feature: 'Отмена заказа',
    id: 'bluemarket-3304',
    issue: 'BLUEMARKET-9140',
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    myOrderCancelButton: () => this.createPageObject(MyOrderCancelButton, {parent: this.myOrder}),
                    myOrderOrderStatus: () => this.createPageObject(MyOrderOrderStatus, {parent: this.myOrder}),
                    myOrderOrderStatusInfoOpener: () => this.createPageObject(InfoOpener),
                    tooltip: () => this.createPageObject(Tooltip),
                    cancelPopupBody: () => this.createPageObject(CancelPopupBody),
                    cancellationRequestForm: () => this.createPageObject(
                        CancellationRequestForm,
                        {parent: this.cancelPopupBody}
                    ),
                    cancelSubstatusRadioControl: () => this.createPageObject(
                        CancelSubstatusRadioControl,
                        {
                            parent: this.cancellationRequestForm,
                            root: `${CancelSubstatusRadioControl.root}:nth-child(1)`,
                        }
                    ),
                    cancellationRequestSuccess: () => this.createPageObject(
                        CancellationRequestSuccess,
                        {parent: this.cancelPopupBody}
                    ),
                });
            },
        },
        {
            'Кнопка отмены для заказа в статусе PENDING': {
                beforeEach() {
                    return createOrder.call(this, {status: 'PENDING'});
                },
                отображается: makeCase({
                    test() {
                        return isCancelButtonVisible.call(this);
                    },
                }),
            },

            'Кнопка отмены для заказа в статусе UNPAID': {
                beforeEach() {
                    return createOrder.call(this, {status: 'UNPAID'});
                },
                отображается: makeCase({
                    test() {
                        return isCancelButtonVisible.call(this);
                    },
                }),
            },

            'Кнопка отмены для заказа в статусе PROCESSING': {
                beforeEach() {
                    return createOrder.call(this, {status: 'PROCESSING'});
                },
                отображается: makeCase({
                    test() {
                        return isCancelButtonVisible.call(this);
                    },
                }),
            },

            'Кнопка отмены для заказа в статусе DELIVERY': {
                beforeEach() {
                    return createOrder.call(this, {status: 'DELIVERY'});
                },
                отображается: makeCase({
                    test() {
                        return isCancelButtonVisible.call(this);
                    },
                }),
            },

            'Кнопка отмены для заказа в статусе PICKUP': {
                beforeEach() {
                    return createOrder.call(this, {
                        status: ORDER_STATUS.PICKUP,
                        substatus: ORDER_SUBSTATUS.PICKUP_SERVICE_RECEIVED,
                    });
                },
                отображается: makeCase({
                    test() {
                        return isCancelButtonVisible.call(this);
                    },
                }),
            },

            'Кнопка отмены для заказа в статусе CANCELLED': {
                beforeEach() {
                    return createOrder.call(this, {status: 'CANCELLED'});
                },
                'не отображается': makeCase({
                    test() {
                        return this.myOrderCancelButton
                            .isVisible()
                            .should.eventually.to.be.equal(false, 'Кнопки отмены не должно быть');
                    },
                }),
            },

            'Кнопка отмены для заказа в статусе DELIVERED': {
                beforeEach() {
                    return createOrder.call(this, {status: 'DELIVERED'});
                },
                'не отображается': makeCase({
                    test() {
                        return this.myOrderCancelButton
                            .isVisible()
                            .should.eventually.to.be.equal(false, 'Кнопки отмены не должно быть');
                    },
                }),
            },
        },
        {
            'Отмена заказа c cancellationRequest': {
                'при созданной заявке на отмену': {
                    beforeEach() {
                        return createOrder.call(this, {
                            status: 'PROCESSING',
                            cancellationRequest: {},
                        });
                    },
                    'выводится статус "Отменён"': makeCase({
                        id: 'bluemarket-2379',
                        issue: 'BLUEMARKET-1743',
                        environment: 'kadavr',
                        test() {
                            return checkOrderStatus.call(this);
                        },
                    }),
                },
            },
        }
    ),
});

async function isCancelButtonVisible() {
    return this.myOrderCancelButton
        .isVisible()
        .should.eventually.to.be.equal(true, 'Кнопка отмены должна быть видимой');
}

async function checkOrderStatus() {
    return this.myOrderOrderStatus
        .getStatusText()
        .should.eventually.to.be.equal(
            'Ждёт отмены',
            'Текст статуса должен быть "Ждёт отмены"'
        );
}
