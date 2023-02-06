import {makeSuite, makeCase, prepareSuite} from 'ginny';

const ClickdaemonSuite = makeSuite('Кликдемон.', {
    story: {
        'Получает нужную ссылку': makeCase({
            async test() {
                const validRequest = ({request}) => request.url.includes(this.params.url);

                await this.browser.waitUntil(
                    async () => {
                        const request = await this.browser.allure.runStep(
                            'Проверяем запросы в кадавр',
                            async () => {
                                const logs = await this.browser.getLog('ClickDaemon');
                                return logs.find(validRequest);
                            }
                        );

                        return Boolean(request);
                    },
                    5000,
                    'Запрос в кликдемон не отправлен.',
                    200
                );
            },
        }),
    },
});

/**
 * @param {PageObject.SearchSnippetCartButton} snippetCartButton
 */
export default makeSuite('Сниппет.', {
    story: {

        'При переходе по ссылке.': prepareSuite(ClickdaemonSuite, {
            hooks: {
                async beforeEach() {
                    return this.snippet.click();
                },
            },
        }),

        'При добавлении в корзину.': prepareSuite(ClickdaemonSuite, {
            hooks: {
                async beforeEach() {
                    return this.snippetCartButton.click();
                },
            },
        }),
    },
});
