import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок Linker.
 * @param {PageObject.Linker|PageObject.PopularRecipes} linker
 */
export default makeSuite('Блок перелинковки. Кнопка «Ещё».', {
    feature: 'Блок перелинковки',
    story: {
        'При большом кол-ве рецептов': {
            'видна кнопка «Ещё»': makeCase({
                id: 'marketfront-1756',
                issue: 'MARKETVERSTKA-26382',
                test() {
                    return this.linker.showMore.isVisible()
                        .should.eventually.to.be.equal(true, 'Кнопка «Показать ещё» видна');
                },
            }),
        },
    },
});
