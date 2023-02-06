import {
    makeSuite,
    makeCase,
    mergeSuites,
} from 'ginny';
import assert from 'assert';
import {setupOrder, openEntryPage, setupOrderConsultations} from '../utils';

module.exports = makeSuite('Открытие чата по заказу', {
    feature: 'Арбитраж',
    environment: 'kadavr',
    params: {
        pageId: 'Идентификатор страницы',
        orderStatus: 'Статус заказа',
        orderSubstatus: 'Подстатус заказа',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                assert(this.orderCard, 'orderCard pageObject must be defined');
                assert(this.orderConsultationButton, 'orderConsultationButton pageObject must be defined');
                assert(this.yandexMessenger, 'yandexMessenger pageObject must be defined');
                assert(this.params.orderStatus, 'orderStatus param must be defined');

                const {orderId} = await setupOrder(this, {
                    status: this.params.orderStatus,
                    substatus: this.params.orderSubstatus,
                    deliveryDaysDiff: 0,
                    isDsbs: true,
                });

                await setupOrderConsultations(this, {isExisting: false});

                await openEntryPage(this, {orderId});
            },
        },
        {
            'Нажимаем на кнопку "Чат с продавцом"': makeCase({
                async test() {
                    await this.orderCard.waitForVisible();

                    await this.orderConsultationButton.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка чата должна быть видна'
                        );
                },
            }),
        }
    ),
});

