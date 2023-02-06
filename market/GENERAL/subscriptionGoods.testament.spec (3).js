// @flow
import {makeMirror} from '@self/platform/helpers/testament';
import {
    // flowlint-next-line untyped-import:off
    createProduct,
    // flowlint-next-line untyped-import:off
    mergeState,
    // flowlint-next-line untyped-import:off
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createOffer} from '@self/project/src/entities/offer/__mock__/offer.mock';
import type {Offer} from '@self/project/src/entities/offer';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
// flowlint-next-line untyped-import:off
import PricePO from '@self/platform/components/Price/__pageObject';
// flowlint-next-line untyped-import:off
import TextPO from '@self/root/src/uikit/components/Text/__pageObject';
// flowlint-next-line untyped-import:off
import DefaultOfferPO from '@self/platform/components/DefaultOffer/__pageObject';
// flowlint-next-line untyped-import:off
import SubscriptionButtonPO from '@self/root/src/components/SubscriptionButton/__pageObject';
import * as actions from '@self/root/src/actions/checkout/fromOffer';
import {
    FEE_SHOW,
    OFFER,
    OFFER_ID,
    PRODUCT,
    PRODUCT_ID,
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

type TestContext = {
    exps?: { [string]: boolean },
    user?: { [string]: mixed },
};

const widgetPath = require.resolve('@self/platform/widgets/content/StickyProductCard');

async function makeContext({exps = {}, user = {}}: TestContext = {}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user: {
            ...user,
        },
        request: {
            cookie,
            params: {
                productId: PRODUCT_ID,
            },
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

const setReportState = async (offerData: Offer) => {
    const offer = createOffer(offerData);

    const reportState = mergeState([
        createProduct(PRODUCT, PRODUCT_ID),
        createOfferForProduct(offer, PRODUCT_ID, OFFER_ID),
        {
            data: {
                search: {
                    total: 1,
                },
            },
        },
    ]);

    await kadavrLayer.setState('report', reportState);
};

describe('Widget: DefaultOffer.', () => {
    beforeAll(async () => {
        mockIntersectionObserver();
        mirror = await makeMirror({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
            kadavr: {
                // asLibrary: true,
            },
        });
        mandrelLayer = mirror.getLayer('mandrel');
        kadavrLayer = mirror.getLayer('kadavr');
        apiaryLayer = mirror.getLayer('apiary');
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    describe('Подписочный оффер.', () => {
        beforeAll(async () => {
            await setReportState(OFFER);
        });

        describe('Цена.', () => {
            // bluemarket-4145
            test('Отображается с "/мес"', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    productId: PRODUCT_ID,
                });

                const priceSubscriptionAppendix = container.querySelector(PricePO.subscriptionAppendix);

                expect(priceSubscriptionAppendix).toBeTruthy();
            });
        });

        describe('"Вместе с подпиской Плюс Мульти".', () => {
            // bluemarket-4145
            test('Отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    productId: PRODUCT_ID,
                });

                const textElements = container.querySelectorAll(TextPO.root);
                const multiSubscriptionElements = Array.prototype.filter.call(
                    textElements,
                    element => element.innerHTML === 'Вместе с Плюс Мульти');

                expect(multiSubscriptionElements.length).toBe(1);
            });
        });

        describe('Кнопка "В корзину"', () => {
            // bluemarket-4145
            test('Не отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    productId: PRODUCT_ID,
                });

                const cartButton = container.querySelector(DefaultOfferPO.cartButton);

                expect(cartButton).toBeFalsy();
            });
        });

        describe('Кнопка "Оформить подписку".', () => {
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

            // bluemarket-4145
            test('Отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    productId: PRODUCT_ID,
                });

                const subscriptionButton = container.querySelector(SubscriptionButtonPO.root);

                expect(subscriptionButton).toBeTruthy();
            });

            // bluemarket-4145
            test('Клик авторизованного пользователя тригерит "onCheckout"', async () => {
                await makeContext({
                    user: {
                        isAuth: true,
                    },
                });
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    productId: PRODUCT_ID,
                });

                const subscriptionButton = container.querySelector(SubscriptionButtonPO.root);

                subscriptionButton.click();

                // TODO MARKETFRONT-86630
                // expect(onCheckoutSpy).toHaveBeenCalledTimes(1);
                expect(toCheckoutSpy).toHaveBeenCalledWith({
                    offerShowPlaceId: FEE_SHOW,
                    regionId: 213,
                    shouldSkipCart: true,
                    payment: PAYMENTS.YANDEX,
                });
            });

            // bluemarket-4145
            test('Клик неавторизованного пользователя тригерит "onAuth"', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    productId: PRODUCT_ID,
                });

                const subscriptionButton = container.querySelector(SubscriptionButtonPO.root);

                subscriptionButton.click();

                // TODO MARKETFRONT-86630
                // expect(onAuthSpy).toHaveBeenCalledTimes(1);
                expect(toAuthSpy).toHaveBeenCalledWith({
                    offerShowPlaceId: FEE_SHOW,
                    regionId: 213,
                    rawAuthUrl: "external:passport-auth_{'region':'ru'}",
                    payment: PAYMENTS.YANDEX,
                });
            });
        });
    });
});
