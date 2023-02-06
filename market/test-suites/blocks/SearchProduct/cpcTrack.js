import {makeSuite, makeCase} from 'ginny';

/**
 * @property {PageObject.SearchProduct} snippet
 */
export default makeSuite('Сниппет продукта типа model c cpc-параметром.', {
    environment: 'kadavr',
    params: {
        cpc: 'Параметр cpc для ссылки на КМ',
    },
    story: {
        'Название.': {
            'По умолчанию': {
                'содержит правильную ссылку со cpc параметром': makeCase({
                    id: 'MARKETFRONT-40195',
                    issue: 'MARKETFRONT-40195',
                    test() {
                        return this.snippet.getSnippetLink()
                            .then(link => this.expect(link).to.be.link({
                                pathname: '/product--.*/\\d+',
                                query: {
                                    cpc: this.params.cpc,
                                },
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            }));
                    },
                }),
            },
        },

        'При клике': {
            'по заголовку': {
                'в репорт должен передаватся cpc': makeCase({
                    id: 'MARKETFRONT-40195',
                    issue: 'MARKETFRONT-40195',
                    async test() {
                        const validRequest = ({request}) => {
                            const {url} = request;
                            return url.includes('place=productoffers') && url.includes(`cpc=${this.params.cpc}`);
                        };

                        const currentTabId = await this.browser.allure.runStep(
                            'Получаем идентификатор текущей вкладки',
                            () => this.browser.getCurrentTabId()
                        );

                        await this.snippet.click();

                        await this.browser.yaWaitForNewTab({
                            startTabIds: [currentTabId],
                            timeout: 2000,
                        });

                        await this.browser.waitUntil(
                            async () => {
                                const request = await this.browser.allure.runStep(
                                    'Проверяем запросы в кадавр',
                                    () => this.browser.getLog('Report').then(logs => logs.find(validRequest))
                                );

                                return Boolean(request);
                            },
                            5000,
                            'Запрос в productoffers с cpc не отправлен.',
                            200
                        );
                    },
                }),
            },
        },
    },
});
