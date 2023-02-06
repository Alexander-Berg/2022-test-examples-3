import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок header2-nav, фича мультиавторизации, когда один аккаунт
 * @param {PageObject.Header2Nav} headerNav
 */
export default makeSuite('Боковое меню пользователя с одним аккаунтом.', {
    story: {
        async beforeEach() {
            await this.headerNav.clickOpen();
        },

        'Пункт меню "Добавить аккаунт"': {
            'отображается': makeCase({
                id: 'marketfront-3434',
                issue: 'MARKETVERSTKA-34304',
                test() {
                    return this.browser.allure.runStep(
                        'Проверяем видимость пункта "Добавить аккаунт"',
                        () => this.headerNav.addAccount.isVisible()
                    ).should.eventually.be.equal(true, '"Добавить аккаунт" отображается');
                },
            }),

            'содержит ссылку на форму авторизации': makeCase({
                id: 'marketfront-3435',
                issue: 'MARKETVERSTKA-34303',
                test() {
                    return this.headerNav.getAddAccountUrl()
                        .should.eventually.be.link({
                            hostname: 'passport-rc.yandex.ru',
                            pathname: '/passport',
                            query: {
                                mode: 'add-user',
                            },
                        }, {
                            skipProtocol: true,
                        });
                },
            }),
        },
    },
});
