import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок DefaultOffer
 * @property {PageObject.DefaultOffer} defaultOffer
 */
export default makeSuite('Название магазина.', {
    story: {
        'По умолчанию': {
            'должно присутствовать': makeCase({
                test() {
                    return this.defaultOffer.shopName.isExisting().should.eventually.to.equal(
                        true, 'Название магазина должно присутствовать'
                    );
                },
            }),
        },
    },
});
