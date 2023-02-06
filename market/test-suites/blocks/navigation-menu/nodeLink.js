import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок NavigationMenu
 * @param {PageObject.NavigationMenuNodeLinkGroup} nodeLinkGroup
 */
export default makeSuite('Блок категорий.', {
    feature: 'Навигационное меню',
    story: {
        'По умолчанию': {
            'имеет один заголовок блока категорий.': makeCase({
                id: 'marketfront-2861',
                issue: 'MARKETVERSTKA-31077',
                async test() {
                    const categoryHeaderLinks = await this.nodeLinkGroup.categoryHeaderLinks;
                    await this.browser.allure.runStep(
                        'Проверяем, что в блоке категорий только один заголовок',
                        () => this.expect(categoryHeaderLinks.value.length).equal(1)
                    );
                },
            }),
            'заголовок блока категорий кликабелен.': makeCase({
                id: 'marketfront-2862',
                issue: 'MARKETVERSTKA-31078',
                severity: 'critical',
                async test() {
                    const href = await this.nodeLinkGroup.getCategoryHeaderLinkHref();
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
            'имеет более одной категории.': makeCase({
                id: 'marketfront-2861',
                issue: 'MARKETVERSTKA-31077',
                async test() {
                    const categoryLinks = await this.nodeLinkGroup.categoryLinks;
                    await this.browser.allure.runStep(
                        'Проверяем, что в блоке категорий более одной категории',
                        () => this.expect(categoryLinks.value.length).above(1)
                    );
                },
            }),
            'категории кликабелены.': makeCase({
                id: 'marketfront-2862',
                issue: 'MARKETVERSTKA-31078',
                severity: 'critical',
                async test() {
                    await this.nodeLinkGroup.waitForVisible();

                    const hrefs = await this.nodeLinkGroup.getCategoryLinkHrefs();

                    await this.browser.allure.runStep(
                        'Проверяем, что в параметр href задан',
                        () => hrefs.map(
                            href => this.expect(href).to.be.link(href, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                                skipPathname: true,
                            })
                        )
                    );
                },
            }),
        },
    },
});
