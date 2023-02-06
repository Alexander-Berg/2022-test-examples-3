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
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
// flowlint-next-line untyped-import:off
import PricePO from '@self/platform/components/Price/__pageObject';
// flowlint-next-line untyped-import:off
import MultiSubscriptionPO from '@self/project/src/components/MultiSubscription/__pageObject';
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
} from './__mock__/subscriptionGoodsMock.js';

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

const widgetPath = require.resolve('@self/platform/widgets/content/KMDefaultOffer');

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

const setReportState = async offerData => {
    // $FlowFixMe не видит вложенные сущности
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
            await setReportState(OFFER);
            await jestLayer.runCode(() => {
                jest.mock(
                    '@self/platform/widgets/pages/ProductPage',
                    () => ({SKU_ID_QUERY_PARAM: 'sku'})
                );
            }, []);
        });

        // SKIPPED MARKETFRONT-96354
        // eslint-disable-next-line jest/no-disabled-tests
        describe.skip('Цена', () => {
            // bluemarket-4144
            test('Отображается с "/мес"', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

                const priceSubscriptionAppendix = container.querySelector(PricePO.subscriptionAppendix);

                expect(priceSubscriptionAppendix).toBeTruthy();
            });
        });

        describe('"Вместе с подпиской Плюс Мульти"', () => {
            // bluemarket-4144
            test('Отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

                const multiSubscription = container.querySelector(MultiSubscriptionPO.root);

                expect(multiSubscription).toBeTruthy();
            });
        });

        describe('Кнопка "В корзину"', () => {
            // bluemarket-4144
            test('Не отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

                const cartButton = container.querySelector(DefaultOfferPO.cartButton);

                expect(cartButton).toBeFalsy();
            });
        });

        describe('Кнопка "Оформить"', () => {
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
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

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
                const {container} = await apiaryLayer.mountWidget(widgetPath, {
                    isSecondaryDO: false,
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

            // bluemarket-4144
            test('Клик неавторизованного пользователя тригерит "onAuth"', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

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
