

import assert from 'assert';

const {makeCase, makeSuite, mergeSuites} = require('ginny');

module.exports = makeSuite('Кнопка "Оплатить"', {
    story: mergeSuites({
        'Кнопка "Оплатить" не должна быть видна': makeCase({
            async test() {
                assert(this.myOrder, 'PageObject.myOrder must be defined');

                const exists = await this.myOrder.isExisting(this.myOrder.paymentButton);
                return this.expect(exists)
                    .to.be.equal(false, 'Кнопка "Оплатить" не должна отображаться');
            },
        }),
    }),
});
