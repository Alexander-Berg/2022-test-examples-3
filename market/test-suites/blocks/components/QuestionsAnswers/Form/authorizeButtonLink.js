import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 */
export default makeSuite('Форма добавления, ссылка кнопки «Войти»', {
    feature: 'Авторизация',
    story: {
        'По умолчанию': {
            'ведёт на авторизацию в паспорт': makeCase({
                async test() {
                    await this.form.clickTextarea();
                    await this.form.clickAuthorizeButton();
                    const url = await this.browser.getUrl();
                    await this.expect(url).to.be.link({
                        hostname: '^passport',
                        pathname: 'auth',
                    }, {
                        mode: 'match',
                        skipProtocol: true,
                        skipQuery: true,
                    });
                },
            }),
        },
    },
});
