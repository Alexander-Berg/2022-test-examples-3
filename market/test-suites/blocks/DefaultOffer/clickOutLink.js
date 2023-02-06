import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок DefaultOffer
 * @property {PageObject.DefaultOffer} defaultOffer
 */
export default makeSuite('Кнопка перехода в магазин.', {
    story: {
        'По умолчанию': {
            'должна присутствовать': makeCase({
                test() {
                    return this.defaultOffer.button.isExisting().should.eventually.to.equal(
                        true, 'Кнопка "В магазин" должна присутствовать'
                    );
                },
            }),
        },
    },
});
