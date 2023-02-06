import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты подписки на снижение цены
 * @property {PageObject.ProductSubscriptionPopup} попап подписки на снижение цены
 */
export default makeSuite('Форма успешной подписки.', {
    feature: 'Подписка на снижение цены',
    environment: 'kadavr',
    story: {
        'При вводе известной почты': {
            beforeEach() {
                return this.browser.setState(
                    'marketUtils',
                    {
                        data: {
                            subscriptions: [],
                        },
                        defaultStatusForNewSubscription: 'CONFIRMED',
                    }
                )
                    .then(() => this.browser.yaPageReload(5000, ['state']));
            },
            'После клика на кнопку "Подписаться"': {
                'в попапе показывается сообщение об успешной подписке': makeCase({
                    async test() {
                        await this.openerButton.subscribeButtonClick();
                        await this.productSubscriptionPopup.waitForPopupVisible();
                        await this.productSubscriptionPopup.clickSubscribe();
                        return this.productSubscriptionPopup.waitForConfirmedFormVisible()
                            .should.eventually.to.be.equal(true,
                                'Показалось сообщение об удачном завершении подписки');
                    },
                }),
            },
        },
    },
});
