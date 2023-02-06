import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок catalog-menu: виртуальный узел дерева (isLeaf: false, hasPromo: false, navnodes < 5).
 * @param {PageObject.NavigationDepartment} navigationDepartment
 * @param {String} params.itemDisplayName отображаемое имя узла - обязателен.
 */
export default makeSuite('Виртуальный узел навигационного дерева.', {
    feature: 'Навигационное дерево',
    environment: 'kadavr',
    params: {
        itemDisplayName: 'отображаемое имя узла - обязателен',
    },
    story: {
        'Заголовок.': {
            'По умолчанию': {
                'кликабелен': makeCase({
                    id: 'marketfront-1241',
                    issue: 'MARKETVERSTKA-25896',
                    async test() {
                        const link = await this.navigationDepartment
                            .getDepartmentTitleLink(this.params.itemDisplayName);

                        return this.expect(link).to.be.link({
                            pathname: '/catalog--.*/\\d+',
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
            },
        },
    },
});
