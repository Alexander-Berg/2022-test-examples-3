import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок n-compare-head.
 * @param {PageObject.CompareHead} compareHead
 */
export default makeSuite('Список товаров для сравнения', {
    environment: 'kadavr',
    id: 'marketfront-28',
    issue: 'MARKETVERSTKA-23789',
    feature: 'Сниппет ко/км',
    story: {
        'Список сравнения': {
            'при удалении товара': {
                'уменьшается на 1': makeCase({
                    test() {
                        return this.compareHead.getCellsInside()
                            .then(result => result.value.length)
                            .should.eventually.to.be.equal(2, 'Изначально в сравнении должно быть два товара')
                            .then(() => this.compareCell.getSelector())
                            .then(selector => this.browser.moveToObject(selector))
                            .then(() => this.browser.yaWaitForPageReloadedExtended(
                                () => this.compareHead.clickOnRemove(),
                                20000
                            ))
                            .then(() => this.compareHead.getCellsInside())
                            .then(result => result.value.length)
                            // FIXME: тест должен быть относительным. Надо сравнивать на разницу
                            // FIXME: того, что было и того что стало
                            .should.eventually.to.be.equal(1, 'Количество товаров должно уменьшиться до единицы');
                    },
                }),
            },
        },
    },
});
