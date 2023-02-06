import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок DefaultOffer
 * @property {PageObject.DefaultOffer} defaultOffer
 */
export default makeSuite('Рейтинг магазина.', {
    story: {
        'По умолчанию': {
            'должен присутствовать': makeCase({
                test() {
                    return this.defaultOffer.shopRating.isExisting().should.eventually.to.equal(
                        true, 'Рейтинг магазина должен присутствовать'
                    );
                },
            }),
        },
    },
});
