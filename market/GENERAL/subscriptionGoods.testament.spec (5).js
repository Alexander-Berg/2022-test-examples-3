// @flow
import {makeMirror} from '@self/platform/helpers/testament';
// flowlint-next-line untyped-import:off
import PricePO from '@self/project/src/components/Price/__pageObject';
// flowlint-next-line untyped-import:off
import MultiSubscriptionPO from '@self/project/src/components/MultiSubscription/__pageObject';
// flowlint-next-line untyped-import:off
import CartButtonPO from '@self/project/src/components/CartButton/__pageObject';
// flowlint-next-line untyped-import:off
import SubscriptionButtonPO from '@self/root/src/components/SubscriptionButton/__pageObject';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import * as actions from '@self/platform/actions/checkout/fromOffer';
import {
    WIDGET_OPTIONS_MOCK,
    WISHLIST_ITEMS_MOCK,
} from './__mock__/subscriptionGoodsMock';

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
    user?: { [string]: mixed },
};

const widgetPath = require.resolve('@self/platform/widgets/parts/OfferSummary');

async function makeContext({exps = {}, user = {}}: TestContext = {}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';

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
            abt: {
                expFlags: exps || {},
            },
        },
    });
}
// SKIPPED MARKETFRONT-96354
// eslint-disable-next-line jest/no-disabled-tests
describe.skip('Widget: OfferSummary', () => {
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
        // kadavrLayer = mirror.getLayer('kadavr');
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');

        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/ProductTableSize'),
            () => ({create: () => Promise.resolve(null)})
        );
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    describe('Подписочный оффер', () => {
        beforeAll(async () => {
            // await setReportState();
            await jestLayer.backend.runCode((widgetOptionsMock, wishlistItemsMock) => {
                const actualController = jest.requireActual('@self/platform/widgets/parts/OfferSummary/controller').default;

                jest.spyOn(require('@self/platform/widgets/parts/OfferSummary/controller'), 'default')
                    .mockImplementation(ctx => actualController(ctx, Promise.resolve(widgetOptionsMock)));

                jest.spyOn(require('@self/root/src/resolvers/wishlist'), 'resolveWishlistItems')
                    .mockResolvedValue(wishlistItemsMock);
            }, [WIDGET_OPTIONS_MOCK, WISHLIST_ITEMS_MOCK]);
        });

        describe('Цена', () => {
            // bluemarket-4163
            test('Отображается с "/мес"', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

                const priceSubscriptionAppendix = container.querySelector(PricePO.subscriptionAppendix);

                expect(priceSubscriptionAppendix).toBeTruthy();
            });
        });

        describe('"Вместе с подпиской Плюс Мульти"', () => {
            // bluemarket-4163
            test('Отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

                const multiSubscription = container.querySelector(MultiSubscriptionPO.root);

                expect(multiSubscription).toBeTruthy();
            });
        });

        describe('Кнопка "В корзину"', () => {
            // bluemarket-4163
            test('Не отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

                const cartButton = container.querySelector(CartButtonPO.root);

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
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

                const subscriptionButton = container.querySelector(SubscriptionButtonPO.root);

                expect(subscriptionButton).toBeTruthy();
            });

            // bluemarket-4163
            test('Клик авторизованного пользователя тригерит "onCheckout"', async () => {
                await makeContext({
                    user: {
                        isAuth: true,
                    },
                });
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

                const subscriptionButton = container.querySelector(SubscriptionButtonPO.root);

                subscriptionButton.click();

                // TODO MARKETFRONT-86630
                // expect(onCheckoutSpy).toHaveBeenCalledTimes(1);
                expect(toCheckoutSpy).toHaveBeenCalledWith({
                    offerId: 'PdNu1X2B5Z3ymfw2t1QDsg_OqJkg2fkYZF54AmVEenHKUqnZNm25RHXgd0vYkAuxOQgtCInkKCbZzgk101Xf7-2knb-3CndsMgsZZJXDmnFdFhk_xDPlUu8OfQhPTQZLAmQEmSlSSaMSSJnyZ4z7xiGDUuT8UegvTHPVsN8Ldg2Zr3cFRBEkKe266pimotlRZE,',
                    regionId: 213,
                    shouldSkipCart: true,
                    payment: PAYMENTS.YANDEX,
                });
            });

            // bluemarket-4163
            test('Клик неавторизованного пользователя тригерит "onAuth"', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

                const subscriptionButton = container.querySelector(SubscriptionButtonPO.root);

                subscriptionButton.click();

                // TODO MARKETFRONT-86630
                // expect(onAuthSpy).toHaveBeenCalledTimes(1);
                expect(toAuthSpy).toHaveBeenCalledWith({
                    offerId: 'PdNu1X2B5Z3ymfw2t1QDsg_OqJkg2fkYZF54AmVEenHKUqnZNm25RHXgd0vYkAuxOQgtCInkKCbZzgk101Xf7-2knb-3CndsMgsZZJXDmnFdFhk_xDPlUu8OfQhPTQZLAmQEmSlSSaMSSJnyZ4z7xiGDUuT8UegvTHPVsN8Ldg2Zr3cFRBEkKe266pimotlRZE,',
                    regionId: 213,
                    rawAuthUrl: 'external:passport-auth_{\"region\":\"ru\"}',
                    payment: PAYMENTS.YANDEX,
                });
            });
        });
    });
});
