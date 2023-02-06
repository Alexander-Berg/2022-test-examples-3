import {makeSuite, makeCase} from 'ginny';

const POPUP_ANIMATION_DELAY = 1000;

/**
 * Тест на появление попапа подписки на снижение цены
 * @property {PageObject.ProductSubscriptionPopup} this.productSubscriptionPopup попап подписки на снижение цены
 * @property {PageObject.AveragePrice} this.averagePrice блок средней цены
 */

export default makeSuite('Попап подписки на снижение цены.', {
    feature: 'Подписка на снижение цены',
    story: {
        'По клику на кнопку "Следить за снижением цены"': {
            'попап появляется.': makeCase({
                id: 'm-touch-2253',
                issue: 'MOBMARKET-8969',
                async test() {
                    await this.averagePrice.clickSubscribeButton();

                    return this.productSubscriptionPopup.waitForPopupVisible()
                        .should.eventually.to.be.equal(true,
                            'Появился попап подписки на снижение цены');
                },
            }),
        },

        'Чекбокс "Узнавать о других скидках и акциях"': {
            beforeEach() {
                return this.browser
                    .allure.runStep(
                        'Чистим куку yaGdprCheck',
                        () => this.browser.deleteCookie('yaGdprCheck')
                    )
                    .yaReactPageReload(10000);
            },

            'присутствует и проставлен по умолчанию.': makeCase({
                id: 'm-touch-2252',
                issue: 'MOBMARKET-8974',

                async test() {
                    await this.averagePrice.clickSubscribeButton();

                    return this.productSubscriptionPopup.getCheckboxState()
                        .should.eventually.to.be.equal(true,
                            'Чекбокс присутствует и проставлен');
                },
            }),
        },

        'Чекбокс "Узнавать о других скидках и акциях" для пользователей с кукой is_eu_test': {
            beforeEach() {
                return this.browser
                    .allure.runStep(
                        'Ставим куку is_eu_test',
                        () => this.browser.yaSetCookie({name: 'is_eu_test', value: '1'})
                    )
                    .yaReactPageReload(10000);
            },

            afterEach() {
                return this.browser.deleteCookie('is_eu_test');
            },

            'присутствует и снят по умолчанию.': makeCase({
                id: 'm-touch-2337',
                issue: 'MOBMARKET-9472',

                async test() {
                    await this.averagePrice.clickSubscribeButton();

                    return this.productSubscriptionPopup.getCheckboxState()
                        .should.eventually.to.be.equal(false,
                            'Чекбокс присутствует и снят');
                },
            }),
        },

        'По клику на кнопку "Не сейчас"': {
            'попап закрывается.': makeCase({
                id: 'm-touch-2248',
                issue: 'MOBMARKET-8970',
                async test() {
                    await this.averagePrice.clickSubscribeButton();

                    await this.productSubscriptionPopup.waitForPopupVisible();

                    /**
                     * Такой хак нужен из-за того, что попап выезжает секунду
                     * и если кликать по кнопке до того, как он выехал, тест падает,
                     * поскольку селениум не может самостоятельно доскроллить до кнопки
                     * и считает, что нажать на неё невозможно
                     */
                    await new Promise(resolve => {
                        setTimeout(() => {
                            resolve(this.productSubscriptionPopup.clickDeffer());
                        }, POPUP_ANIMATION_DELAY);
                    });

                    return this.productSubscriptionPopup.waitForHidden()
                        .should.eventually.to.be.equal(true, 'Попап подписки закрылся');
                },
            }),
        },
    },
});
