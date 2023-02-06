import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок referenceUnit
 * @param {PageObject.parent} Любой PageObject в котором есть метод getFullPrice()
 */
export default makeSuite('Основная единица измерения.', {
    story: {
        'По умолчанию': {
            'Присутствует в цене товара': makeCase({
                params: {
                    expectedPriceText: 'Ожидаемое значение для цены товара.',
                },
                async test() {
                    const {expectedPriceText = '2 215 ₽/уп'} = this.params;

                    await this.parent.waitForVisible();

                    const priceText = await this.parent.getFullPrice();

                    return this.expect(priceText).to.be.equal(
                        expectedPriceText,
                        'Указана цена за упаковку'
                    );
                },
            }),
        },
    },
});
