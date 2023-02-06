import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-snippet-card2
 * @param {PageObject.SnippetCard2} snippetCard2
 */
export default makeSuite('Листовой сниппет продукта.', {
    story: {
        'Предупреждение.': {
            'По умолчанию': {
                'должно быть одно': makeCase({
                    test() {
                        return this.snippetCard2
                            .getWarningsCount()
                            .should.eventually.to.be.equal(
                                1,
                                'одно предупреждение в снипете.'
                            );
                    },
                }),
            },
        },
    },
});
