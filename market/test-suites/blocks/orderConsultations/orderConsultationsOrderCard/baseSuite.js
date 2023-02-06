import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';
import assert from 'assert';
import {
    ORDER_STATUS,
    ORDER_SUBSTATUS,
} from '@self/root/src/entities/order';
import openChat from './openChat';
import {
    setupOrder,
    openEntryPage,
    setupOrderConsultations,
} from '../utils';

module.exports = makeSuite('Арбитраж. Открытие чата с карточки заказа', {
    feature: 'Арбитраж',
    environment: 'kadavr',
    params: {
        pageId: 'Идентификатор страницы',
    },
    meta: {
        issue: 'MARKETFRONT-36441',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                assert(this.params.pageId, 'Param pageId must be defined');
                assert(this.orderCard, 'orderCard pageObject must be defined');
                assert(this.orderConsultationButton, 'orderConsultationButton pageObject must be defined');
                assert(this.yandexMessenger, 'yandexMessenger pageObject must be defined');
            },
        },
        {
            'DSBS заказ': {
                'В статусе PROCESSING': prepareSuite(openChat, {
                    params: {
                        orderStatus: ORDER_STATUS.PROCESSING,
                    },
                }),
                'В статусе DELIVERY': prepareSuite(openChat, {
                    params: {
                        orderStatus: ORDER_STATUS.DELIVERY,
                    },
                }),
                'В статусе PICKUP': prepareSuite(openChat, {
                    params: {
                        orderStatus: ORDER_STATUS.PICKUP,
                        orderSubstatus: ORDER_SUBSTATUS.PICKUP_SERVICE_RECEIVED,
                    },
                }),
                'В статусе DELIVERED': prepareSuite(openChat, {
                    params: {
                        orderStatus: ORDER_STATUS.DELIVERED,
                    },
                }),
            },
            'Отмененный DSBS заказ': {
                'Существующий чат': {
                    id: 'marketfront-5513',
                    async test() {
                        const {orderId} = await setupOrder(this, {
                            status: ORDER_STATUS.CANCELLED,
                            substatus: ORDER_SUBSTATUS.UNKNOWN,
                            deliveryDaysDiff: 0,
                            isDsbs: true,
                        });

                        await setupOrderConsultations(this, {isExisting: true, orderId});

                        await openEntryPage(this, {orderId});

                        await this.orderCard.waitForVisible();

                        await this.orderConsultationButton.isVisible()
                            .should.eventually.be.equal(
                                true,
                                'Кнопка чата должна быть видна'
                            );
                        await this.orderConsultationButton.getText()
                            .should.eventually.be.equal(
                                'Чат с продавцом',
                                'Кнопка чата должна быть для связи с продавцом'
                            );

                        await this.orderConsultationButton.click();
                    },
                },
                'Новый чат': {
                    id: 'bluemarket-4030',
                    async test() {
                        const {orderId} = await setupOrder(this, {
                            status: ORDER_STATUS.CANCELLED,
                            substatus: ORDER_SUBSTATUS.UNKNOWN,
                            deliveryDaysDiff: 0,
                            isDsbs: true,
                        });

                        await setupOrderConsultations(this, {isExisting: false});

                        await openEntryPage(this, {orderId});

                        await this.orderCard.waitForVisible();

                        await this.orderConsultationButton.isVisible()
                            .should.eventually.be.equal(
                                false,
                                'Кнопка чата должна быть скрыта'
                            );
                    },
                },
            },
            'Не DSBS заказ': {
                id: 'marketfront-5515',
                async test() {
                    const {orderId} = await setupOrder(this, {
                        status: ORDER_STATUS.PROCESSING,
                        substatus: ORDER_SUBSTATUS.UNKNOWN,
                        deliveryDaysDiff: 0,
                        isDsbs: false,
                    });

                    await setupOrderConsultations(this, {isExisting: false});

                    await openEntryPage(this, {orderId});

                    await this.orderCard.waitForVisible();

                    await this.orderConsultationButton.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка чата должна быть видна'
                        );
                    await this.orderConsultationButton.getText()
                        .should.eventually.be.equal(
                            'Чат с поддержкой',
                            'Кнопка чата должна быть для связи с поддержкой Маркета'
                        );
                },
            },
        }
    ),
});
