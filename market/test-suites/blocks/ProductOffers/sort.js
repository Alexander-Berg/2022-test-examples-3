import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок ProductOffers/sort.
 * @param {PageObject.ProductOffers} productOffers
 */
export default makeSuite('Блок сортировки.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'должен присутствовать': makeCase({
                id: 'm-touch-1487',
                issue: 'MOBMARKET-4618',
                test() {
                    return this.productOffers.sorting
                        .isExisting()
                        .should.eventually
                        .equal(true, 'Проверяем видимость блока');
                },
            }),
        },
    },
});
