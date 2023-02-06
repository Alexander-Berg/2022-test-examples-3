import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок ProductOffersSnippet
 * @param {PageObject.ProductOffersSnippet}
 */
export default makeSuite('Блок минимальной суммы заказа на сниппете оффера.', {
    story: {
        'По умолчанию': {
            'должен присутствовать': makeCase({
                feature: 'Минимальная сумма заказа',
                id: 'm-touch-2400',
                issue: 'MOBMARKET-9830',
                test() {
                    return this.offerSnippet
                        .orderMinCost
                        .isExisting()
                        .should.eventually.to.equal(true, 'Проверяем, что на сниппете есть блок стоимости');
                },
            }),
        },
    },
});
