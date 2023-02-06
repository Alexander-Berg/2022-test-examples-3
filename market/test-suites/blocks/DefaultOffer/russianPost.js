import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок DefaultOffer
 * @property {PageObject.DefaultOffer} defaultOffer
 */
export default makeSuite('Дисклеймер о Почте России.', {
    story: {
        'По умолчанию': {
            'должен присутствовать': makeCase({
                test() {
                    return this.defaultOffer.russianPost.isExisting().should.eventually.to.equal(
                        true, 'Дисклеймер о Почте России должен присутствовать'
                    );
                },
            }),
        },
    },
});
