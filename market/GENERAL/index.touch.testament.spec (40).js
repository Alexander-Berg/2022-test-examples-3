import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

// COMMON TOOLTIP
import {
    baseMockFunctionality as commonBaseMockFunctionality,
    mockWidgetData as commonMockWidgetFunction,
} from './mockFunctionality';
import {TEST_CASES as COMMON_PROMO_TOOLTIP_CASES} from './testCases/';

const WIDGET_PATH = '@self/root/src/widgets/content/PromoTooltip/tooltips/YandexPlusPromoCommonTooltip';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
beforeAll(async () => {
    mockLocation();
    mirror = await makeMirrorTouch({
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
                testCase.expectedDescription
            );
        });
    }
};

describe('PromoTooltip', () => {
    describe('Общий тултип с выгодой Плюса', () => {
        beforeAll(async () => {
            await commonBaseMockFunctionality(jestLayer);
        });

        COMMON_PROMO_TOOLTIP_CASES.map(testCase => createTestSuites(
            testCase,
            WIDGET_PATH,
            commonMockWidgetFunction
        ));
    });
});
