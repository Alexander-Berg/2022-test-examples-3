import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок AccountsList.
 * @param {PageObject.SideMenu} sideMenu
 * @param {PageObject.Header} header
 * @param {PageObject.AccountsList} accountsList
 * @param {PageObject.AccountRow} defaultAccountRow активный аккаунт
 * @param {PageObject.AccountRow} otherAccountRow неактивный аккаунт
 */
export default makeSuite('Список аккаунтов.', {
    story: {
        async beforeEach() {
            await this.header.clickMenuTrigger();
            await this.sideMenu.waitForVisible();
            await this.sideMenu.clickOtherAccountsItem();
        },

        'Кнопка "Другие аккаунты"': {
            'содержит ссылку на форму авторизации': makeCase({
                id: 'm-touch-2762',
                issue: 'MOBMARKET-12003',
                test() {
                    return this.accountsList.getAddUserUrl()
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

        'Активный аккаунт.': {
            'Кнопка "Выйти"': {
                'содержит ссылку на разлогин в паспорт': makeCase({
                    id: 'm-touch-2764',
                    issue: 'MOBMARKET-12005',
                    params: {
                        defaultUid: 'uid активного аккуанта',
                    },
                    test() {
                        return this.defaultAccountRow.getLogoutUrl()
                            .should.eventually.be.link({
                                hostname: 'passport-rc.yandex.ru',
                                pathname: '/passport',
                                query: {
                                    mode: 'embeddedauth',
                                    action: 'logout',
                                    uid: this.params.defaultUid,
                                },
                            }, {
                                skipProtocol: true,
                            });
                    },
                }),
            },

            'По умолчанию': {
                'отмечена зеленым кружком': makeCase({
                    id: 'm-touch-2763',
                    issue: 'MOBMARKET-12004',
                    test() {
                        return this.defaultAccountRow.isDefaultIconVisible()
                            .should.eventually.be.equal(true, 'Иконка активного аккаунта отображается');
                    },
                }),
            },
        },

        'Неактивный аккаунт.': {
            'По умолчанию': {
                'содержит ссылку на переключение активного аккаунта': makeCase({
                    id: 'm-touch-2760',
                    issue: 'MOBMARKET-12002',
                    params: {
                        otherUid: 'uid неактивного аккуанта',
                    },
                    test() {
                        return this.otherAccountRow.getLoginUrl()
                            .should.eventually.be.link({
                                hostname: 'passport-rc.yandex.ru',
                                pathname: '/passport',
                                query: {
                                    mode: 'embeddedauth',
                                    action: 'change_default',
                                    uid: this.params.otherUid,
                                },
                            }, {
                                skipProtocol: true,
                            });
                    },
                }),
            },
        },
    },
});
