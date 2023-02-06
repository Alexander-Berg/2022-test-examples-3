// @flow
import {makeMirrorTouch as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
// flowlint-next-line untyped-import:off
import PricePO from '@self/root/src/uikit/components/PriceBase/__pageObject';
// flowlint-next-line untyped-import:off
import OrderPaymentPO from '@self/root/src/widgets/parts/OrderConfirmation/components/OrderPayment/__pageObject';
// flowlint-next-line untyped-import:off
import OrderLinkReceiptPO from '@self/root/src/widgets/parts/OrderConfirmation/components/OrderLinkReceipt/__pageObject';
import {
    paidOrder,
    unpaidOrder,
} from './__mock__/subscriptionGoodsMock.js';

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

const widgetPath = require.resolve('@self/root/src/widgets/parts/OrderConfirmation');

async function makeContext({exps = {}, user = {}}: TestContext = {}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user: {
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

const setCheckouterState = async () => {
    const checkouterState = {
        collections: {
            order: {
                [paidOrder.id]: paidOrder,
                [unpaidOrder.id]: unpaidOrder,
            },
        },
    };

    await kadavrLayer.setState('Checkouter', checkouterState);
};

describe('Widget: OrderConfirmation', () => {
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

        window.performance.timing = {
            navigationStart: () => 0,
        };

        await setCheckouterState();

        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/OrderPollPopup'),
            () => ({create: () => Promise.resolve(null)})
        );

        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/parts/Coin/GiftedBonusList'),
            () => ({create: () => Promise.resolve(null)})
        );

        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/NextOrder'),
            () => ({create: () => Promise.resolve(null)})
        );

        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/parts/ReferralLandingLink'),
            () => ({create: () => Promise.resolve(null)})
        );

        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/cashbackDescription/CashbackDescriptionPopupContentLoader'),
            () => ({create: () => Promise.resolve(null)})
        );

        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/MediaGallery'),
            () => ({create: () => Promise.resolve(null)})
        );

        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/content/AppDistributionBanner/AppDistributionBannerLoader'),
            () => ({create: () => Promise.resolve(null)})
        );
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    describe('Заказ по подписке оформлен', () => {
        const widgetOptions = {
            orderIds: [paidOrder.id],
        };

        describe('Статус оплаты', () => {
            // bluemarket-4168
            test('"Оплачено по подписке"', async () => {
                await makeContext({
                    user: {
                        isAuth: true,
                    },
                });
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const orderPayment = container.querySelector(OrderPaymentPO.root);
                const orderPaymentText = orderPayment?.textContent || '';

                expect(orderPaymentText.includes('Оплачено по подписке')).toBeTruthy();
            });
        });

        describe('Цена', () => {
            // bluemarket-4168
            test('Отображается с "/мес"', async () => {
                await makeContext({
                    user: {
                        isAuth: true,
                    },
                });
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const price = container.querySelector(PricePO.root);
                const priceText = price?.textContent || '';

                expect(priceText.includes('/мес')).toBeTruthy();
            });
        });

        describe('Кнопка "Посмотреть чек"', () => {
            // bluemarket-4168
            test('Не отображается', async () => {
                await makeContext({
                    user: {
                        isAuth: true,
                    },
                });
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const orderLinkReceipt = container.querySelector(OrderLinkReceiptPO.root);

                expect(orderLinkReceipt).toBeFalsy();
            });
        });
    });

    describe('Заказ по подписке не оформлен', () => {
        const widgetOptions = {
            orderIds: [unpaidOrder.id],
        };

        describe('Статус оплаты', () => {
            // bluemarket-4170
            test('"Не оплачено"', async () => {
                await makeContext({
                    user: {
                        isAuth: true,
                    },
                });
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const orderPayment = container.querySelector(OrderPaymentPO.root);
                const orderPaymentText = orderPayment?.textContent || '';

                expect(orderPaymentText.includes('Не оплачено')).toBeTruthy();
            });
        });

        describe('Цена', () => {
            // bluemarket-4170
            test('Отображается с "/мес"', async () => {
                await makeContext({
                    user: {
                        isAuth: true,
                    },
                });
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const price = container.querySelector(PricePO.root);
                const priceText = price?.textContent || '';

                expect(priceText.includes('/мес')).toBeTruthy();
            });
        });
    });
});
