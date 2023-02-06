import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.SearchProduct | PageObject.SearchProductTile} snippet
 */
export default makeSuite('Сниппет продукта.', {
    story: {
        'При клике': {
            'должна открываться КМ': makeCase({
                id: 'm-touch-2025',
                issue: 'MOBMARKET-7811',
                async test() {
                    const currentTabId = await this.browser.allure.runStep(
                        'Получаем идентификатор текущей вкладки',
                        () => this.browser.getCurrentTabId()
                    );
                    await this.snippet.click();
                    const newTabId = await this.browser.yaWaitForNewTab({startTabIds: [currentTabId], timeout: 2000});

                    await this.browser.allure.runStep(
                        'Переключаем вкладку на только что открытую вкладку карточки продукта',
                        () => this.browser.switchTab(newTabId)
                    );

                    const productUrl = await this.browser.getUrl();

                    await this.browser.close();
                    await this.browser.allure.runStep(
                        'Переключаем вкладку на начальную',
                        () => this.browser.switchTab(currentTabId)
                    );

                    return this.browser.allure.runStep(
                        'Проверяем, что url открытой вкладки соответствовал продуктовому url',
                        () => Promise.resolve(productUrl).should.eventually.be.link({
                            pathname: 'product--.*/\\d+',
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        })
                    );
                },
            }),
        },
    },
});
