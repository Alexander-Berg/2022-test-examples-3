import {makeSuite, makeCase} from 'ginny';

const MESSAGE = 'Текст доставки соответствует ожидаемому';

/**
 * @param {PageObject.Delivery | PageObject.SnippetDelivery} delivery
 */
export default makeSuite('Авторасчёт доставки', {
    feature: 'Автоматический расчёт доставки',
    story: {
        'По умолчанию': {
            'текст доставки соответствует ожидаемому': makeCase({
                async test() {
                    const {expectedText, matchMode} = this.params;

                    if (matchMode) {
                        return this.delivery.getText()
                            .then(text => expectedText.test(text))
                            .should.eventually.be.equal(true, MESSAGE);
                    }

                    return this.delivery.getText()
                        .then(text => this.expect(text).to.be.equal(expectedText, MESSAGE));
                },
            }),
        },
    },
});
