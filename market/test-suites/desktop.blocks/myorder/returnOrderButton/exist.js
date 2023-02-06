

const {makeCase, makeSuite, mergeSuites} = require('ginny');

module.exports = makeSuite('Кнопка "Вернуть заказ"', {
    story: mergeSuites({
        'Кнопка "Вернуть заказ" должна быть видна': makeCase({
            async test() {
                const isExist = await this.myOrder.isExisting(this.myOrder.returnsButton);
                return this.expect(isExist)
                    .to.be.equal(true, 'Кнопка "Вернуть заказ" должна отображаться');
            },
        }),
    }),
});
