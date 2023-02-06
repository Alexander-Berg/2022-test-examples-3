// @flow
import {makeMirror} from '@self/platform/helpers/testament';
// flowlint-next-line untyped-import:off
import PricePO from '@self/project/src/components/Price/__pageObject';
// flowlint-next-line untyped-import:off
import TextPO from '@self/root/src/uikit/components/Text/__pageObject';
// flowlint-next-line untyped-import:off
import CartButtonPO from '@self/project/src/components/CartButton/__pageObject';
// flowlint-next-line untyped-import:off
import SubscriptionButtonPO from '@self/root/src/components/SubscriptionButton/__pageObject';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import * as actions from '@self/platform/actions/checkout/fromOffer';
import {
    CART_ITEMS_MOCK,
    PAGE_STATE_MOCK,
    CPA_OFFER_MOCK,
} from './__mock__/subscriptionGoodsMock';

const {PAYMENTS} = actions;

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

type TestContext = {
    exps?: { [string]: boolean },
    user?: { [string]: mixed },
};

const widgetPath = require.resolve('@self/platform/widgets/parts/StickyOffer');

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

describe('Widget: StickyOffer', () => {
    beforeAll(async () => {
        mockIntersectionObserver();
        mirror = await makeMirror({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
        });
        mandrelLayer = mirror.getLayer('mandrel');
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    describe('Подписочный оффер', () => {
        beforeAll(async () => {
            await jestLayer.backend.runCode((cartItemsMock, pageStateMock) => {
                jest.spyOn(require('@self/project/src/resolvers/cartService/resolveCartItems'), 'resolveCartItems')
                    .mockResolvedValue(cartItemsMock);

                jest.spyOn(require('@self/platform/resolvers/pageState'), 'getPageState')
                    .mockResolvedValue(pageStateMock);

                jest.spyOn(require('@self/root/src/resolvers/user'), 'resolveCurrentUserRegionSync')
                    .mockReturnValue({result: 213});
            }, [CART_ITEMS_MOCK, PAGE_STATE_MOCK]);

            await jestLayer.runCode(cpaOfferMock => {
                jest.spyOn(require('@self/platform/widgets/parts/StickyOffer/selectors'), 'selectDenormalizedCpaDefaultOffers')
                    .mockReturnValue([cpaOfferMock]);
            }, [CPA_OFFER_MOCK]);
        });

        describe('Цена', () => {
            // bluemarket-4147
            test('Отображается с "/мес"', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

                const priceSubscriptionAppendix = container.querySelector(PricePO.subscriptionAppendix);

                expect(priceSubscriptionAppendix).toBeTruthy();
            });
        });

        describe('"Вместе с подпиской Плюс Мульти"', () => {
            // bluemarket-4147
            test('Отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

                const textElements = container.querySelectorAll(TextPO.root);
                const multiSubscriptionElements = Array.prototype.filter.call(
                    textElements,
                    element => element.innerHTML === 'Вместе с Плюс Мульти');

                expect(multiSubscriptionElements.length).toBe(1);
            });
        });

        describe('Кнопка "В корзину"', () => {
            // bluemarket-4147
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

            // bluemarket-4147
            test('Отображается', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

                const subscriptionButton = container.querySelector(SubscriptionButtonPO.root);

                expect(subscriptionButton).toBeTruthy();
            });

            // bluemarket-4147
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
                    offerId: 'H6Bn4eLTXsswSYSJg-BsLw_WQXO-z10zeNVsd8SwcshpymOEBqwEnDnDNkg9szvcjE_nN_HMDyKLj1hH95I2ydR64RFRiBcT_g3igm8z9Hm-E01Uswc2Y_yYrujGNcrYfGuK9AGtCx_UGC30fcLFWfQHYJNXELD8DLRc8G4t_MRMo1R_AD-G--InUljkVrueWw,',
                    regionId: 213,
                    shouldSkipCart: true,
                    payment: PAYMENTS.YANDEX,
                });
            });

            // bluemarket-4147
            test('Клик неавторизованного пользователя тригерит "onAuth"', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(widgetPath, {});

                const subscriptionButton = container.querySelector(SubscriptionButtonPO.root);

                subscriptionButton.click();

                // TODO MARKETFRONT-86630
                // expect(onAuthSpy).toHaveBeenCalledTimes(1);
                expect(toAuthSpy).toHaveBeenCalledWith({
                    offerId: 'H6Bn4eLTXsswSYSJg-BsLw_WQXO-z10zeNVsd8SwcshpymOEBqwEnDnDNkg9szvcjE_nN_HMDyKLj1hH95I2ydR64RFRiBcT_g3igm8z9Hm-E01Uswc2Y_yYrujGNcrYfGuK9AGtCx_UGC30fcLFWfQHYJNXELD8DLRc8G4t_MRMo1R_AD-G--InUljkVrueWw,',
                    regionId: 213,
                    rawAuthUrl: 'external:passport-auth_{\"region\":\"ru\"}',
                    payment: PAYMENTS.YANDEX,
                });
            });
        });
    });
});
