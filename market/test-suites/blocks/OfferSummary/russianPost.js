import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок OfferSummary
 * @property {PageObject.OfferSummary} offerSummary
 */
export default makeSuite('Дисклеймер о Почте России.', {
    story: {
        'По умолчанию': {
            'должен присутствовать': makeCase({
                test() {
                    return this.offerSummary.russianPost.isExisting().should.eventually.to.equal(
                        true, 'Дисклеймер о Почте России должен присутствовать'
                    );
                },
            }),
        },
    },
});
