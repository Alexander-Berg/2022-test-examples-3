import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Button2} productDefaultOfferActionButton
 * @param {PageObject.ProductDefaultOffer} productDefaultOffer
 */
export default makeSuite('Блок ДО', {
    story: {
        'На карточке оффeра.': {
            'кнопка "В магазин"': {
                'всегда видна': makeCase({
                    async test() {
                        const visible = this.productDefaultOfferActionButton.isVisible();

                        await this.expect(visible).to.be.equal(true, 'Кнопка не видна на странице');
                    },
                }),
            },

            'цена': {
                'всегда видна': makeCase({
                    async test() {
                        const visible = await this.productDefaultOffer.isVisible();

                        await this.expect(visible).to.be.equal(true, 'Цена видна');
                    },
                }),
            },
        },
    },
});
