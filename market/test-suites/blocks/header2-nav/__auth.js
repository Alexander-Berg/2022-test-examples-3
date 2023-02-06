import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на кнопку "Войти" блока header2-nav
 *
 * @param {PageObject.Header2Nav} headerNav
 */

export default makeSuite('Кнопка "Войти".', {
    environment: 'testing',
    id: 'marketfront-2743',
    story: {
        'По умолчанию': {
            'содержится на странице': makeCase({
                test() {
                    return this.headerNav.loginButton.isVisible()
                        .should.eventually.equal(true, 'Кнопка "Войти" видна');
                },
            }),

            'содержит ссылку на форму авторизации': makeCase({
                test() {
                    return this.headerNav.getLoginButtonUrl()
                        .should.eventually.be.link({
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
