import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на проверку слагов при переходе на создание нового отзыва и обратно.
 */
export default makeSuite('Проверка ссылки на слаг при добавлении отзыва и переходе назад', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'при переходах правильно формируются ссылки': makeCase({
                meta: {
                    id: 'm-touch-2624',
                    issue: 'MOBMARKET-9940',
                },
                async test() {
                    await this.browser.allure.runStep(
                        'Кликаем по ссылке добавления нового отзыва',
                        () => this.newReviewLink.click()
                    );

                    const {selector: subpageHeaderSelector} = await this.subpageHeader.root;
                    await this.browser.allure.runStep(
                        'Ждем пока появится страница добавления отзыва',
                        () => this.browser.waitUntil(
                            () => this.browser.isVisible(subpageHeaderSelector),
                            10000
                        )
                    );

                    let currentUrl = await this.browser.allure.runStep(
                        'Берём ссылку из урла страницы',
                        () => this.browser.getUrl()
                    );

                    await this.expect(currentUrl, 'Ссылка на странице добавления отзыва не должна содержать слаг')
                        .to.be.link({
                            pathname: 'product\\/[\\d]+\\/reviews/add',
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });

                    await this.browser.allure.runStep(
                        'Кликаем по кнопке закрытия страницы',
                        () => this.subpageHeader.rightControl.click()
                    );

                    currentUrl = await this.browser.allure.runStep(
                        'Берём ссылку из урла страницы',
                        () => this.browser.getUrl()
                    );

                    return this.expect(currentUrl, 'После закрытия диалогого окна ссылка должна содержать слаг')
                        .to.be.link({
                            pathname: 'product--[\\w-]+\\/[\\d]+(\\/.*)?',
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
