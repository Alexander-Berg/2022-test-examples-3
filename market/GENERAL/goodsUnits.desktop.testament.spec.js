// @flow
// flowlint untyped-import:off

import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver, mockLocation} from '@self/root/src/helpers/testament/mock';

import OrderPrice from '@self/root/src/components/Orders/OrderItems/OrderPrice/__pageObject';

import type {Options} from '../index';
import {ORDER_ID, ORDERS} from './__mocks__/subscriptionGoodsMocks';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

type TestContext = {
    user?: { [string]: mixed },
};

const widgetPath = require.resolve('@self/root/src/widgets/content/orders/OrderCard');
const UNIT_POSTFIX: string = '/уп';

beforeAll(async () => {
    mockLocation();
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
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');
});

async function makeContext({user = {}}: TestContext = {}) {
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
        },
    });
}

afterAll(() => {
    mirror.destroy();
});

describe('OrderCard', () => {
    describe('Под названием товара указана цена', () => {
        // https://testpalm.yandex-team.ru/bluemarket/testcases/5573
        const user = {
            isAuth: true,
        };
        const widgetProps: Options = {
            props: {
                orderId: ORDER_ID,
            },
        };

        let container: HTMLElement;

        beforeAll(async () => {
            await makeContext({user});
            await kadavrLayer.setState(
                'Checkouter.collections.order',
                ORDERS.WITH_UNIT
            );

            const {container: widgetContainer} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
            container = widgetContainer;
        });

        it(' с суффиксом "/уп"', async () => {
            const priceElement = container.querySelector(OrderPrice.root);
            const price: number = parseFloat(priceElement && priceElement.textContent);

            expect(Number.isFinite(price)).toBe(true);
            expect(priceElement && priceElement.textContent.includes(UNIT_POSTFIX)).toBe(true);
        });
    });
});
