import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.price}
 */
export default makeSuite('Основная единица измерения.', {
    story: {
        'По умолчанию': {
            'присутствует в цене товара.': makeCase({
                params: {
                    expectedFirstDOMainPriceText: 'Ожидаемое значение для цены товара в первом ДО.',
                    expectedSecondDOMainPriceText: 'Ожидаемое значение для цены товара во втором ДО.',
                },
                async test() {
                    const {expectedFirstDOMainPriceText, expectedSecondDOMainPriceText} = this.params;

                    await this.price.waitForVisible();

                    const priceText = await this.price.getPriceText();
                    return this.expect(priceText).to.be.deep.equal(
                        [expectedFirstDOMainPriceText, expectedSecondDOMainPriceText],
                        'Указана цена за упаковку в обоих ДО'
                    );
                },
            }),
        },
    },
});
