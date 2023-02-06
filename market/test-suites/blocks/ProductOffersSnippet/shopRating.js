import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок ProductOffersSnippet
 * @param {PageObject.ProductOffersSnippet} offerSnippet
 */
export default makeSuite('Рейтинг магазина.', {
    story: {
        'По умолчанию': {
            'должен присутствовать': makeCase({
                test() {
                    return this.offerSnippet.shopRating.isExisting().should.eventually.to.equal(
                        true, 'Рейтинг магазина должен присутствовать'
                    );
                },
            }),
        },
    },
});
