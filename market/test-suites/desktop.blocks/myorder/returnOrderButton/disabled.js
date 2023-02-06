

const {makeCase, makeSuite} = require('ginny');

module.exports = makeSuite('Кнопка "Вернуть заказ"', {
    story: {
        'Не активная кнопка "Вернуть заказ" должна быть видна': makeCase({
            async test() {
                await this.myOrder.isExisting(this.myOrder.returnsButtonDisabled)
                    .should.eventually.equal(true, 'Не активная кнопка "Вернуть заказ" должна отображаться');
            },
        }),
        'Подпись под неактивной кнопкой': makeCase({
            async test() {
                await this.myOrder.isExisting(this.myOrder.returnsButtonDisabledNote)
                    .should.eventually.equal(true, 'Подпись под неактивной кнопкой должна отображаться');
                await this.myOrder.getReturnsButtonDisabledNoteText()
                    .should.eventually.to.be.match(
                        new RegExp('Кнопка станет активной в течение нескольких дней, ' +
                        'а пока оформить возврат можно по телефону \\d \\d{3} \\d{3}-\\d{2}-\\d{2}'),
                        'Подпись должна быть "Кнопка станет активной в течение нескольких дней, ' +
                        'а пока оформить возврат можно по телефону \\d \\d{3} \\d{3}-\\d{2}-\\d{2}'
                    );
            },
        }),
    },
});
