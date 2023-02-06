import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-snippet-list
 * @param {PageObject.SnippetList} snippetList
 */
export default makeSuite('Вид выдачи.', {
    story: {
        'по умолчанию': {
            'должен совпадать с переданным': makeCase({
                id: 'marketfront-2724',
                issue: 'MARKETVERSTKA-30640',
                params: {
                    viewtype: 'list или grid',
                },
                async test() {
                    const {viewtype} = this.params;
                    const type = await this.snippetList.getViewType();
                    return this.expect(type).to.be.equal(viewtype, `Тип показа должен быть ${viewtype}`);
                },
            }),
        },

        'При клике': {
            'по заголовку': {
                'должна открываться КМ': makeCase({
                    id: 'marketfront-4184',
                    issue: 'MARKETFRONT-19652',
                    async test() {
                        const currentTabId = await this.browser.allure.runStep(
                            'Получаем идентификатор текущей вкладки',
                            () => this.browser.getCurrentTabId()
                        );

                        await this.snippetCard2.clickTitle();
                        const newTabId = await this.browser.yaWaitForNewTab({
                            startTabIds: [currentTabId],
                            timeout: 2000,
                        });

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
    },
});
