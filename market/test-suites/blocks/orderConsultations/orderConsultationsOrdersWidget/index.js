import {
    makeSuite,
    makeCase,
    mergeSuites,
} from 'ginny';
import assert from 'assert';
import YandexMessenger from '@self/root/src/widgets/core/YandexMessenger/__pageObject';
import {setupOrderConsultations} from '../utils';

module.exports = makeSuite('Арбитраж. Открытие чата из виджета "Заказ у меня"', {
    feature: 'Арбитраж',
    id: 'bluemarket-4021',
    issue: 'MARKETFRONT-36441',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                assert(this.orderConsultationButton, 'orderConsultationButton pageObject must be defined');

                this.setPageObjects({
                    yandexMessenger: () => this.createPageObject(YandexMessenger),
                });

                await setupOrderConsultations(this, {isExisting: false});
            },
        },
        {
            'Нажимаем на кнопку "Чат с продавцом"': makeCase({
                async test() {
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
