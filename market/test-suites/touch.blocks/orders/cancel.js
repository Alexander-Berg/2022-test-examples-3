import {
    makeCase,
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import {CancellationButton} from '@self/root/src/components/OrderActions/Actions/CancellationButton/__pageObject';
import {
    OrderCancellation,
} from '@self/root/src/widgets/content/orders/OrderCancellation/components/OrderCancellation/__pageObject';
import {Radio} from '@self/root/src/uikit/components/Radio/__pageObject';
import {TextField} from '@self/root/src/uikit/components/TextField/__pageObject';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import {OrderCancellationDone}
    from '@self/root/src/widgets/content/orders/OrderCancellation/containers/OrderCancellationDone/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';
import {CancellationTitle} from '@self/root/src/components/CancellationTitle/__pageObject';
import {OrderHeader} from '@self/root/src/components/OrderHeader/__pageObject';
// Заказ в списке
import OrderCard from '@self/root/src/widgets/content/orders/OrderCard/components/View/__pageObject';
// Попап с уведомлениями (об ошибках)
import Notification from '@self/root/src/components/Notification/__pageObject';

import cancelValidationErrorSuite
    from '@self/root/src/spec/hermione/test-suites/touch.blocks/orders/cancel-validation-errors';
import {createOrder} from '@self/root/market/src/spec/hermione/test-suites/blocks/order/helpers';


/**
 * Тесты на отмену заказа для списка заказов и для отдельного заказа
 * @param {PageObject.OrderCard} orderCard - карточка заказа (для списка и для отдельного заказа они разные),
 * тут используется только как родитель для остального
 *
 * После успешной отмены происходит переход на список заказов,
 * поэтому всегда нужны списочные ПО orderCardInList и orderHeader
 */

module.exports = makeSuite('Отмена заказа.', {
    feature: 'Отмена заказа',
    params: {
        pageId: 'Идентификатор страницы',
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    orderCardInList() {
                        return this.createPageObject(OrderCard, {root: `${OrderCard.root}:nth-child(1)`});
                    },
                    orderCancelButton: () => this.createPageObject(CancellationButton, {
                        parent: this.orderCard,
                        root: `${CancellationButton.root} ${Link.root}`,
                    }),
                    orderCancellation: () => this.createPageObject(OrderCancellation),
                    cancellationButton: () => this.createPageObject(Button, {parent: this.orderCancellation}),
                    cancellationComment: () => this.createPageObject(TextField, {parent: this.orderCancellation}),
                    cancelSubstatusRadioControl: () => this.createPageObject(
                        Radio,
                        {
                            parent: this.orderCancellation,
                            // всегда выбираем первую причину отказа
                            root: `${Radio.root}:nth-child(1)`,
                        }
                    ),
                    cancellationDone: () => this.createPageObject(OrderCancellationDone),
                    cancellationDoneTitle: () => this.createPageObject(
                        CancellationTitle,
                        {parent: this.cancellationDone}
                    ),
                    cancellationDoneButton: () => this.createPageObject(Button, {
                        parent: this.cancellationDone,
                    }),
                    orderHeader: () => this.createPageObject(OrderHeader, {parent: this.orderCardInList}),
                    orderDetailsHeader: () => this.createPageObject(OrderHeader, {parent: this.orderCard}),
                    cancellationErrorPopup: () => this.createPageObject(Notification),
                });
            },
        },
        {
            'Кнопка отмены для заказа в статусе PENDING': {
                beforeEach() {
                    return createOrder.call(this, {status: 'PENDING'});
                },
                'по умолчанию': {
                    'должна быть видимой': makeCase({
                        id: 'bluemarket-672',
                        issue: 'BLUEMARKET-3250',
                        environment: 'kadavr',
                        async test() {
                            return this.orderCancelButton.root
                                .isVisible()
                                .should.eventually.to.be.equal(true, 'Кнопка отмены должна быть видимой');
                        },
                    }),
                },
            },

            'Кнопка отмены для заказа в статусе UNPAID': {
                beforeEach() {
                    return createOrder.call(this, {status: 'UNPAID'});
                },
                'по умолчанию': {
                    'должна быть видимой': makeCase({
                        id: 'bluemarket-672',
                        issue: 'BLUEMARKET-3250',
                        environment: 'kadavr',
                        test() {
                            return this.orderCancelButton.root
                                .isVisible()
                                .should.eventually.to.be.equal(true, 'Кнопка отмены должна быть видимой');
                        },
                    }),
                },
            },

            'Кнопка отмены для заказа в статусе PROCESSING': {
                beforeEach() {
                    return createOrder.call(this, {status: 'PROCESSING'});
                },
                'по умолчанию': {
                    'должна быть видимой': makeCase({
                        id: 'bluemarket-672',
                        issue: 'BLUEMARKET-3250',
                        environment: 'kadavr',
                        test() {
                            return this.orderCancelButton.root
                                .isVisible()
                                .should.eventually.to.be.equal(true, 'Кнопка отмены должна быть видимой');
                        },
                    }),
                },
            },
        },
        {
            'Отмена заказа в статусе PENDING': mergeSuites(
                {
                    beforeEach() {
                        return createOrder.call(this, {status: 'PENDING'});
                    },
                },
                prepareSuite(cancelValidationErrorSuite, {})
            ),
        },
        {
            'Отмена заказа в статусе PROCESSING': {
                'при корректных данных без комментария': mergeSuites(
                    {
                        beforeEach() {
                            return createOrder.call(this, {status: 'PROCESSING'});
                        },
                    },
                    prepareSuite(cancelValidationErrorSuite, {})
                ),
                'при созданной заявке на отмену': {
                    beforeEach() {
                        return createOrder.call(this, {
                            cancellationRequest: {},
                            status: 'DELIVERY',
                        });
                    },
                    'выводится статус "Заказ отменится в ближайшие 3 дня"': makeCase({
                        id: 'bluemarket-191',
                        issue: 'BLUEMARKET-3250',
                        environment: 'kadavr',
                        test() {
                            const text = 'Заказ отменится в ближайшие 3 дня';

                            if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
                                return this.orderDetailsHeader
                                    .getText()
                                    .should.eventually.to.have.string(
                                        text,
                                        `Текст статуса должен быть "${text}"`
                                    );
                            }

                            return this.orderHeader
                                .getText()
                                .should.eventually.to.have.string(
                                    text,
                                    `Текст статуса должен быть "${text}"`
                                );
                        },
                    }),
                },
            },
        }
    ),
});
