import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок header2-nav, фича мультиавторизации, когда несколько аккаунтов
 * @param {PageObject.Header2Nav} headerNav
 * @param {PageObject.Index} accountsList
 */
export default makeSuite('Боковое меню пользователя с множеством аккаунтов.', {
    story: {
        async beforeEach() {
            await this.headerNav.clickOpen();
        },

        'Пункт меню "Другие аккаунты"': {
            'отображается': makeCase({
                id: 'marketfront-3436',
                issue: 'MARKETVERSTKA-34302',
                async test() {
                    return this.browser.allure.runStep(
                        'Проверяем видимость пункта "Другие аккаунты"',
                        () => this.headerNav.otherAccounts.isVisible()
                    ).should.eventually.be.equal(true, '"Другие аккаунты" отображается');
                },
            }),

            'при нажатии': {
                'открывает список аккаунтов': makeCase({
                    id: 'marketfront-3437',
                    issue: 'MARKETVERSTKA-34301',
                    async test() {
                        await this.headerNav.clickOtherAccountsItem();
                        return this.accountsList.isContentVisible()
                            .should.eventually.be.equal(true, 'Список аккаунтов отображается');
                    },
                }),
            },
        },
    },
});
