import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.QuestionForm} questionForm
 */
export default makeSuite('Блок формы вопроса на товар для авторизованного пользователя.', {
    story: {
        'Кнопка "Войти".': {
            'По умолчанию': {
                'не должна отображаться': makeCase({
                    feature: 'Авторизация',
                    id: 'm-touch-2293',
                    issue: 'MOBMARKET-9114',
                    async test() {
                        await this.questionForm.clickTextField();
                        const hasAuthorizeButton = await this.questionForm.hasAuthorizeButton();

                        return this.expect(hasAuthorizeButton)
                            .to.be.equal(false, 'Кнопка "Войти" отсутствует');
                    },
                }),
            },
        },
    },
});
