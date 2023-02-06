import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на чпу ссылки блок MenuCatalog.
 * @param {PageObject.MenuCatalog} menuCatalog
 */
export default makeSuite('MenuCatalog.', {
    story: {
        'Пункт меню "Все категории".': {
            beforeEach() {
                return this.header.clickMenuTrigger()
                    .then(() => this.sideMenu.waitForVisible());
            },
            'По умолчанию': {
                'содержит ЧПУ ссылки на листовые выдачи': makeCase({
                    id: 'm-touch-2513',
                    issue: 'MOBMARKET-10282',
                    async test() {
                        await this.sideMenu.clickCatalogItem();
                        await this.menuCatalog.clickCategoryByIndex(1);
                        await this.menuCatalog.clickInnerCategoryByIndex(2, 2);

                        return this.browser.allure.runStep('Проверяем ссылку категории', () =>
                            this.menuCatalog.getInnerCategoryHref(3, 2)
                                .should.eventually.be.link({
                                    pathname: 'catalog--[\\w-]+/\\d+/list',
                                }, {
                                    mode: 'match',
                                    skipProtocol: true,
                                    skipHostname: true,
                                }));
                    },
                }),
            },
        },
    },
});
