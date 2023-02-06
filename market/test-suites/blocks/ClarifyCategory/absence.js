import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на отсутствие блока ClarifyCategory.
 * @param {PageObject.ClarifyCategory} clarifyCategory
 */
export default makeSuite('Визуальный блок уточнения категорий', {
    feature: 'Отсутствие визуального блока уточнения категорий',
    environment: 'kadavr',
    story: {
        'На страницах, где блока не должно быть': {
            'его нет': makeCase({
                id: 'marketfront-2371',
                issue: 'MARKETVERSTKA-28491',
                test() {
                    return this.clarifyCategory.isExisting()
                        .should.eventually.to.be.equal(false);
                },
            }),
        },
    },
});
