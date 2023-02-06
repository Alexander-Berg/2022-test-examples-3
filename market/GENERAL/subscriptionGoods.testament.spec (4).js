// @flow
import {makeMirror} from '@self/platform/helpers/testament';
import {
    createProduct,
    createOffer,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {PackedFunction} from '@yandex-market/testament/mirror';
// flowlint-next-line untyped-import:off
import PricePO from '@self/project/src/components/Price/__pageObject';
// flowlint-next-line untyped-import:off
import MultiSubscriptionPO from '@self/project/src/components/MultiSubscription/__pageObject';
// flowlint-next-line untyped-import:off
import DefaultOfferPO from '@self/platform/components/DefaultOffer/__pageObject';
// flowlint-next-line untyped-import:off
import SubscriptionButtonPO from '@self/root/src/components/SubscriptionButton/__pageObject';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import * as actions from '@self/platform/actions/checkout/fromOffer';
import {
    OFFER,
    OFFER_ID,
    PRODUCT,
    PRODUCT_ID,
    PRODUCT_PROMISE_MOCK,
    DEFAULT_OFFER_PROMISE_MOCK,
    PAGE_STATE_PROMISE_MOCK,
} from './__mock__/subscriptionGoodsMock';

const {PAYMENTS} = actions;

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

type TestContext = {
    exps?: { [string]: boolean },
    user?: { [string]: mixed },
};

const widgetPath = require.resolve('@self/platform/widgets/parts/DefaultOffer');
const widgetOptions = new PackedFunction(
    (productMock, defaultOfferMock, pageStateMock) => ({
        productPromise: Promise.resolve(productMock),
        defaultOfferPromise: Promise.resolve(defaultOfferMock),
        pageStatePromise: Promise.resolve(pageStateMock),
        isDynamic: true,
    }),
    [PRODUCT_PROMISE_MOCK, DEFAULT_OFFER_PROMISE_MOCK, PAGE_STATE_PROMISE_MOCK]
);

async function makeContext({exps = {}, user = {}}: TestContext = {}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';

    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            region: {
                id: 213,
            },
            ...user,
        },
        request: {
            cookie,
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

const setReportState = async () => {
    const product = createProduct(PRODUCT, PRODUCT_ID);
    const offer = createOffer(OFFER, OFFER_ID);

    const reportState = mergeState([
        product,
        offer,
    ]);

    await kadavrLayer.setState('report', reportState);
};

describe('Widget: DefaultOffer', () => {
    beforeAll(async () => {
        mockIntersectionObserver();
        mirror = await makeMirror({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
            kadavr: {
                asLibrary: true,
            },
        });
        mandrelLayer = mirror.getLayer('mandrel');
        kadavrLayer = mirror.getLayer('kadavr');
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    describe('Подписочный оффер', () => {
        beforeAll(async () => {
            await setReportState();
            await jestLayer.backend.runCode(() => {
                jest.spyOn(require('@self/root/src/resolvers/region'), 'resolveCurrentUserRegionSync')
                    .mockReturnValue({result: 213});
            }, []);
        });
        // SKIPPED MARKETFRONT-96354
        // eslint-disable-next-line jest/no-disabled-tests
        describe.skip('Цена', () => {
            // bluemarket-4146
            test('Отображается с "/мес"', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const priceSubscriptionAppendix = container.querySelector(PricePO.subscriptionAppendix);

                expect(priceSubscriptionAppendix).toBeTruthy();
            });
        });

        // SKIPPED MARKETFRONT-96354
        // eslint-disable-next-line jest/no-disabled-tests
        describe.skip('"Вместе с подпиской Плюс Мульти"', () => {
            // bluemarket-4146
            test('Отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const multiSubscription = container.querySelector(MultiSubscriptionPO.root);

                expect(multiSubscription).toBeTruthy();
            });
        });

        describe('Кнопка "В корзину"', () => {
            // bluemarket-4146
            test('Не отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const cartButton = container.querySelector(DefaultOfferPO.cartButton);

                expect(cartButton).toBeFalsy();
            });
        });

        describe('Кнопка "Оформить подписку"', () => {
            let toAuthSpy = jest.spyOn(actions, 'toAuth');
            let toCheckoutSpy = jest.spyOn(actions, 'toCheckout');

            beforeEach(() => {
                toAuthSpy = jest.spyOn(actions, 'toAuth');
                toCheckoutSpy = jest.spyOn(actions, 'toCheckout');
            });

            afterEach(() => {
                toAuthSpy.mockClear();
                toCheckoutSpy.mockClear();
            });

            test('Отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const subscriptionButton = container.querySelector(SubscriptionButtonPO.root);

                expect(subscriptionButton).toBeTruthy();
            });

            // bluemarket-4146
            test('Клик авторизованного пользователя тригерит "onCheckout"', async () => {
                await makeContext({
                    user: {
                        isAuth: true,
                    },
                });
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const subscriptionButton = container.querySelector(SubscriptionButtonPO.root);

                subscriptionButton.click();

                // TODO MARKETFRONT-86630
                // expect(onCheckoutSpy).toHaveBeenCalledTimes(1);
                expect(toCheckoutSpy).toHaveBeenCalledWith({
                    offerId: 'H6Bn4eLTXsswSYSJg-BsLw_s2gX7czYM7-uGu-9hhlE1gLmsf8UrgPLzfPMW4Hyraue9yUtW2bMaPonTeYVSB_jBs8TROtBDweEwBeHOTTjHDq3Alo7y81aA0gdP-TsGurYQ4aee91QyPNJq6DW3xJUqFwjvI1aRt9embLkh3t3zxvrgru4PZY2z5Js5pPhNyA,',
                    regionId: 213,
                    shouldSkipCart: true,
                    payment: PAYMENTS.YANDEX,
                });
            });

            // bluemarket-4146
            test('Клик неавторизованного пользователя тригерит "onAuth"', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const subscriptionButton = container.querySelector(SubscriptionButtonPO.root);

                subscriptionButton.click();

                // TODO MARKETFRONT-86630
                // expect(onAuthSpy).toHaveBeenCalledTimes(1);
                expect(toAuthSpy).toHaveBeenCalledWith({
                    offerId: 'H6Bn4eLTXsswSYSJg-BsLw_s2gX7czYM7-uGu-9hhlE1gLmsf8UrgPLzfPMW4Hyraue9yUtW2bMaPonTeYVSB_jBs8TROtBDweEwBeHOTTjHDq3Alo7y81aA0gdP-TsGurYQ4aee91QyPNJq6DW3xJUqFwjvI1aRt9embLkh3t3zxvrgru4PZY2z5Js5pPhNyA,',
                    regionId: 213,
                    rawAuthUrl: 'external:passport-auth_{\"region\":\"ru\"}',
                    payment: PAYMENTS.YANDEX,
                });
            });
        });
    });
});
