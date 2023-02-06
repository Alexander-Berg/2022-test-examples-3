import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок ProductOffersSnippet
 * @param {PageObject.ProductOffersSnippet} offerSnippet
 */
export default makeSuite('Название магазина.', {
    params: {
        expectedShopName: 'Ожидаемое название магазина',
    },
    story: {
        'По умолчанию': {
            'должно присутствовать': makeCase({
                test() {
                    return this.offerSnippet.shopName.isExisting().should.eventually.to.equal(
                        true, 'Название магазина должно присутствовать'
                    );
                },
            }),
            'должно иметь корректное значение': makeCase({
                test() {
                    return this.offerSnippet.getShopName().should.eventually.to.equal(
                        this.params.expectedShopName,
                        'Название магазина должно содержать корректное значение'
                    );
                },
            }),
        },
    },
});
