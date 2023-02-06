import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-snippet-card
 * @param {PageObject.SnippetCard} snippetCard
 */
export default makeSuite('Блок минимальной суммы заказа на сниппете оффера.', {
    story: {
        'По умолчанию': {
            'должен присуствовать': makeCase({
                feature: 'Минимальная сумма заказа',
                id: 'marketfront-2914',
                issue: 'MARKETVERSTKA-31255',
                test() {
                    return this.snippetCard
                        .orderMinCost
                        .isExisting()
                        .should.eventually.to.equal(true, 'Блок присутствует на сниппете');
                },
            }),
        },
    },
});
