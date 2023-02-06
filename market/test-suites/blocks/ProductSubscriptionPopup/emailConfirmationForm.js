import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты подписки на снижение цены
 * @property {PageObject.ProductSubscriptionPopup} попап подписки на снижение цены
 */
export default makeSuite('Форма подтверждения почты.', {
    feature: 'Подписка на снижение цены',
    environment: 'kadavr',
    story: {
        'При вводе некорректного адреса почты': {
            'отображается соообщение "В поле для ввода введён некорректый адрес электронной почты"': makeCase({
                issue: 'MARKETVERSTKA-33967',
                id: 'marketfront-2655',
                async test() {
                    await this.openerButton.subscribeButtonClick();
                    await this.productSubscriptionPopup.waitForPopupVisible();
                    await this.productSubscriptionPopup.setEmail('stest@yandex');
                    await this.productSubscriptionPopup.clickSubscribe();

                    return this.productSubscriptionPopup.waitForErrorMessageVisible()
                        .should.eventually.to.be.equal(true,
                            'Показалось сообщение об ошибке');
                },
            }),
        },
        'При вводе неизвестной почты': {
            beforeEach() {
                return this.browser.setState(
                    'marketUtils',
                    {
                        data: {
                            subscriptions: [],
                        },
                        defaultStatusForNewSubscription: 'NEED_SEND_CONFIRMATION',
                    }
                )
                    .then(() => this.browser.yaPageReload(5000, ['state']));
            },
            'После клика на кнопку "Подписаться"': {
                'в попапе показывается сообщение об отправленном для подтверждения почты письме': makeCase({
                    async test() {
                        await this.openerButton.subscribeButtonClick();
                        await this.productSubscriptionPopup.waitForPopupVisible();
                        await this.productSubscriptionPopup.setEmail('stest@yandex.ru');
                        await this.productSubscriptionPopup.clickSubscribe();

                        return this.productSubscriptionPopup.waitForEmailConfirmationFormVisible()
                            .should.eventually.to.be.equal(true,
                                'Показалось сообщение о необходимости подтвердить почту');
                    },
                }),
            },
        },
    },
});
