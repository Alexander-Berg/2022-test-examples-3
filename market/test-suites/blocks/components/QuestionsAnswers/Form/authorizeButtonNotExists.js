import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 */
export default makeSuite('Форма добавления, кнопка «Войти» отсутствует', {
    feature: 'Авторизация',
    story: {
        'По умолчанию': {
            'отсутствует': makeCase({
                async test() {
                    await this.form.clickTextarea();
                    const isVisible = await this.form.isAuthorizeButtonVisible();
                    await this.expect(isVisible).to.equal(false, 'Кнопки «Войти» нет на странице');
                },
            }),
        },
    },
});
