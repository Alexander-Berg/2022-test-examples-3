const {makeCase, makeSuite, mergeSuites} = require('ginny');

module.exports = makeSuite('Кнопка "Вернуть заказ"', {
    story: mergeSuites({
        'Не активная кнопка "Вернуть заказ" должна быть видна': makeCase({
            async test() {
                await this.orderCard.isExisting(
                    this.orderCard.returnOrderLinkEnabled
                ).should.eventually.equal(false, 'Активная кнопка "Вернуть заказ" не должна отображаться');
                await this.orderCard.isExisting(
                    this.orderCard.returnOrderLinkDisabled
                ).should.eventually.equal(true, 'Не активная кнопка "Вернуть заказ" должна отображаться');
            },
        }),
    }),
});
