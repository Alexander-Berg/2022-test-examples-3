import {mockLocation, mockPerformance} from '@self/root/src/helpers/testament/mock';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';

import {TEST_CASES} from './yandexPlusInfo/testCases/';
import {
    containsUniqueOffers,
    containsEstimatedOffers,
    containsCancelationOffers,
} from './uniqueOrder/testCases';

import {baseMockFunctionality, orderConfirmationResolversMockConstructor} from './common/mockFunctionality';

const WIDGET_PATH = '@self/root/src/widgets/parts/OrderConfirmation';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

beforeAll(async () => {
    mockPerformance();
    mockLocation();
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

    await baseMockFunctionality(jestLayer);

    await jestLayer.backend.doMock(
        require.resolve('@self/root/src/widgets/content/NextOrder'),
        () => ({create: () => Promise.resolve(null)})
    );

    await jestLayer.backend.doMock(
        require.resolve('@self/root/src/widgets/parts/Coin/GiftedBonusList'),
        () => ({create: () => Promise.resolve(null)})
    );

    await jestLayer.backend.doMock(
        require.resolve('@self/root/src/widgets/content/OrderPollPopup'),
        () => ({create: () => Promise.resolve(null)})
    );

    await jestLayer.backend.doMock(
        require.resolve('@self/root/src/widgets/content/OrderConfirmationAuth'),
        () => ({create: () => Promise.resolve(null)})
    );
});

afterAll(() => {
    mirror.destroy();
});

const createTestSuites = (
    testCase,
    widgetPath,
    mockFunction
) => {
    if (testCase.describe) {
        // eslint-disable-next-line jest/valid-describe
        describe(testCase.describe.name, () => {
            testCase.describe.suites.map(suite => createTestSuites(
                suite,
                widgetPath,
                mockFunction
            ));
        });
    } else {
        it(testCase.caseName, async () => {
            await testCase.method(
                widgetPath,
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockFunction,
                testCase.mockData,
                testCase.ctxMock,
                testCase.expectedTitle,
                testCase.expectedDescription,
                testCase.expectedLink,
                testCase.expectedPrice
            );
        });
    }
};

describe('OrderConfirmation', () => {
    describe('YandexPlusInfo. Блок с информацией о Яндекс.Плюсе', () => {
        TEST_CASES.map(testCase => createTestSuites(
            testCase,
            WIDGET_PATH,
            orderConfirmationResolversMockConstructor
        ));
    });
    describe('В заказе содержатся', () => {
        test('товары на заказ',
            async () => {
                await containsUniqueOffers(jestLayer, apiaryLayer, mandrelLayer);
            }
        );

        test('товары с долгим сроком доставки ',
            async () => {
                await containsEstimatedOffers(jestLayer, apiaryLayer, mandrelLayer);
            }
        );

        test('товары с возможностью отмены',
            async () => {
                await containsCancelationOffers(jestLayer, apiaryLayer, mandrelLayer);
            }
        );
    });
});
