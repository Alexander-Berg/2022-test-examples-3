import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-snippet-card2 с уценкой
 * @param {PageObject.SnippetCard2} snippetCard2
 */
export default makeSuite('Листовой сниппет с уценкой.', {
    feature: 'Уценка',
    story: {
        'Лэйбл "Уценённый товар"': {
            'По умолчанию': {
                'должен присутствовать': makeCase({
                    test() {
                        return this.snippetCard2.isCutpriceLabelExists()
                            .should.eventually.to.be.equal(
                                true,
                                'Лэйбл должен присутствовать.'
                            );
                    },
                }),
            },
        },
    },
});
