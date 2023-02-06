import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок NavigationMenu
 * @param {PageObject.Tab} catalogTab
 */
export default makeSuite('Таб - вертикальное меню.', {
    feature: 'Навигационное меню',
    story: {
        'При наведениее': {
            'добавляется класс активности.': makeCase({
                id: 'marketfront-2985',
                issue: 'MARKETVERSTKA-31853',
                async test() {
                    await this.catalogTab.waitForVisible();
                    await this.catalogTab.hoverOverTab();
                    // await this.catalogTab.hover();
                    await this.browser.waitUntil(
                        () => this.catalogTab.isActive(),
                        1000,
                        'Класс активности не добавился'
                    );
                },
            }),
        },
    },
});
