import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок product-card.
 * @param {PageObject.ProductCard} productCard
 * @param {PageObject.ProductCardHeader} productCardHeader
 */
export default makeSuite('Визитка.', {
    story: {
        'По умолчанию': {
            'должна присутствовать': makeCase({
                id: 'm-touch-1320',
                issue: 'MOBMARKET-4599',
                test() {
                    return this.productCard
                        .isExisting()
                        .should.eventually.to.equal(true, 'Проверяем, что визитка присутствует на странице');
                },
            }),

            'заголовок h1': {
                'должен быть один на странице': makeCase({
                    id: 'm-touch-1320',
                    issue: 'MOBMARKET-4599',
                    test() {
                        return this.productCard
                            .getAllTitlesCount()
                            .should.eventually.to.equal(1, 'Проверяем общее количество заголовков h1 на странице');
                    },
                }),

                'должен соответствовать ожидаемому': makeCase({
                    id: 'm-touch-1320',
                    issue: 'MOBMARKET-4599',
                    params: {
                        expectedTitle: 'Ожидаемый заголовок h1 визитки',
                    },
                    test() {
                        return this.productCard
                            .title
                            .getText()
                            .should.eventually.to.equal(this.params.expectedTitle, 'Проверяем заголовок h1');
                    },
                }),
            },
        },
    },
});
