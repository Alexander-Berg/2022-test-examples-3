import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {checkoutCartId} from '@self/root/src/widgets/content/checkout/common/__spec__/mockData/common';
import {mockPerformance, mockYaRum} from '@self/root/src/helpers/testament/mock';

import {
    baseMockFunctionality,
    checkoutDeliveryEditorMockFunctionality,
} from './mockFunctionality';

import {
    containsUniqueOffers,
    containsEstimatedOffers,
} from './testCases';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

async function makeContext({cookies = {}, exps = {}, user = {}}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';

    const cookie = {
        ...cookies,
    };

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
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

beforeAll(async () => {
    mockPerformance();
    mockYaRum();

    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');

    await jestLayer.doMock(
        require.resolve('@self/root/src/widgets/core/Fink'),
        () => ({create: () => Promise.resolve(null)})
    );

    await jestLayer.doMock(
        require.resolve('@self/root/src/widgets/content/checkout/common/CheckoutHeader'),
        () => ({create: () => Promise.resolve(null)})
    );

    /**
     * @expFlag all_checkout_vector_map
     * @ticket MARKETFRONT-75723
     * @start
     */
    await jestLayer.doMock(
        require.resolve('@self/root/src/widgets/content/checkout/common/CheckoutVectorPinMap'),
        () => ({create: () => Promise.resolve(null)})
    );

    await jestLayer.doMock(
        require.resolve('@self/root/src/widgets/content/checkout/common/CheckoutVectorPlacemarkMap'),
        () => ({create: () => Promise.resolve(null)})
    );

    await jestLayer.doMock(
        require.resolve('@self/root/src/widgets/content/checkout/common/CheckoutPlacemarks'),
        () => ({create: () => Promise.resolve(null)})
    );
    /**
     * @expFlag all_checkout_vector_map
     * @ticket MARKETFRONT-75723
     * @end
     */
});

afterAll(() => {
    mirror.destroy();
});

const widgetPath = '@self/root/src/widgets/content/checkout/common/CheckoutDeliveryEditor';

describe('CheckoutDeliveryEditor', () => {
    describe('Карта ПВЗ', () => {
        describe('В посылке содержатся', () => {
            // MARKETFRONT-96354
            // eslint-disable-next-line jest/no-disabled-tests
            test.skip('товары на заказ',
                async () => {
                    await jestLayer.backend.runCode(baseMockFunctionality, [
                        '@self/root/src/widgets/content/checkout/common/__spec__/mockData/uniqueOrder', {
                            isUniqueOrder: true,
                            isEstimatedOrder: true,
                        },
                    ]);

                    await jestLayer.backend.runCode(checkoutDeliveryEditorMockFunctionality, []);
                    await makeContext({});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, {
                        visibleCheckoutCartId: checkoutCartId,
                    });

                    await containsUniqueOffers(container);
                }
            );

            test('товары с долгим сроком доставки ',
                async () => {
                    await jestLayer.backend.runCode(baseMockFunctionality, [
                        '@self/root/src/widgets/content/checkout/common/__spec__/mockData/uniqueOrder', {
                            isEstimatedOrder: true,
                        },
                    ]);
                    await jestLayer.backend.runCode(checkoutDeliveryEditorMockFunctionality, []);
                    await makeContext({});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, {
                        visibleCheckoutCartId: checkoutCartId,
                    });

                    await containsEstimatedOffers(container);
                }
            );
        });
    });
});
