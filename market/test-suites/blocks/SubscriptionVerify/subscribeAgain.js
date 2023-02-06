import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.SubscriptionVerify} subscriptionVerify
 * @param {PageObject.UnsubscribeReasons} unsubscribeReasons
 */
export default makeSuite('Информация о проверке подписки.', {
    story: {
        'По умолчанию': {
            'должна отображаться': makeCase({
                test() {
                    return this.browser.allure.runStep('Проверям видимость блока', () =>
                        this.subscriptionVerify.waitForSubscribeAgainButtonVisible()
                            .should.eventually.to.be.equal(true, 'Блок отображается')
                    );
                },
            }),
        },
        'При клике': {
            'появляется сообщение об успешной подписке': makeCase({
                async test() {
                    await this.subscriptionVerify.waitForSubscribeAgainButtonVisible();

                    await this.subscriptionVerify.clickSubscribeAgainButton();

                    return this.browser.allure.runStep('Проверям видимость блока', () =>
                        this.unsubscribeReasons.waitForThanksMessageVisible()
                            .should.eventually.to.be.equal(true,
                                'Отобразилсоь благодарственное сообщение')
                    );
                },
            }),
        },
    },
});
