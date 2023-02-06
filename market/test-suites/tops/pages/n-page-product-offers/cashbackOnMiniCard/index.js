import {mergeSuites, prepareSuite, makeSuite} from 'ginny';

import {stateProductWithDO} from '@self/platform/spec/hermione/configs/seo/mocks';
import paymentSystemCashback from '@self/root/src/spec/hermione/test-suites/blocks/paymentSystemCashback';
import {paymentSystemExtraCashbackPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';

import productOptionsFixture from '../fixtures/productOptions';

const CASHBACK_AMOUNT = 100;
const PAYMENT_SYSTEM_CASHBACK_AMOUNT = 200;

export default makeSuite('Акционный кешбэк по платежной системе на мини оффере', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(paymentSystemCashback({shouldShow: false}), {
            suiteName: 'Незалогин',
            meta: {
                id: 'marketfront-5245',
            },
            params: {
                isAuthWithPlugin: false,
                prepareState: function () {
                    createAndSetPaymentSystemCashbackState.call(this, {
                        isPromoAvailable: false,
                        isPromoProduct: false,
                        hasCashback: true,
                    });
                },
            },
        }),
        prepareSuite(paymentSystemCashback({shouldShow: false}), {
            suiteName: 'Акция недоступна для пользователя',
            meta: {
                id: 'marketfront-5243',
            },
            params: {
                prepareState: async function () {
                    await createAndSetPaymentSystemCashbackState.call(this, {
                        isPromoAvailable: false,
                        isPromoProduct: false,
                        hasCashback: true,
                    });
                },
            },
        }),
        prepareSuite(paymentSystemCashback({shouldShow: false}), {
            suiteName: 'Выбран не акционный товар',
            meta: {
                id: 'marketfront-5244',
            },
            params: {
                prepareState: async function () {
                    await createAndSetPaymentSystemCashbackState.call(this, {
                        isPromoAvailable: true,
                        isPromoProduct: false,
                        hasCashback: true,
                    });
                },
            },
        }),
        prepareSuite(paymentSystemCashback({shouldShow: true}), {
            suiteName: 'Акционный кешбэк отображается вместе с обычным кешбэком',
            meta: {
                id: 'marketfront-5234',
            },
            params: {
                cashbackAmount: CASHBACK_AMOUNT,
                paymentSystemCashbackAmount: PAYMENT_SYSTEM_CASHBACK_AMOUNT,
                prepareState: async function () {
                    await createAndSetPaymentSystemCashbackState.call(this, {
                        isPromoAvailable: true,
                        isPromoProduct: true,
                        hasCashback: true,
                    });
                },
            },
        }),
        prepareSuite(paymentSystemCashback({shouldShow: true}), {
            suiteName: 'Отображается только акционный кешбэк',
            meta: {
                id: 'marketfront-5235',
            },
            params: {
                paymentSystemCashbackAmount: PAYMENT_SYSTEM_CASHBACK_AMOUNT,
                prepareState: async function () {
                    await createAndSetPaymentSystemCashbackState.call(this, {
                        isPromoAvailable: true,
                        isPromoProduct: true,
                    });
                },
            },
        })
    ),
});

async function createAndSetPaymentSystemCashbackState({
    isPromoAvailable,
    isPromoProduct,
    hasCashback,
}) {
    const promos = [];

    if (isPromoProduct) {
        promos.push({
            id: '1',
            key: '1',
            type: 'blue-cashback',
            value: PAYMENT_SYSTEM_CASHBACK_AMOUNT,
            tags: ['payment-system-promo'],
        });
    }

    if (hasCashback) {
        promos.push({
            id: '2',
            key: '2',
            type: 'blue-cashback',
            value: CASHBACK_AMOUNT,
        });
    }

    const productId = 12345;
    const state = stateProductWithDO(productId, {}, {promos});

    await this.browser.setState('report', state);
    await this.browser.setState('Loyalty.collections.perks', isPromoAvailable ? [paymentSystemExtraCashbackPerk] : []);
    return this.browser.yaOpenPage('market:product-offers', {
        slug: productOptionsFixture.slug,
        productId,
    });
}
