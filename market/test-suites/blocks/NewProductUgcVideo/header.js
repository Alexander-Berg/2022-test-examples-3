import {makeSuite, makeCase} from 'ginny';

/**
 * Тест шапки блока добавления UGC видео
 * @param {PageObject.NewProductUgcVideoPage} page
 *
 * @param {Object} params
 * @param {string} params.testPage
 * @param {Object} params.testPageParams
 */
export default makeSuite('Шапка блока добавления видео.', {
    story: {
        'Кнопка "Закрыть"': {
            'при клике': {
                'перенаправляет на предыдущую страницу': makeCase({
                    id: 'm-touch-3404',
                    issue: 'MARKETFRONT-9167',
                    async test() {
                        return this.browser.yaWaitForPageReloadedExtended(
                            () => this.page.clickCloseCross()
                        ).then(() => Promise.all([
                            this.browser.getUrl(),
                            this.browser.yaBuildURL(this.params.testPage, this.params.testPageParams),
                        ])
                        ).then(([openedUrl, expectedPath]) => this
                            .expect(openedUrl, 'Проверяем что произошел переход на правильную страницу')
                            .to.be.link(expectedPath, {
                                skipProtocol: true,
                                skipHostname: true,
                            })
                        );
                    },
                }),
            },
        },
        'Кнопка "Назад"': {
            'при клике': {
                'перенаправляет на предыдущую страницу': makeCase({
                    id: 'm-touch-3410',
                    issue: 'MARKETFRONT-9167',
                    async test() {
                        return this.browser.yaWaitForPageReloadedExtended(
                            () => this.page.clickCloseCornerLeft()
                        ).then(() => Promise.all([
                            this.browser.getUrl(),
                            this.browser.yaBuildURL(this.params.testPage, this.params.testPageParams),
                        ])
                        ).then(([openedUrl, expectedPath]) => this
                            .expect(openedUrl, 'Проверяем что произошел переход на правильную страницу')
                            .to.be.link(expectedPath, {
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
