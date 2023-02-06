// @flow
// flowlint-next-line untyped-import:off
import {getNodeText, screen} from '@testing-library/dom';
import {makeMirrorTouch as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
// flowlint-next-line untyped-import:off
import OrderPaymentPO from '@self/root/src/widgets/parts/OrderConfirmation/components/OrderPayment/__pageObject';
// flowlint-next-line untyped-import:off
import OrderConfirmation from '@self/root/src/spec/page-objects/OrderConfirmation';
import {
    preorderOrder,
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
                [preorderOrder.id]: preorderOrder,
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

    describe('Заказ по предзаказу оформлен', () => {
        const widgetOptions = {
            orderIds: [preorderOrder.id],
        };

        describe('Заголовок', () => {
            test('"Предзаказ оформлен"', async () => {
                await makeContext({
                    user: {
                        isAuth: true,
                    },
                });

                await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const title = getNodeText(screen.getByRole('heading', {
                    name: 'Предзаказ оформлен',
                }));

                expect(title).toBe('Предзаказ оформлен');
            });
        });

        describe('Текст под заголовком', () => {
            test('Пустой', async () => {
                await makeContext({
                    user: {
                        isAuth: true,
                    },
                });
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const orderInfo = getNodeText(container.querySelector(OrderConfirmation.ordersInfoText));

                expect(orderInfo.includes('')).toBeTruthy();
            });
        });

        describe('Статус оплаты', () => {
            test('"Оплачено"', async () => {
                await makeContext({
                    user: {
                        isAuth: true,
                    },
                });
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const orderPayment = container.querySelector(OrderPaymentPO.root);
                const orderPaymentText = orderPayment?.textContent || '';

                expect(orderPaymentText).toBe('Оплачено');
            });
        });
    });
});
