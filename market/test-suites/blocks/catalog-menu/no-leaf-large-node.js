import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок catalog-menu: нелистовой узел дерева (isLeaf: false, hasPromo: false, navnodes >= 8).
 * @param {PageObject.NavigationDepartment} navigationDepartment
 * @param {String} params.nodeDisplayName отображаемое имя узла - обязателен.
 * @param {Number} params.collapsedItemCount количество элементов в свернутом виде - обязателен
 * @param {Number} params.expandedItemCount количество элементов в развернутом виде - обязателен
 */
export default makeSuite('Нелистовой узел навигационного дерева (без промо, больше 8 подкатегорий).', {
    feature: 'Навигационное дерево',
    environment: 'kadavr',
    params: {
        itemDisplayName: 'отображаемое имя узла - обязателен',
        collapsedItemCount: 'количество элементов в свернутом виде - обязателен',
        expandedItemCount: 'количество элементов в развернутом виде - обязателен',
    },
    story: {
        'Заголовок.': {
            'При нажатии': {
                'раскрывает список подкатегорий': makeCase({
                    id: 'marketfront-1233',
                    issue: 'MARKETVERSTKA-25885',
                    async test() {
                        // eslint-disable-next-line no-unreachable
                        const {nodeDisplayName} = this.params;
                        const href = await this.navigationDepartment.getDepartmentTitleLink(nodeDisplayName);
                        await this.browser.allure.runStep(
                            `Проверяем, что атрибут href в заголовке "${nodeDisplayName}" существует`,
                            () => this.expect(href).to.be.not.equal(null, 'Атрибут пустой')
                        );

                        let itemCount = await this.navigationDepartment.getSubItemLinkCount();
                        await this.browser.allure.runStep(
                            `Проверяем, что список подкатегорий категории "${nodeDisplayName}" свернут` +
                            `(${this.params.collapsedItemCount} элементов)`,
                            () => this.expect(itemCount).to.be.equal(this.params.collapsedItemCount)
                        );

                        await this.navigationDepartment.clickToggleMore();

                        itemCount = await this.navigationDepartment.getSubItemLinkCount();
                        await this.browser.allure.runStep(
                            `Проверяем, что список подкатегорий категории "${nodeDisplayName}" раскрыт` +
                            `(${this.params.expandedItemCount} элементов)`,
                            () => this.expect(itemCount).to.be.equal(this.params.expandedItemCount)
                        );
                    },
                }),
            },
        },
    },
});
