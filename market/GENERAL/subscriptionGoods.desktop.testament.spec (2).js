// @flow

// flowlint-next-line untyped-import: off
import {screen} from '@testing-library/dom';
import {
    // flowlint-next-line untyped-import:off
    createProduct,
    // flowlint-next-line untyped-import:off
    mergeState,
    // flowlint-next-line untyped-import:off
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {mockLocation, mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {buildUrl} from '@self/project/src/utils/router';
import * as actions from '@self/root/src/actions/checkout/fromOffer';
import {EXP_FLAG_ID} from '@self/root/src/resolvers/plus/constants';
// flowlint-next-line untyped-import:off
import SubscriptionButtonPO from '@self/root/src/components/SubscriptionButton/__pageObject';
// flowlint-next-line untyped-import:off
import TextPO from '@self/root/src/uikit/components/Text/__pageObject';
// flowlint-next-line untyped-import:off
import DefaultOfferMiniPO from '@self/platform/components/DefaultOfferMini/__pageObject';
// flowlint-next-line untyped-import:off
import PricePO from '@self/platform/components/Price/__pageObject';
import {createOffer} from '@self/project/src/entities/offer/__mock__/offer.mock';
import {makeMirror} from '@self/platform/helpers/testament';

import {OFFER, PRODUCT, PRODUCT_ID, OFFER_ID, FEE_SHOW, SLUG} from './__mocks__/subscriptionGoodsMock';

const {PAYMENTS} = actions;

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

type TestContext = {
    exps?: { [string]: boolean },
    user?: { [string]: mixed },
};

const widgetPath = require.resolve('@self/platform/widgets/content/ProductCardTitle');

beforeAll(async () => {
    mockLocation();
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
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');

    await jestLayer.doMock(
        require.resolve('@self/root/src/widgets/content/PopupInformer'),
        () => ({create: () => Promise.resolve(null)})
    );
    await jestLayer.doMock(
        require.resolve('@self/platform/widgets/content/StickyProductCard'),
        () => ({create: () => Promise.resolve(null)})
    );
});

async function makeContext({user = {}, exps = {}}: TestContext = {}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            ...user,
        },
        request: {
            cookie,
            params: {
                productId: PRODUCT_ID,
                sponsored: '1',
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

afterAll(() => {
    mirror.destroy();
});

describe('ProductCardTitle', function () {
    describe('Станция по подписке', () => {
        // https://testpalm.yandex-team.ru/bluemarket/testcases/4148
        const widgetProps = {
            props: {
                productId: PRODUCT_ID,
            },
        };
        const exps = {
            [EXP_FLAG_ID]: true,
        };

        test('Указано для цены "/мес"', async () => {
            await makeContext({exps});
            await setReportState(OFFER);
            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);

            const priceSubscriptionAppendix = container.querySelector(PricePO.subscriptionAppendix);

            expect(priceSubscriptionAppendix).toBeTruthy();
        });

        test('Ссылка "Характеристики" cодержит параметр sponsored=1', async () => {
            await makeContext({exps});
            const expectedUrl = buildUrl('market:product-spec', {
                productId: String(PRODUCT_ID),
                slug: SLUG,
                sponsored: 1,
            });

            const button = screen.getByRole('link', {name: /характеристики/i});
            expect(button.getAttribute('href')).toEqual(expectedUrl);
        });

        test('Указано "Вместе с Плюс Мульти"', async () => {
            await makeContext({exps});
            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);

            const textElements = container.querySelectorAll(TextPO.root);
            const multiSubscriptionElements = Array.from(textElements).filter(
                element => element.textContent === 'Вместе с Плюс Мульти');

            expect(multiSubscriptionElements.length).toBe(1);
        });

        describe('Кнопка "В корзину"', () => {
            test('Не отображается', async () => {
                await makeContext({exps});
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);

                const cartButton = container.querySelector(DefaultOfferMiniPO.cartButton);

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

            test('Отображается', async () => {
                await makeContext({exps});
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);

                const subscriptionButton = container.querySelector(SubscriptionButtonPO.root);

                expect(subscriptionButton).toBeTruthy();
            });

            test('Клик авторизованного пользователя тригерит "onCheckout"', async () => {
                await makeContext({
                    user: {
                        isAuth: true,
                    },
                    exps,
                });
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);

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

            test('Клик неавторизованного пользователя тригерит "onAuth"', async () => {
                await makeContext({exps});
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetProps);

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
