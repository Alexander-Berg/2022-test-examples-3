import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-snippet-card2
 * @param {PageObject.SnippetCard2} snippetCard2
 */
export default makeSuite('Листовой сниппет продукта.', {
    params: {
        id: 'ID продукта, на котором отображена цена (оффера или модели)',
    },
    story: {
        'Цена': {
            'По умолчанию': {
                'должна присутствовать': makeCase({
                    id: 'marketfront-1266',
                    test() {
                        return this.snippetCard2.mainPrice
                            .isExisting()
                            .should.eventually.to.be.equal(
                                true,
                                'Цена должна быть видна.'
                            );
                    },
                }),
            },
        },
    },
});
