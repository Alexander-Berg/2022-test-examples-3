import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок NavigationMenu
 * @param {PageObject.MenuTab} headerTab
 */
export default makeSuite('Таб - верхний уровень меню.', {
    feature: 'Навигационное меню',
    story: {
        'По умолчанию': {
            'кликабелен.': makeCase({
                id: 'marketfront-2851',
                issue: 'MARKETVERSTKA-31074',
                async test() {
                    const href = await this.headerTab.getLinkHref();
                    await this.browser.allure.runStep(
                        'Проверяем, что в параметр href задан',
                        () => this.expect(href)
                            .to.be.link(href, {
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
});
