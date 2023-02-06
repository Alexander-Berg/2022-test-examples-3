import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-snippet-cell2 cо cpc-параметром
 * @param {PageObject.SnippetCell2} snippetCell2
 */
export default makeSuite('Гридовый сниппет продукта типа model c cpc-параметром.', {
    environment: 'kadavr',
    params: {
        cpc: 'Параметр cpc для ссылки на КМ',
    },
    story: {
        beforeEach() {
            return this.snippetCell2
                .getEntityParams()
                .then(params => {
                    if (params.type !== 'model') {
                        // eslint-disable-next-line market/ginny/no-skip
                        return this.skip(`Тест должен выполняться только для моделей.
                            Тип сущности ${params.type}.`);
                    }

                    this.params = {
                        ...this.params,
                        ...params,
                    };

                    return undefined;
                });
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

                        await this.snippetCell2.clickTitle();

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
