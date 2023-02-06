

const {makeCase, makeSuite, mergeSuites} = require('ginny');

module.exports = makeSuite('Кнопка "Вернуть заказ"', {
    story: mergeSuites({
        'Кнопка "Вернуть заказ" не должна быть видна': makeCase({
            async test() {
                const isExist = await this.myOrder.isExisting(this.myOrder.returnsButton);
                return this.expect(isExist)
                    .to.be.equal(false, 'Кнопка "Вернуть заказ" не должна отображаться');
            },
        }),
    }),
});
