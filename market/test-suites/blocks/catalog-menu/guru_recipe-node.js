import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок catalog-menu: узел дерева с типом guru_recipe.
 * @param {PageObject.NavigationDepartment} navigationDepartment
 * @param {String} params.itemDisplayName отображаемое имя узла - обязателен.
 * @param {String} params.linkParams параметры ссылки.
 */
export default makeSuite('Узел навигационного дерева типа guru_recipe.', {
    environment: 'kadavr',
    feature: 'Навигационное дерево',
    params: {
        itemDisplayName: 'отображаемое имя узла - обязателен',
        linkParams: 'параметры ссылки - обязателен',
    },
    story: {
        'Заголовок.': {
            'По умолчанию': {
                'кликабелен': makeCase({
                    id: 'marketfront-1241',
                    issue: 'MARKETVERSTKA-25896',
                    async test() {
                        const href = await this.navigationDepartment
                            .getDepartmentTitleLink(this.params.itemDisplayName);
                        await this.browser.allure.runStep(
                            'Проверяем, что аттрибут href не пустой',
                            () => this.expect(href).not.to.be.equal(null, 'Атрибут пустой')
                        );
                    },
                }),
                'имеет правильный формат ссылки': makeCase({
                    id: 'marketfront-1250',
                    issue: 'MARKETVERSTKA-25899',
                    async test() {
                        const href = await this.navigationDepartment
                            .getDepartmentTitleLink(this.params.itemDisplayName);
                        await this.browser.allure.runStep(
                            'Проверяем, что в параметрах присутствует nid, hid, slug, glfilter, track ' +
                            'с нужными значениями',
                            () => this.expect(href)
                                .to.be.link({
                                    query: {
                                        hid: this.params.linkParams.hid[0],
                                        glfilter: this.params.linkParams.glfilter[0],
                                    },
                                    pathname: `/catalog--${this.params.slug}/${this.params.linkParams.nid[0]}/list`,
                                }, {
                                    mode: 'match',
                                    skipProtocol: true,
                                    skipHostname: true,
                                    skipPathname: true,
                                })
                        );
                    },
                }),
            },
        },
    },
});
