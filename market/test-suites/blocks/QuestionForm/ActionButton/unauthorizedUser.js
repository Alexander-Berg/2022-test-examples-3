import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.QuestionForm} questionForm
 */
export default makeSuite('Блок формы вопроса на товар для неавторизованного пользователя.', {
    story: {
        'Кнопка "Войти".': {
            'По умолчанию': {
                'должна отображаться': makeCase({
                    feature: 'Авторизация',
                    id: 'm-touch-2292',
                    issue: 'MOBMARKET-9113',
                    async test() {
                        await this.questionForm.clickTextField();
                        const hasAuthorizeButton = await this.questionForm.hasAuthorizeButton();

                        return this.expect(hasAuthorizeButton)
                            .to.be.equal(true, 'Кнопка "Войти" присутствует');
                    },
                }),
            },

            'При клике': {
                'ведет на форму авторизации с правильным retpath': makeCase({
                    feature: 'Авторизация',
                    id: 'm-touch-2291',
                    issue: 'MOBMARKET-9112',
                    async test() {
                        const retpath = await this.browser.getUrl();

                        await this.questionForm.clickTextField();
                        await this.questionForm.clickAuthorizeButton();

                        const passportUrl = await this.browser.getUrl();

                        return this.browser.allure.runStep(
                            'Проверяем URL формы авторизации',
                            () => this.expect(passportUrl).to.be.link({
                                hostname: '^passport',
                                query: {
                                    retpath,
                                },
                            }, {
                                mode: 'match',
                                skipPathname: true,
                                skipProtocol: true,
                            })
                        );
                    },
                }),
            },
        },
    },
});
