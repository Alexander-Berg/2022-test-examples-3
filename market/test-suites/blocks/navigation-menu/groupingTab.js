import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок NavigationMenu
 * @param {PageObject.NavigationMenu} firstNavigationMenu
 * @param {PageObject.CatalogTab} catalogTab
 */
export default makeSuite('Кнопка "Каталог".', {
    feature: 'Навигационное меню',
    story: {
        'По умолчанию': {
            'меню появляется при первом клике и скрывается при втором': makeCase({
                id: 'marketfront-2812',
                issue: 'MARKETVERSTKA-31070',
                async test() {
                    await this.headerCatalogEntrypoint.clickCatalogAndWaitForVisible();
                    await this.headerCatalog.isVisible()
                        .should.eventually.be.equal(true, 'Попап с каталогом отображается');
                    await this.headerCatalogEntrypoint.clickCatalog();
                    return this.headerCatalog.isVisible()
                        .should.eventually.be.equal(false, 'Каталог скрыт');
                },
            }),
            'заголовок выпадашки совпадает с названием первого в вертикальном меню узла': makeCase({
                id: 'marketfront-2809',
                issue: 'MARKETVERSTKA-31067',
                async test() {
                    await this.headerCatalogEntrypoint.clickCatalogAndWaitForVisible();
                    await this.firstNavigationMenu.waitForVisible();

                    const titleText = await this.firstNavigationMenu.getTitleText();
                    const verticalMenuItemText = await this.catalogTab.getTabText();

                    return this.expect(titleText)
                        .to.be.equal(verticalMenuItemText,
                            'Заголовок выпадашки совпадает с названием первого в вертикальном меню узла'
                        );
                },
            }),
        },
    },
});
