// @flow
// flowlint untyped-import:off
import {makeMirror} from '@self/platform/helpers/testament';
import PricePO from '@self/platform/components/Price/__pageObject';
import MultiSubscriptionPO from '@self/project/src/components/MultiSubscription/__pageObject';
import DefaultOfferPO from '@self/platform/components/DefaultOffer/__pageObject';
import SubscriptionButtonPO from '@self/root/src/components/SubscriptionButton/__pageObject';

import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

import * as actions from '@self/root/src/actions/checkout/fromOffer';

const {PAYMENTS} = actions;

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

type TestContext = {
    exps?: { [string]: boolean },
    // $FlowFixMe
    user?: { [string]: any },
};

const widgetPath = require.resolve('@self/platform/widgets/content/DefaultOffer');
const widgetOptions = {
    pp: 'OFFER_CARD',
    showMoreOffersLink: false,
    offerId: '456',
    showTitle: true,
};

async function makeContext({exps = {}, user = {}}: TestContext = {}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            ...user,
        },
        request: {
            abt: {
                expFlags: exps || {},
            },
        },
    });
}


describe('Widget: DefaultOffer', () => {
    beforeAll(async () => {
        mockIntersectionObserver();
        mirror = await makeMirror({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
            kadavr: {
                skipLayer: true,
            },
        });
        mandrelLayer = mirror.getLayer('mandrel');
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');

        // $FlowFixMe
        jest.useFakeTimers('modern');
        // $FlowFixMe
        jest.setSystemTime(new Date('2021-05-21T12:00:00.223z'));
        await jestLayer.doMock(
            require.resolve('@self/platform/widgets/content/FinancialProduct'),
            () => ({create: () => Promise.resolve(null)})
        );
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    describe('Подписочный оффер', () => {
        beforeAll(async () => {
            await jestLayer.backend.runCode(() => {
                const {
                    FEE_SHOW,
                    OFFER_COLLECTIONS,
                } = require('./__mock__/subscriptionGoodsMock');
                jest.mock(
                    '@self/platform/widgets/pages/ProductPage',
                    () => ({SKU_ID_QUERY_PARAM: 'sku'})
                );
                jest.spyOn(require('@self/root/src/resolvers/experimentFlags'), 'resolveExperimentFlagsSync')
                    .mockReturnValue({
                        result: [],
                        collections: {
                            experimentFlag: {},
                        },
                    });
                jest.spyOn(require('@self/project/src/resolvers/cartService/resolveCartItems'), 'resolveCartItems')
                    .mockReturnValue(Promise.resolve({
                        result: [],
                        collections: {
                            userCartItem: {},
                            cartItem: {},
                            cartItemState: {},
                        },
                    }));
                jest.spyOn(require('@self/project/src/resolvers/offer/resolveOffersByIds'), 'resolveOffersByIds')
                    .mockReturnValue(Promise.resolve({
                        result: [FEE_SHOW],
                        collections: OFFER_COLLECTIONS,
                    }));
            }, []);
        });

        // SKIPPED MARKETFRONT-96354
        // eslint-disable-next-line jest/no-disabled-tests
        describe.skip('Цена', () => {
            // bluemarket-4144
            test('Отображается с "/мес"', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const priceSubscriptionAppendix = container.querySelector(PricePO.subscriptionAppendix);

                expect(priceSubscriptionAppendix).toBeTruthy();
            });
        });

        describe('"Вместе с подпиской Плюс Мульти"', () => {
            // bluemarket-4144
            test('Отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const multiSubscription = container.querySelector(MultiSubscriptionPO.root);

                expect(multiSubscription).toBeTruthy();
            });
        });

        describe('Кнопка "В корзину"', () => {
            // bluemarket-4144
            test('Не отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const cartButton = container.querySelector(DefaultOfferPO.cartButton);

                expect(cartButton).toBeFalsy();
            });
        });

        describe('Кнопка "Оформить подписку"', () => {
            const {FEE_SHOW} = require('./__mock__/subscriptionGoodsMock');
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

            // bluemarket-4144
            test('Отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const subscriptionButton = container.querySelector(SubscriptionButtonPO.root);

                expect(subscriptionButton).toBeTruthy();
            });

            // bluemarket-4144
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
                    offerShowPlaceId: FEE_SHOW,
                    regionId: 213,
                    shouldSkipCart: true,
                    payment: PAYMENTS.YANDEX,
                });
            });

            // bluemarket-4144
            test('Клик неавторизованного пользователя тригерит "onAuth"', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

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
