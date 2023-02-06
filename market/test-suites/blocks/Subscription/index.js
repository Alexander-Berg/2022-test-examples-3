import {makeSuite, makeCase} from 'ginny';
import CONFIRMED_ADVERTISING_SUBSCRIPTION from './subscription.mock';

/**
 * Тесты на виджет Subscription.
 * @param {PageObject.Subscription} subscription
 */
export default makeSuite('Блок форма подписки на общую рассылку.', {
    environment: 'kadavr',
    story: {
        'Если пользователь не был подписан': {
            async beforeEach() {
                await this.browser.setState('marketUtils.data.subscriptions', []);
                await this.browser.yaReactPageReload();
            },

            'должен отображаться на странице': makeCase({
                id: 'marketfront-3150',
                issue: 'MARKETVERSTKA-32956',
                test() {
                    return this.subscription.isVisible()
                        .should.eventually.to.be.equal(true, 'Форма подписки отображается на странице');
                },
            }),

            'Если пользователь ввел email и нажал кнопку подписаться': {
                'должно отобразиться благодарственное сообщение': makeCase({
                    id: 'marketfront-3151',
                    issue: 'MARKETVERSTKA-32956',
                    test() {
                        return this.subscription
                            .setEmail('test@test.ru')
                            .then(() => this.subscription.clickSubscribeButton())
                            .then(() => this.subscription.waitThanksForVisible()
                                .should.eventually.to.be.equal(true, 'Благодарственное сообщение появилось'));
                    },
                }),
            },
        },

        'Ecли пользователь уже был подписан': {
            async beforeEach() {
                await this.browser.setState('marketUtils.data.subscriptions', [
                    CONFIRMED_ADVERTISING_SUBSCRIPTION,
                ]);
                await this.browser.yaReactPageReload();
            },

            'не должен отображаться на странице': makeCase({
                id: 'marketfront-3150',
                issue: 'MARKETVERSTKA-32956',
                test() {
                    return this.subscription.isVisible()
                        .should.eventually.to.be.equal(false, 'Форма подписки не отображается на странице');
                },
            }),
        },
    },
});
