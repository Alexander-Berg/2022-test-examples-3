import {makeSuite, makeCase} from 'ginny';

const POPUP_ANIMATION_DELAY = 1000;

const TEST_EMAIL = 'testmarketby@yandex.ru';

/**
 * Тесты подписки на снижение цены
 * @property {PageObject.ProductSubscriptionPopup} попап подписки на снижение цены
 * @property {PageObject.AveragePrice} this.averagePrice блок средней цены
 */
export default makeSuite('Форма подтверждения почты.', {
    feature: 'Подписка на снижение цены',
    environment: 'kadavr',
    story: {
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
                    .then(() => this.browser.yaReactPageReload());
            },
            'после клика на кнопку "Подписаться"': {
                async beforeEach() {
                    await this.averagePrice.clickSubscribeButton();

                    await this.productSubscriptionPopup.waitForPopupVisible();

                    /**
                     * На всякий случай ждём, пока попап полностью появится.
                     * Если предпринимать действия до того, как он выехал,
                     * селениум может упасть из-за невозможности доскроллить до элемента
                     */
                    await new Promise(resolve => {
                        setTimeout(() => {
                            resolve(this.productSubscriptionPopup.setEmail(TEST_EMAIL));
                        }, POPUP_ANIMATION_DELAY);
                    });

                    await this.productSubscriptionPopup.clickSubscribe();
                },

                'в попапе показывается сообщение об отправленном для подтверждения почты письме.': makeCase({
                    id: 'm-touch-2357',
                    issue: 'MOBMARKET-9535',
                    async test() {
                        return this.needForEmailConfirmationForm.waitForEmailConfirmationFormVisible()
                            .should.eventually.to.be.equal(true,
                                'Показалось сообщение о необходимости подтвердить почту');
                    },
                }),

                'По клику на кнопку "Хорошо" попап закрывается.': {
                    id: 'm-touch-2251',
                    issue: 'MOBMARKET-8973',
                    async test() {
                        await this.needForEmailConfirmationForm.waitForEmailConfirmationFormVisible();

                        await this.needForEmailConfirmationForm.clickOkay();

                        return this.needForEmailConfirmationForm.waitForHidden()
                            .should.eventually.to.be.equal(true, 'Попап закрылся');
                    },
                },
            },
        },
    },
});
