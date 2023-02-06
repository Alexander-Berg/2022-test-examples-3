import {makeSuite, makeCase} from 'ginny';

const GDPR_CHECK_COOKIE = 'is_eu_test';

/*
 * Тесты на галку рекламной подписки
 * @property {PageObject.productSummarySubscribe} this.productSummarySubscribe подписка pa_on_sale
 */
export default makeSuite('Подписка на появление в наличии', {
    environment: 'kadavr',
    story: {
        'Чекбокс рекламной подписки при отсутствии подтвержденной рекламной подписки': {
            beforeEach() {
                return this.browser.setState(
                    'marketUtils',
                    {
                        data: {
                            subscriptions: [],
                        },
                    }
                );
            },
            'для обычных пользователей': {
                async beforeEach() {
                    await this.browser.deleteCookie(GDPR_CHECK_COOKIE);
                },

                'включен': makeCase({
                    test() {
                        return this.productSummarySubscribe.isAdvertisingSelected()
                            .should.eventually.to.be.equal(true, 'чекбокс включен');
                    },
                }),
            },
            'для попадающих под GDPR пользователей': {
                async beforeEach() {
                    await this.browser
                        .yaSetCookie({name: GDPR_CHECK_COOKIE, value: '1'})
                        .then(() => this.browser.yaPageReload(5000, ['state']));
                },

                async afterEach() {
                    await this.browser.deleteCookie(GDPR_CHECK_COOKIE);
                },

                'выключен': makeCase({
                    test() {
                        return this.productSummarySubscribe.isAdvertisingSelected()
                            .should.eventually.to.be.equal(false, 'чекбокс выключен');
                    },
                }),

                'при подписке на появление в наличии не приводит к созданию рекламной подписки': makeCase({
                    async test() {
                        await this.productSummarySubscribe.setEmail('somecooltestemail999@yandex.ru');
                        await this.productSummarySubscribe.clickSubscribe();

                        return this.productSummarySubscribe.cancelForm.isVisible()
                            .should.eventually.be.equal(true, 'рекламная подписка не создана');
                    },
                }),
            },
        },
        'Чекбокс рекламной подписки при наличии рекламной подписки для всех пользователей': {
            async beforeEach() {
                await this.browser.setState(
                    'marketUtils',
                    {
                        data: {
                            subscriptions: [{
                                subscriptionType: 'ADVERTISING',
                                subscriptionStatus: 'CONFIRMED',
                            }],
                        },
                        defaultStatusForNewSubscription: 'CONFIRMED',
                    }
                );

                return this.browser.yaPageReload(5000, ['state']);
            },

            'отсутствует': makeCase({
                test() {
                    return this.productSummarySubscribe.isAdvertisingExists()
                        .should.eventually.to.be.equal(false, 'рекламная подписка отсутствует');
                },
            }),
        },
    },
});
