// @flow
import {makeMirrorTouch as makeMirror} from '@self/root/src/helpers/testament/mirror';
// flowlint-next-line untyped-import:off
import {waitFor, getByTestId} from '@testing-library/dom';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import * as paymentManagerUtils from '@self/root/src/utils/paymentManager';
import {
    paidOrder,
    paymentWidgetOrigin,
    openWidgetMock,
    orderPaymentByOrderIdsMock,
    MOCK_NONCE,
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

const widgetPath = require.resolve('@self/root/src/widgets/parts/Payment');
const widgetOptions = {
    orderIds: [paidOrder.id],
    bindKeys: [],
    allowSpasiboPayment: false,
    allowYandexPay: false,
    relatedOrderIds: [],
    fromPaymentChangePage: true,
};

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
            },
        },
    };

    await kadavrLayer.setState('Checkouter', checkouterState);
};

describe('Дооплата подписного товара', () => {
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

        await jestLayer.backend.runCode((_orderPaymentByOrderIdsMock, mockNonce) => {
            // eslint-disable-next-line global-require
            jest.spyOn(require('@self/root/src/resolvers/orders/resolveOrderPaymentByOrderIds'), 'resolveOrderPaymentByOrderIds')
                .mockResolvedValue(_orderPaymentByOrderIdsMock);

            // eslint-disable-next-line global-require
            jest.spyOn(require('@yandex-market/mandrel/resolvers/csp'), 'resolveCspNonceSync')
                .mockReturnValue(mockNonce);
        }, [orderPaymentByOrderIdsMock, MOCK_NONCE]);


        await setCheckouterState();
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    // bluemarket-4178
    test('открывается фрейм медиабиллинга с правильными параметрами', async () => {
        const initializingPaymentManagerSpy = jest.spyOn(paymentManagerUtils, 'initializingPaymentManager').mockResolvedValue();
        const openWidgetSpy = jest.spyOn(paymentManagerUtils, 'openWidget').mockImplementation(() => {});

        await makeContext({
            user: {
                isAuth: true,
            },
        });
        const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

        await waitFor(() => {
            const paymentWidgetFrame = getByTestId(container, 'mediabilling-payment-widget');

            expect(paymentWidgetFrame).toBeTruthy();
            expect(initializingPaymentManagerSpy).toHaveBeenCalledWith(paymentWidgetOrigin, MOCK_NONCE);
            expect(openWidgetSpy).toHaveBeenCalledWith(expect.objectContaining(openWidgetMock));
        });
    });
});
