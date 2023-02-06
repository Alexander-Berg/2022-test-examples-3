import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 */
export default makeSuite('Форма добавления, кнопка «Войти» присутствует', {
    feature: 'Авторизация',
    story: {
        'По умолчанию': {
            'присутствует': makeCase({
                async test() {
                    await this.form.clickTextarea();
                    const isVisible = await this.form.isAuthorizeButtonVisible();
                    await this.expect(isVisible).to.equal(true, 'Кнопка «Войти» есть на странице');
                },
            }),
        },
    },
});
