import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок PromoNavigationTree
 * @param {PageObject.PromoNavigationTree} promoNavigationTree
 */
export default makeSuite('Категорийное дерево промо-хаба.', {
    story: {
        'по клику на департамент': {
            'раскрывает дочерние узлы': makeCase({
                issue: 'MARKETVERSTKA-34074',
                id: 'marketfront-3406',
                async test() {
                    await this.promoNavigationTree.clickDepartment(2);
                    const isChildrenVisible = await this.promoNavigationTree.waitForDepartmentChildrenVisible(2);

                    return this.expect(isChildrenVisible).to.equal(true, 'Дочерние узлы отображаются');
                },
            }),
        },
    },
});
