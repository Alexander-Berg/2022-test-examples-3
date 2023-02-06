import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок SideMenu.
 * @param {PageObject.SideMenu} sideMenu
 * @param {PageObject.Header} header
 */
export default makeSuite('Бокового меню авторизованного пользователя с одним аккаунтом.', {
    story: {
        async beforeEach() {
            await this.header.clickMenuTrigger();
            await this.sideMenu.waitForVisible();
        },

        'Пункт меню "Добавить аккаунт"': {
            'отображается': makeCase({
                id: 'm-touch-2757',
                issue: 'MOBMARKET-11999',
                test() {
                    return this.browser.allure.runStep(
                        'Проверяем видимость пункта "Добавить аккаунт"',
                        () => this.sideMenu.addAccount.isVisible()
                    ).should.eventually.be.equal(true, '"Добавить аккаунт" отображается');
                },
            }),

            'содержит ссылку на форму авторизации': makeCase({
                id: 'm-touch-2756',
                issue: 'MOBMARKET-11998',
                test() {
                    this.sideMenu.clickAuthUserInfoContainerItem();
                    return this.sideMenu.getAddAccountUrl()
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
