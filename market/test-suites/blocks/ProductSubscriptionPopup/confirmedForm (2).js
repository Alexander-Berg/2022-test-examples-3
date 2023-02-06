import {makeSuite, makeCase} from 'ginny';

const POPUP_ANIMATION_DELAY = 1000;

/**
 * Тесты подписки на снижение цены
 * @property {PageObject.ProductSubscriptionPopup} попап подписки на снижение цены
 * @property {PageObject.AveragePrice} this.averagePrice блок средней цены
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
                    .then(() => this.browser.yaReactPageReload());
            },
            'после клика на кнопку "Подписаться"': {
                'в попапе показывается сообщение об успешной подписке.': makeCase({
                    async test() {
                        await this.averagePrice.clickSubscribeButton();

                        await this.productSubscriptionPopup.waitForPopupVisible();

                        /**
                         * На всякий случай ждём, пока попап полностью появится.
                         * Если предпринимать действия до того, как он выехал,
                         * селениум может упасть из-за невозможности доскроллить до элемента
                         */
                        await new Promise(resolve => {
                            setTimeout(() => {
                                resolve(this.productSubscriptionPopup.clickSubscribe());
                            }, POPUP_ANIMATION_DELAY);
                        });

                        return this.productSubscriptionPopup.waitForConfirmedFormVisible()
                            .should.eventually.to.be.equal(true,
                                'Показалось сообщение об удачном завершении подписки');
                    },
                }),
            },
        },
    },
});
