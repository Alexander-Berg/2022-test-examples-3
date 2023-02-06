import {makeSuite, makeCase} from 'ginny';
import schema from 'js-schema';
import nodeConfig from '@self/platform/configs/current/node';
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
                id: 'm-touch-2541',
                issue: 'MOBMARKET-11074',
                test() {
                    return this.subscription.isVisible()
                        .should.eventually.to.be.equal(true, 'Форма подписки отображается на странице');
                },
            }),

            'Если пользователь ввел email и нажал кнопку подписаться': {
                'должно отобразиться благодарственное сообщение': makeCase({
                    id: 'm-touch-2542',
                    issue: 'MOBMARKET-11074',
                    async test() {
                        await this.subscription.setEmail('test@test.ru');
                        await this.subscription.clickSubscribeButton();

                        return this.subscription.waitThanksForVisible()
                            .should.eventually.to.be.equal(true, 'Благодарственное сообщение появилось');
                    },
                }),

                'должна отправиться метрика подписки': makeCase({
                    id: 'm-touch-2543',
                    issue: 'MOBMARKET-11074',
                    async test() {
                        await this.subscription.setEmail('test@test.ru');

                        await this.subscription.clickSubscribeButton();

                        await this.subscription.waitThanksForVisible();

                        const goal = await this.browser.yaGetMetricaGoal(
                            nodeConfig.yaMetrika.market.id,
                            'subscription-landing-type-subscription_subscription-submit',
                            schema({})
                        );

                        return this.expect(Boolean(goal))
                            .be.equal(
                                true,
                                'Цель найдена'
                            );
                    },
                }),
            },

            'Если пользователь ввел некорректный email и нажал кнопку подписаться': {
                'должно отобразиться сообщение об ошибке': makeCase({
                    id: 'm-touch-2544',
                    issue: 'MOBMARKET-11074',
                    async test() {
                        await this.subscription.setEmail('test@test');

                        await this.subscription.clickSubscribeButton();

                        return this.subscription.tooltipError.isVisible()
                            .should.eventually.to.be.equal(true, 'Отображается сообщение о неверном формате адреса');
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

            'должно отображаться сообщение': makeCase({
                id: 'm-touch-2541',
                issue: 'MOBMARKET-11074',
                test() {
                    return this.subscription.thanks.isVisible()
                        .should.eventually.to.be.equal(true, 'Отображается сообщение о подписке');
                },
            }),
        },
    },
});
