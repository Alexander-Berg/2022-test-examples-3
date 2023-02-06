const {makeCase, makeSuite, mergeSuites} = require('ginny');

module.exports = makeSuite('Кнопка "Вернуть заказ"', {
    story: mergeSuites({
        'Кнопка "Вернуть заказ" должна быть видна': makeCase({
            async test() {
                const isExist = await this.orderCard.isExisting(this.orderCard.returnButton);
                return this.expect(isExist)
                    .to.be.equal(true, 'Кнопка "Вернуть заказ" должна отображаться');
            },
        }),
    }),
});
