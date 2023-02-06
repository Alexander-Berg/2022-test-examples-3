import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {ORDER_TEXTS} from '@self/root/src/constants/order';

/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {Mirror} */
let mirror;

const WIDGET_PATH = require.resolve('@self/root/src/widgets/content/orders/OrderDownloadPaymentInvoices');

const orderId = 12345;

async function makeContext({cookies = {}, exps = {}, user = {}}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';
    const cookie = {
        kadavr_session_id: await kadavrLayer.getSessionId(),
        ...cookies,
    };

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            ...user,
        },
        request: {
            params: {
                orderId,
            },
            cookie,
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

describe('Widget: OrderDownloadPaymentInvoices.', () => {
    const FAKE_URL = 'https://yandex.ru/testme';

    beforeAll(async () => {
        mockIntersectionObserver();
        mirror = await makeMirrorDesktop({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
            kadavr: {
                asLibrary: true,
            },
        });
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');
        kadavrLayer = mirror.getLayer('kadavr');
        mandrelLayer = mirror.getLayer('mandrel');

        await jestLayer.doMock(
            require.resolve('@self/root/src/resolvers/orders/resolveMultiOrderPaymentInvoices'),
            () => ({
                resolveMultiOrderPaymentInvoices: () => Promise.resolve({
                    result: 'https://yandex.ru/testme',
                    collections: {},
                }),
            })
        );
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    const stateStub = {
        data: {
            url: FAKE_URL,
        },
        collections: {},
    };

    // testpalm: m2b-2 (MARKETFRONT-78771)
    test('Получает ссылку на архив со счетами и отображает кнопку', async () => {
        // Arrange
        const widgetOptions = {orderId};
        await kadavrLayer.setState('report', stateStub);
        await makeContext({cookies: {orderId}});

        // Act
        const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

        // Assert
        const selector = '[data-auto="order-download-payment-invoices"]';
        const button = container.querySelector(selector);
        expect(button.textContent).toEqual(ORDER_TEXTS.BUTTON.DOWNLOAD_PAYMENT_INVOICES);
    });
});
