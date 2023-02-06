import {
    makeSuite,
    makeCase,
} from 'ginny';

/**
 * Тесты на успешное офорление постоплатного заказа
 * @param {PageObject.OrderConfirmation} orderConfirmation
 */
// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Постоплатный заказ успешно оформлен.', {
    environment: 'testing',
    story: {
        'Состав кнопок': {
            'по умолчанию': {
                'отображается правильный состав': makeCase({
                    id: 'bluemarket-513',
                    issue: 'bluemarket-461',
                    feature: 'Спасибо за заказ',
                    test() {
                        const checkVisibility = (control, isVisible, title) => this.orderConfirmation.isVisible(control)
                            .should.eventually.to.be
                            .equal(isVisible, `Ссылка "${title}" ${isVisible ? '' : 'не '}должна быть отображена`);

                        return checkVisibility(
                            this.orderConfirmation.receiptLink, false, 'Посмотреть чек'
                        )
                            .then(() => checkVisibility(
                                this.orderConfirmation.detailsLink, true, 'Подробности'
                            ))
                            .then(() => checkVisibility(
                                this.orderConfirmation.trackLink, true, 'Отследить'
                            ))
                            .then(() => checkVisibility(
                                this.orderConfirmation.continueShoppingButton, true, 'Продолжить покупки'
                            ));
                    },
                }),
            },
        },
    },
});
