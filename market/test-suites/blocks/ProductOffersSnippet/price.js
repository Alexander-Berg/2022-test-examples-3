import {makeCase, makeSuite} from 'ginny';


const currencyList = {
    RUB: '₽',
};

/**
 * Тесты на блок ProductOffersSnippet
 * @param {PageObject.ProductOffersSnippet} offerSnippet
 */
export default makeSuite('Цена товарного предложения.', {
    params: {
        expectedPriceValue: 'Ожидаемое значение стоимости',
        expectedPriceCurrency: 'Ожидаемая валюта',
    },
    story: {
        'По умолчанию': {
            'должна присутствовать': makeCase({
                test() {
                    return this.offerSnippet.price.isExisting().should.eventually.to.equal(
                        true, 'Цена товарного предложения должна присутствовать'
                    );
                },
            }),
            'должна содержать корректное значение': makeCase({
                async test() {
                    const price = await this.offerSnippet.getPriceValue();

                    return this.expect(Number(price)).to.be.equal(
                        this.params.expectedPriceValue,
                        'Значение цены товарного предложения должно отображаться'
                    );
                },
            }),
            'должна содержать корректную валюту': makeCase({
                async test() {
                    return this.offerSnippet.getPriceCurrency().should.eventually.to.equal(
                        currencyList[this.params.expectedPriceCurrency],
                        'Значение цены товарного предложения должно отображаться'
                    );
                },
            }),
        },
    },
});
