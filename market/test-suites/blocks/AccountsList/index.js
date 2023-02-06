import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок AccountsList.
 * @param {PageObject.Header2Nav} headerNav
 * @param {PageObject.Header} header
 * @param {PageObject.Index} accountsList
 * @param {PageObject.AccountRow} defaultAccountRow активный аккаунт
 * @param {PageObject.AccountRow} otherAccountRow неактивный аккаунт
 */
export default makeSuite('Список аккаунтов.', {
    story: {
        async beforeEach() {
            await this.headerNav.clickOpen();
            await this.headerNav.clickOtherAccountsItem();
        },

        'Кнопка "Добавить аккаунт"': {
            'содержит ссылку на форму авторизации': makeCase({
                id: 'marketfront-3439',
                issue: 'MARKETVERSTKA-34300',
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
                    id: 'marketfront-3441',
                    issue: 'MARKETVERSTKA-34305',
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
                'отмечен зеленым кружком': makeCase({
                    id: 'marketfront-3440',
                    issue: 'MARKETVERSTKA-34306',
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
                    id: 'marketfront-3438',
                    issue: 'MARKETVERSTKA-34299',
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
