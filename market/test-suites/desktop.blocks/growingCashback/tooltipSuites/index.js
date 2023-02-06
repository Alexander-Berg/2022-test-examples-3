import {makeSuite, prepareSuite, mergeSuites, makeCase} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import YandexPlusPromoOfferTooltip from
    '@self/root/src/widgets/content/PromoTooltip/common/yandexPlus/components/YandexPlusPromoTooltip/__pageObject';
import {
    growingCashbackPerk,
    growingCashbackFullRewardPerk,
} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';

const YANDEX_HELP_COOKIE = 'yandex_help';

async function prepareState({isPromoAvailable, isGotFullReward}) {
    let perks = [];

    if (isPromoAvailable) {
        perks = isGotFullReward ? [growingCashbackFullRewardPerk] : [growingCashbackPerk];
    }

    await this.browser.setState('Loyalty.collections.perks', perks);

    // Ставим куку что бы попап онбординга Помощи рядом не открывался и не загораживал попап нотификации
    await this.browser.yaSetCookie({
        name: YANDEX_HELP_COOKIE,
        value: '1',
    });

    await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART);
}

const suite = makeSuite('Тултип акции "Растущий кешбэк".', {
    params: {
        shouldBeShown: 'Должен ли показываться тултип',
    },
    story: {
        'Содержит корректный контент': makeCase({
            async test() {
                await this.browser.allure.runStep('Проверяем, отображение на странице',
                    () => this.growingCashbackTooltip.waitForVisible(!this.params.shouldBeShown)
                );

                if (this.params.shouldBeShown) {
                    await this.browser.allure.runStep(
                        'Проверяем текст заголовка',
                        () => this.growingCashbackTooltip.getTitleText()
                            .should.eventually.to.be.equal(
                                'Дарим 1 550 баллов Плюса',
                                'Заголовок должен содержать корректный текст'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем текст информационного сообщения',
                        () => this.growingCashbackTooltip.getDescriptionText()
                            .should.eventually.to.be.equal(
                                'За первые 3 заказа в приложении',
                                'Информационное сообщение должно содержать корректный текст'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем текст ссылки',
                        () => this.growingCashbackTooltip.getPromoTooltipLinkText()
                            .should.eventually.to.be.equal(
                                'Подробнее',
                                'Ссылка должна содержать корректный текст'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем содержимое ссылки',
                        () => this.growingCashbackTooltip.getPromoTooltipLink()
                            .should.eventually.to.be.link({
                                pathname: '/special/growing-cashback',
                            }, {
                                skipProtocol: true,
                                skipHostname: true,
                            })
                    );
                }
            },
        }),
    },
});

export default makeSuite('Тултип акции "Растущий кешбэк".', {
    environment: 'kadavr',
    feature: 'Растущий кешбэк',
    issue: 'MARKETFRONT-71625',
    id: 'marketfront-5299',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    growingCashbackTooltip: () => this.createPageObject(YandexPlusPromoOfferTooltip),
                });
            },
            'Авторизованный пользователь': {
                'Акция доступна.': prepareSuite(suite, {
                    params: {
                        isAuthWithPlugin: true,
                        shouldBeShown: true,
                    },
                    hooks: {
                        async beforeEach() {
                            prepareState.call(this, {
                                isPromoAvailable: true,
                                isGotFullReward: false,
                            });
                        },
                    },
                }),
                'Акция не доступна.': prepareSuite(suite, {
                    params: {
                        isAuthWithPlugin: true,
                        shouldBeShown: false,
                    },
                    hooks: {
                        async beforeEach() {
                            prepareState.call(this, {
                                isPromoAvailable: false,
                                isGotFullReward: false,
                            });
                        },
                    },
                }),
                'Пользователь сделал максимальное количество заказов по акции': prepareSuite(suite, {
                    params: {
                        isAuthWithPlugin: true,
                        shouldBeShown: false,
                    },
                    hooks: {
                        async beforeEach() {
                            prepareState.call(this, {
                                isPromoAvailable: true,
                                isGotFullReward: true,
                            });
                        },
                    },
                }),
            },
            'Не авторизованный пользователь': prepareSuite(suite, {
                params: {
                    shouldBeShown: false,
                },
                hooks: {
                    async beforeEach() {
                        prepareState.call(this, {
                            isPromoAvailable: true,
                            isGotFullReward: false,
                        });
                    },
                },
            }),
        }
    ),
});
