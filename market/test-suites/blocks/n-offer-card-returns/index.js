import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.OfferCardReturns} offerCardReturns
 */
export default makeSuite('Контент блока "Гарантии"', {
    story: {
        'Текст с информацией о возврате': {
            'виден на странице': makeCase({
                async test() {
                    const visible = this.offerCardReturns.text.isVisible();

                    await this.expect(visible).to.be.equal(true, 'Информация о возврате отсутствует');
                },
            }),
        },

        'Ссылка на условия возврата': {
            'кликабельна': makeCase({
                async test() {
                    const href = this.offerCardReturns.getLinkHref();

                    await this.expect(href).not.to.be.equal(null, 'Атрибут пустой');
                },
            }),
        },
    },
});
