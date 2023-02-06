import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок NavigationMenu
 * @param {PageObject.NavigationMenu} navigationMenu
 */
export default makeSuite('Заголовок департамента.', {
    feature: 'Навигационное меню',
    story: {
        'По умолчанию': {
            'кликабелен.': makeCase({
                id: 'marketfront-2863',
                issue: 'MARKETVERSTKA-31079',
                async test() {
                    const href = await this.navigationMenu.getTitleHref();
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
