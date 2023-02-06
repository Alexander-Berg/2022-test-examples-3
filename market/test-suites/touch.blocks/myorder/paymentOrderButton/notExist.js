import assert from 'assert';

const {makeCase, makeSuite, mergeSuites} = require('ginny');

module.exports = makeSuite('Кнопка "Оплатить"', {
    story: mergeSuites({
        'Кнопка "Оплатить" не должна быть видна': makeCase({
            async test() {
                assert(this.orderCard, 'PageObject.orderCard must be defined');

                const isExist = await this.orderCard.isExisting(this.orderCard.paymentButton);
                return this.expect(isExist)
                    .to.be.equal(false, 'Кнопка "Оплатить" не должна отображаться');
            },
        }),
    }),
});
