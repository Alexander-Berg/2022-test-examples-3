import {makeCase, makeSuite} from 'ginny';


const currencyList = {
    RUB: '₽',
};

/**
 * Тесты на блок DefaultOffer
 * @property {PageObject.DefaultOffer} defaultOffer
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
                    return this.defaultOffer.price.isExisting().should.eventually.to.equal(
                        true, 'Цена дефолтного товарного предложения должна присутствовать'
                    );
                },
            }),
            'должна содержать корректное значение': makeCase({
                async test() {
                    if (!this.params.expectedPriceValue) {
                        return this.expect(true).to.be.equal(true);
                    }

                    const price = await this.defaultOffer.getPriceValue();

                    return this.expect(Number(price)).to.be.equal(
                        this.params.expectedPriceValue,
                        'Значение цены дефолтного товарного предложения должно отображаться'
                    );
                },
            }),
            'должна содержать корректную валюту': makeCase({
                async test() {
                    if (!this.params.expectedPriceCurrency) {
                        return this.expect(true).to.be.equal(true);
                    }

                    return this.defaultOffer.getPriceCurrency().should.eventually.to.equal(
                        currencyList[this.params.expectedPriceCurrency],
                        'Значение цены дефолтного товарного предложения должно отображаться'
                    );
                },
            }),
        },
    },
});
