/**
 * @expFlag all_plus-4-all
 * @ticket MARKETPROJECT-8396
 * @start
 */

import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import {
    prepareCashbackProfile,
    prepareCashbackOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/cashback';
import {deliveryDeliveryMock, deliveryPickupMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import COOKIE_CONSTANTS from '@self/root/src/constants/cookie';
import CashbackForAllBanner from
    '@self/root/src/widgets/content/cart/CartYaPlusPromo/components/CashbackForAllBanner/__pageObject';
import {yandexPlusPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';

const CASHBACK_MORE_THRESHOLD = 200;
const CASHBACK_LESS_THRESHOLD = 100;
const THRESHOLD = 150;

async function prepareState({
    hasYaPlus,
    hasThreshold,
    hasCashbackMoreThreshold,
    hasCashbackLessThreshold,
}) {
    if (hasYaPlus) {
        await this.browser.setState('Loyalty.collections.perks', [yandexPlusPerk]);
    }

    if (hasThreshold) {
        await this.browser.setState('S3Mds.files', {
            '/cashbackForAllConfig/data.json': {
                cashbackThresholdForBannerInCart: THRESHOLD,
            },
        });
    }

    let cashbackAmount;

    if (hasCashbackMoreThreshold) {
        cashbackAmount = CASHBACK_MORE_THRESHOLD;
    } else if (hasCashbackLessThreshold) {
        cashbackAmount = CASHBACK_LESS_THRESHOLD;
    }

    const cart = buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: {
                ...kettle.offerMock,
                promos: cashbackAmount
                    ? [
                        {
                            type: 'blue-cashback',
                            value: cashbackAmount,
                        },
                    ]
                    : [],
            },
            count: 1,
        }],
        deliveryOptions: [
            deliveryDeliveryMock,
            deliveryPickupMock,
        ],
    });

    const cashbackOptionsProfiles = cashbackAmount && prepareCashbackProfile({
        cartId: cart.label,
        offerId: kettle.offerMock.feed.offerId,
        cashbackAmount,
        feedId: kettle.offerMock.feed.id,
    });

    const cashback = cashbackAmount && prepareCashbackOptions(cashbackAmount);

    await this.browser.yaScenario(
        this,
        prepareMultiCartState,
        [cart],
        {
            additionalCheckouterCollections: {
                cashbackOptionsProfiles,
                cashback,
            },
        }
    );

    await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});

    return this.browser.yaScenario(this, waitForCartActualization);
}

module.exports = testSuites => makeSuite('Информационная врезка.', {
    environment: 'kadavr',
    feature: 'Кешбэк для всех',
    issue: 'MARKETFRONT-79274',
    defaultParams: {
        cookie: {
            [COOKIE_CONSTANTS.EXP_FLAGS]: {
                name: COOKIE_CONSTANTS.EXP_FLAGS,
                value: 'all_plus-4-all',
            },
            [COOKIE_CONSTANTS.FORCE_AT_EXP]: {
                name: COOKIE_CONSTANTS.FORCE_AT_EXP,
                value: 'true',
            },
            /**
             * Кука, которая заставляет AB-шницу выдать test-id=533830 (пользователь попавший в )
             * https://wiki.yandex-team.ru/serp/experiments/zerotesting/apidescription/
             * Значение для куки было получено 31-03-2022 и протухнет 31-03-2022
             * Новое значение для куки можно получить, сгенерировав ссылку заново как это написано на wiki
             *
             */
            yexp: {
                name: 'yexp',
                value: 't533830.e1680220800.sE3FD187C5489F6B2A42D3D3EFD73BF9C',
            },
        },
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    cashbackForAllBanner: () => this.createPageObject(CashbackForAllBanner),
                });
            },
            'Авторизованный пользователь': {
                'Плюсовик.': prepareSuite(testSuites, {
                    params: {
                        isAuthWithPlugin: true,
                        shouldBeShown: false,
                    },
                    hooks: {
                        async beforeEach() {
                            prepareState.call(this, {
                                hasYaPlus: true,
                                hasThreshold: true,
                                hasCashbackMoreThreshold: true,
                            });
                        },
                    },
                }),
                'Минусовик.': {
                    'В корзине нет товаров с кешбэком': prepareSuite(testSuites, {
                        params: {
                            isAuthWithPlugin: true,
                            shouldBeShown: false,
                        },
                        hooks: {
                            async beforeEach() {
                                prepareState.call(this, {
                                    hasThreshold: true,
                                });
                            },
                        },
                    }),
                    'Не определен трешхолд для отображения баллов': prepareSuite(testSuites, {
                        params: {
                            isAuthWithPlugin: true,
                            shouldBeShown: false,
                        },
                        hooks: {
                            async beforeEach() {
                                prepareState.call(this, {
                                    hasCashbackMoreThreshold: true,
                                });
                            },
                        },
                    }),
                    'Кешбэк товаров в корзине меньше трешхолда': prepareSuite(testSuites, {
                        params: {
                            isAuthWithPlugin: true,
                            shouldBeShown: false,
                        },
                        hooks: {
                            async beforeEach() {
                                prepareState.call(this, {
                                    hasThreshold: true,
                                    hasCashbackLessThreshold: true,
                                });
                            },
                        },
                    }),
                    'Кешбэк товаров в корзине больше трешхолда': prepareSuite(testSuites, {
                        params: {
                            isAuthWithPlugin: true,
                            shouldBeShown: true,
                        },
                        hooks: {
                            async beforeEach() {
                                prepareState.call(this, {
                                    hasThreshold: true,
                                    hasCashbackMoreThreshold: true,
                                });
                            },
                        },
                    }),
                },
            },
            'Не авторизованный пользователь': {
                'В корзине нет товаров с кешбэком': prepareSuite(testSuites, {
                    params: {
                        shouldBeShown: false,
                    },
                    hooks: {
                        async beforeEach() {
                            prepareState.call(this, {
                                hasThreshold: true,
                            });
                        },
                    },
                }),
                'Не определен трешхолд для отображения баллов': prepareSuite(testSuites, {
                    params: {
                        shouldBeShown: false,
                    },
                    hooks: {
                        async beforeEach() {
                            prepareState.call(this, {
                                hasCashbackMoreThreshold: true,
                            });
                        },
                    },
                }),
                'Кешбэк товаров в корзине меньше трешхолда': prepareSuite(testSuites, {
                    params: {
                        shouldBeShown: false,
                    },
                    hooks: {
                        async beforeEach() {
                            prepareState.call(this, {
                                hasThreshold: true,
                                hasCashbackLessThreshold: true,
                            });
                        },
                    },
                }),
                'Кешбэк товаров в корзине больше трешхолда': prepareSuite(testSuites, {
                    params: {
                        shouldBeShown: true,
                    },
                    hooks: {
                        async beforeEach() {
                            prepareState.call(this, {
                                hasThreshold: true,
                                hasCashbackMoreThreshold: true,
                            });
                        },
                    },
                }),
            },
        }
    ),
});

/**
 * @expFlag all_plus-4-all
 * @ticket MARKETPROJECT-8396
 * @end
 */
