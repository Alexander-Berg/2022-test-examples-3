import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-snippet-cell2 с уценкой
 * @param {PageObject.SnippetCell2} snippetCell2
 */
export default makeSuite('Гридовый сниппет с уценкой.', {
    feature: 'Уценка',
    story: {
        'Лэйбл "Уценённый товар"': {
            'По умолчанию': {
                'должен присутствовать': makeCase({
                    test() {
                        return this.snippetCell2.isCutpriceLabelExists()
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
