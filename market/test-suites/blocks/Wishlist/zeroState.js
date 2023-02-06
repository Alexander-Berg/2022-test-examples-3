import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ZeroState} zeroState
 */
export default makeSuite('Пустой вишлист', {
    story: {
        'Неавторизованный пользователь': {
            'Кнопка "Войти"': {
                'видима': makeCase({
                    id: 'm-touch-2772',
                    issue: 'MOBMARKET-12140',

                    async test() {
                        await this.zeroState.waitForVisible();

                        return this.zeroState.isLoginLinkVisible()
                            .should.eventually.to.be.equal(true, ' Кнопка "Войти" отображается');
                    },
                }),
                'при клике ведет на страницу авторизации': makeCase({
                    id: 'm-touch-2771',
                    issue: 'MOBMARKET-12141',

                    async test() {
                        await this.zeroState.waitForVisible();

                        await this.zeroState.clickLoginLink();

                        return this.browser.yaParseUrl()
                            .should.eventually.be.link({
                                hostname: 'passport-rc.yandex.ru',
                                pathname: '/auth',
                            }, {
                                skipProtocol: true,
                            });
                    },
                }),
            },
        },
    },
});
