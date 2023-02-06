import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {
    createProduct,
    createOffer,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {PackedFunction} from '@yandex-market/testament/mirror';

import PaymentSystemCashbackInfo from '@self/root/src/components/CashbackInfos/PaymentSystemCashbackInfo/__pageObject';

import {paymentSystemExtraCashbackPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';
import {getBonusString} from '@self/root/src/utils/string';
import {
    OFFER,
    OFFER_ID,
    PRODUCT,
    PRODUCT_ID,
    PRODUCT_PROMISE_MOCK,
    DEFAULT_OFFER_PROMISE_MOCK,
    PAGE_STATE_PROMISE_MOCK,
} from './__mock__/paymentSystemCashbackMock';

export const CASHBACK_AMOUNT = 100;
export const PAYMENT_SYSTEM_CASHBACK_AMOUNT = 200;

// путь к виджету который тестируем
const WIDGET_PATH = '../';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {JestLayer} */
let jestLayer;

const widgetOptions = new PackedFunction(
    (productMock, defaultOfferMock, pageStateMock) => ({
        productPromise: Promise.resolve(productMock),
        defaultOfferPromise: Promise.resolve(defaultOfferMock),
        pageStatePromise: Promise.resolve(pageStateMock),
        isDynamic: true,
    }),
    [PRODUCT_PROMISE_MOCK, DEFAULT_OFFER_PROMISE_MOCK, PAGE_STATE_PROMISE_MOCK]
);

async function makeContext({isPromoProduct, hasCashback, isPromoAvailable}, user = {}, pageId = 'touch:index') {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    mandrelLayer.initContext({
        request: {cookie},
        ...(user.isAuth ? {user} : {}),
        page: {
            pageId,
        },
    });

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

    OFFER.promos = promos;
    const product = createProduct(PRODUCT, PRODUCT_ID);
    const offer = createOffer(OFFER, OFFER_ID);
    const state = mergeState([
        product,
        offer,
    ]);

    await kadavrLayer.setState('Loyalty.collections.perks', isPromoAvailable ? [paymentSystemExtraCashbackPerk] : []);
    await kadavrLayer.setState('report', state);
}

beforeAll(async () => {
    mockIntersectionObserver();
    mirror = await makeMirrorTouch({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');
    jestLayer = mirror.getLayer('jest');
});

afterAll(() => {
    mirror.destroy();
    jest.useRealTimers();
});

describe('Дефолтный оффер.', () => {
    beforeAll(async () => {
        await jestLayer.backend.runCode(() => {
            jest.spyOn(require('@self/root/src/resolvers/region'), 'resolveCurrentUserRegionSync')
                .mockReturnValue({result: 213});
        }, []);
    });

    describe('Блок акционного кешбэка по платежной системе.', () => {
        describe('Акционный кешбэк отображается вместе с обычным кешбэком', () => {
            // SKIPPED MARKETFRONT-96354
            // eslint-disable-next-line jest/no-disabled-tests
            it.skip('По умолчанию Отображается', async () => {
                await jestLayer.backend.runCode(() => {
                    jest.spyOn(require('@self/root/src/resolvers/paymentSystemExtraCashback'), 'resolvePaymentSystemPromoCashback')
                        .mockReturnValue(Promise.resolve('mastercard'));
                }, []);
                await makeContext({isPromoProduct: true, hasCashback: true, isPromoAvailable: true}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
                const rootElement = container.querySelector(PaymentSystemCashbackInfo.root);
                expect(rootElement).toBeTruthy();
            });
            it('Для mastercard, содержит корректный текст', async () => {
                await jestLayer.backend.runCode(() => {
                    jest.spyOn(require('@self/root/src/resolvers/paymentSystemExtraCashback'), 'resolvePaymentSystemPromoCashback')
                        .mockReturnValue(Promise.resolve('mastercard'));
                }, []);
                await makeContext({isPromoProduct: true, hasCashback: true, isPromoAvailable: true}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
                const rootElement = container.querySelector(PaymentSystemCashbackInfo.root);
                // eslint-disable-next-line no-irregular-whitespace
                const expectedText = `${CASHBACK_AMOUNT} ${getBonusString(CASHBACK_AMOUNT)} и еще${PAYMENT_SYSTEM_CASHBACK_AMOUNT} ${getBonusString(PAYMENT_SYSTEM_CASHBACK_AMOUNT)} с Mastercard`;
                expect(rootElement.textContent).toEqual(expectedText);
            });
            it('Для mir, содержит корректный текст', async () => {
                await jestLayer.backend.runCode(() => {
                    jest.spyOn(require('@self/root/src/resolvers/paymentSystemExtraCashback'), 'resolvePaymentSystemPromoCashback')
                        .mockReturnValue(Promise.resolve('mir'));
                }, []);
                jest.spyOn(require('@self/root/src/resolvers/paymentSystemExtraCashback'), 'resolvePaymentSystemPromoCashback')
                    .mockReturnValue(Promise.resolve('mir'));
                await makeContext({isPromoProduct: true, hasCashback: true, isPromoAvailable: true}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
                const rootElement = container.querySelector(PaymentSystemCashbackInfo.root);
                // eslint-disable-next-line no-irregular-whitespace
                const expectedText = `${CASHBACK_AMOUNT} ${getBonusString(CASHBACK_AMOUNT)} и еще${PAYMENT_SYSTEM_CASHBACK_AMOUNT} ${getBonusString(PAYMENT_SYSTEM_CASHBACK_AMOUNT)} с картой «Мир»`;
                expect(rootElement.textContent).toEqual(expectedText);
            });
        });

        describe('Акция недоступна для пользователя', () => {
            it('Не отображается', async () => {
                await makeContext({isPromoProduct: false, hasCashback: true, isPromoAvailable: false}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
                const paymentSystem = container.querySelector(PaymentSystemCashbackInfo.root);
                expect(paymentSystem).toBeFalsy();
            });
        });

        describe('Выбран не акционный товар', () => {
            it('Не отображается', async () => {
                await makeContext({isPromoProduct: false, hasCashback: false, isPromoAvailable: true}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
                const paymentSystem = container.querySelector(PaymentSystemCashbackInfo.root);
                expect(paymentSystem).toBeFalsy();
            });
        });

        describe('Незалогин', () => {
            it('Не отображается', async () => {
                await makeContext({isPromoProduct: false, hasCashback: true, isPromoAvailable: false}, {isAuth: false});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
                const paymentSystem = container.querySelector(PaymentSystemCashbackInfo.root);
                expect(paymentSystem).toBeFalsy();
            });
        });

        describe('Отображается только акционный кешбэк', () => {
            it('По умолчанию Отображается', async () => {
                await makeContext({isPromoProduct: true, hasCashback: false, isPromoAvailable: true}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
                const rootElement = container.querySelector(PaymentSystemCashbackInfo.root);
                expect(rootElement).toBeTruthy();
            });
            it('Для mastercard, содержит корректный текст', async () => {
                await jestLayer.backend.runCode(() => {
                    jest.spyOn(require('@self/root/src/resolvers/paymentSystemExtraCashback'), 'resolvePaymentSystemPromoCashback')
                        .mockReturnValue(Promise.resolve('mastercard'));
                }, []);
                await makeContext({isPromoProduct: true, hasCashback: false, isPromoAvailable: true}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
                const rootElement = container.querySelector(PaymentSystemCashbackInfo.root);
                // eslint-disable-next-line no-irregular-whitespace
                const expectedText = `${PAYMENT_SYSTEM_CASHBACK_AMOUNT} ${getBonusString(PAYMENT_SYSTEM_CASHBACK_AMOUNT)} с Mastercard`;
                expect(rootElement.textContent).toEqual(expectedText);
            });
            it('Для mir, cодержит корректный текст', async () => {
                await jestLayer.backend.runCode(() => {
                    jest.spyOn(require('@self/root/src/resolvers/paymentSystemExtraCashback'), 'resolvePaymentSystemPromoCashback')
                        .mockReturnValue(Promise.resolve('mir'));
                }, []);
                await makeContext({isPromoProduct: true, hasCashback: false, isPromoAvailable: true}, {isAuth: true});
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
                const rootElement = container.querySelector(PaymentSystemCashbackInfo.root);
                // eslint-disable-next-line no-irregular-whitespace
                const expectedText = `${PAYMENT_SYSTEM_CASHBACK_AMOUNT} ${getBonusString(PAYMENT_SYSTEM_CASHBACK_AMOUNT)} с картой «Мир»`;
                expect(rootElement.textContent).toEqual(expectedText);
            });
        });
    });
});
