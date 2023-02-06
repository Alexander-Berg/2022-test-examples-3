import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок ProductOffersSnippet
 * @param {PageObject.ProductOffersSnippet} offerSnippet
 */
export default makeSuite('Характеристики предложения.', {
    story: {
        'По умолчанию': {
            'должны присутствовать': makeCase({
                test() {
                    return this.offerSnippet.specs.isVisible().should.eventually.to.equal(
                        true, 'Характеристики преджложения должны присутствовать'
                    );
                },
            }),
        },
    },
});
