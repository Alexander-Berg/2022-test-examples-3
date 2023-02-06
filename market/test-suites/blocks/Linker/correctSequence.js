import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок Linker.
 * @param {PageObject.Linker} linker
 */
export default makeSuite('Блок перелинковки. Правильная последовательность.', {
    feature: 'Блок перелинковки',
    story: {
        'По умолчанию': {
            'рецепты должны идти в последовательности, в которой отдал бэк': makeCase({
                id: 'marketfront-2871',
                issue: 'MARKETVERSTKA-29644',
                params: {
                    recipes: 'Рецепты',
                },
                test() {
                    return this.linker.getLinksName()
                        .should.eventually.to.be.deep.equal(
                            this.params.recipes,
                            'Ссылки расположены в порядке, в котором были получены'
                        );
                },
            }),
        },
    },
});
